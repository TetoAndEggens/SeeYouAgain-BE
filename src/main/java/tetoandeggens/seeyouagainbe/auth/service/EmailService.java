package tetoandeggens.seeyouagainbe.auth.service;

import jakarta.mail.*;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tetoandeggens.seeyouagainbe.auth.dto.ImapCredentials;
import tetoandeggens.seeyouagainbe.global.constants.AuthVerificationConstants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static tetoandeggens.seeyouagainbe.global.constants.AuthVerificationConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final Properties imapConnectionProperties;
    private final ImapCredentials imapCredentials;

    public String getServerEmail() {
        return imapCredentials.userName();
    }

    public boolean extractCodeByPhoneNumber(String code, String phone, LocalDateTime since) {
        Store store = null;
        Folder inbox = null;
        log.info("[IMAP] 인증 시작 - phone: {}, code: {}, since: {}", phone, code, since);

        try {
            log.info("[IMAP] IMAP 연결 시도 - host: {}, port: {}",
                    imapConnectionProperties.getProperty("mail.imaps.host"),
                    imapConnectionProperties.getProperty("mail.imaps.port"));

            store = connectToEmailStore();
            log.info("[IMAP] IMAP 연결 성공");

            inbox = openInboxFolder(store);
            log.info("[IMAP] INBOX 열기 성공 - 메시지 수: {}", inbox.getMessageCount());

            boolean result = searchTokenInEmails(inbox, code, phone, since);
            log.info("[IMAP] 검색 완료 - 결과: {}", result);

            return result;
        } catch (MessagingException e) {
            log.error("[IMAP] MessagingException 발생 - phone: {}, error: {}",
                    phone, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("[IMAP] Exception 발생 - phone: {}, error: {}",
                    phone, e.getMessage(), e);
            return false;
        } finally {
            closeConnections(inbox, store);
            log.info("[IMAP] 연결 종료");
        }
    }

    private Store connectToEmailStore() throws MessagingException {
        Session session = Session.getDefaultInstance(imapConnectionProperties);
        Store store = session.getStore(IMAP_PROTOCOL);
        store.connect(imapCredentials.userName(), imapCredentials.password());
        return store;
    }

    private Folder openInboxFolder(Store store) throws MessagingException {
        Folder inbox = store.getFolder(INBOX);
        inbox.open(Folder.READ_ONLY);
        return inbox;
    }

    private boolean searchTokenInEmails(Folder inbox, String code, String phone, LocalDateTime since) throws MessagingException {
        Date sinceDate = Date.from(since.atZone(ZoneId.systemDefault()).toInstant());
        SearchTerm timeTerm = new ReceivedDateTerm(ComparisonTerm.GE, sinceDate);
        Message[] messages = inbox.search(timeTerm);

        log.info("[IMAP] since 이후 메시지 검색 - 총 {}개 발견", messages.length);

        Arrays.sort(messages, (a, b) -> {
            try {
                Date dateA = a.getReceivedDate();
                Date dateB = b.getReceivedDate();
                if (dateA == null && dateB == null) return 0;
                if (dateA == null) return 1;
                if (dateB == null) return -1;
                return dateB.compareTo(dateA);
            } catch (MessagingException e) {
                return 0;
            }
        });

        for (Message message : messages) {
            try {
                Address[] from = message.getFrom();
                String fromAddr = (from != null && from.length > 0) ? from[0].toString() : "null";
                log.info("[IMAP] 메시지 확인 - From: {}, Subject: {}",
                        fromAddr, message.getSubject());

                if (isFromPhoneNumber(message, phone)) {
                    log.info("[IMAP] 전화번호 매칭 성공 - From: {}", fromAddr);

                    String mailCode = extractCodeFromMessageContent(message);
                    log.info("[IMAP] 추출된 코드: {}, 기대 코드: {}", mailCode, code);

                    if (mailCode != null && code.equals(mailCode)) {
                        log.info("[IMAP] 코드 검증 성공!");
                        return true;
                    }
                } else {
                    log.debug("[IMAP] 전화번호 불일치 - From: {}, 찾는 번호: {}",
                            fromAddr, phone);
                }
            } catch (Exception e) {
                log.error("[IMAP] 메시지 처리 중 오류", e);
            }
        }
        log.warn("[IMAP] 매칭되는 인증 코드를 찾지 못함");
        return false;
    }

    private boolean isFromPhoneNumber(Message message, String phone) {
        try {
            Address[] from = message.getFrom();
            if (from == null || from.length == 0) {
                return false;
            }
            String fromAddress = from[0].toString();
            boolean matches = fromAddress.contains(phone + AT);

            log.debug("[IMAP] isFromPhoneNumber - fromAddress: {}, phone: {}, matches: {}",
                    fromAddress, phone, matches);

            return matches;
        } catch (MessagingException e) {
            log.error("[IMAP] isFromPhoneNumber 오류", e);
            return false;
        }
    }

    private String extractCodeFromMessageContent(Message message) {
        try {
            // LG U+: 단순 텍스트 본문
            if (message.isMimeType(TEXT)) {
                String content = (String) message.getContent();
                return extractCode(content);
            }

            // KT: multipart (본문 + 첨부파일)
            if (message.isMimeType(MULTIPART)) {
                return extractFromMultipart(message);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractFromMultipart(Message message) throws MessagingException, IOException {
        Multipart multipart = (Multipart) message.getContent();

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);

            if (bodyPart.isMimeType(TEXT)) {
                String content = (String) bodyPart.getContent();
                String code = extractCode(content);
                if (code != null) {
                    return code;
                }
            }

            // 텍스트 파일 첨부에서 토큰 찾기 (아이폰 KT)
            if (isTextFile(bodyPart)) {
                InputStream inputStream = bodyPart.getInputStream();
                String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                String code = extractCode(content);
                if (code != null) {
                    return code;
                }
            }
        }
        return null;
    }

    private boolean isTextFile(BodyPart bodyPart) throws MessagingException {
        String fileName = bodyPart.getFileName();
        return fileName != null && fileName.toLowerCase().endsWith(TXT);
    }

    private String extractCode(String content) {
        if (content == null) {
            return null;
        }
        Pattern pattern = Pattern.compile(AuthVerificationConstants.VERIFICATION_CODE_PATTERN);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private void closeConnections(Folder inbox, Store store) {
        try {
            if (inbox != null && inbox.isOpen()) {
                inbox.close(false);
            }
        } catch (MessagingException e) {}

        try {
            if (store != null && store.isConnected()) {
                store.close();
            }
        } catch (MessagingException e) {}
    }
}
