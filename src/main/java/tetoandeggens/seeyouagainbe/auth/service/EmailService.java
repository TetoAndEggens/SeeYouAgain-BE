package tetoandeggens.seeyouagainbe.auth.service;

import jakarta.mail.*;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tetoandeggens.seeyouagainbe.auth.dto.ImapCredentials;
import tetoandeggens.seeyouagainbe.global.constants.EmailVerificationConstant;

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

import static tetoandeggens.seeyouagainbe.global.constants.EmailVerificationConstant.*;

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

        try {
            store = connectToEmailStore();
            inbox = openInboxFolder(store);
            boolean result = searchTokenInEmails(inbox, code, phone, since);
            return result;
        } catch (Exception e) {
            return false;
        } finally {
            closeConnections(inbox, store);
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
            if (isFromPhoneNumber(message, phone)) {
                String mailCode = extractCodeFromMessageContent(message);
                if (mailCode != null && code.equals(mailCode)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isFromPhoneNumber(Message message, String phone) {
        try {
            Address[] from = message.getFrom();
            if (from == null || from.length == 0) {
                return false;
            }
            String fromAddress = from[0].toString();
            return fromAddress.contains(phone + AT);
        } catch (MessagingException e) {
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
        Pattern pattern = Pattern.compile(EmailVerificationConstant.VERIFICATION_CODE_PATTERN);
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
