package tetoandeggens.seeyouagainbe.fcm.util;

import eu.bitwalker.useragentutils.UserAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tetoandeggens.seeyouagainbe.fcm.entity.DeviceType;

@Slf4j
@Component
public class DeviceTypeValidator {

    public DeviceType extractDeviceType(String userAgentString) {
        if (userAgentString == null || userAgentString.isEmpty()) {
            log.warn("User-Agent 헤더가 없습니다. WEB으로 간주합니다.");
            return DeviceType.WEB;
        }

        log.debug("User-Agent 분석 시작: {}", userAgentString);

        try {
            UserAgent userAgent = UserAgent.parseUserAgentString(userAgentString);
            String lowerCaseUA = userAgentString.toLowerCase();

            // 1. Android 확인
            if (lowerCaseUA.contains("android")) {
                log.info("기기 타입 감지: ANDROID - User-Agent: {}", userAgentString);
                return DeviceType.ANDROID;
            }

            // 2. iOS 확인 (iPhone, iPad, iPod)
            if (lowerCaseUA.contains("iphone") ||
                    lowerCaseUA.contains("ipad") ||
                    lowerCaseUA.contains("ipod")) {
                log.info("기기 타입 감지: IOS - User-Agent: {}", userAgentString);
                return DeviceType.IOS;
            }

            // 3. 모바일/태블릿이지만 Android/iOS가 아닌 경우
            switch (userAgent.getOperatingSystem().getDeviceType()) {
                case MOBILE:
                case TABLET:
                    log.warn("알 수 없는 모바일 기기 - WEB으로 처리: {}", userAgentString);
                    return DeviceType.WEB;

                case COMPUTER:
                case UNKNOWN:
                default:
                    log.info("기기 타입 감지: WEB - User-Agent: {}", userAgentString);
                    return DeviceType.WEB;
            }
        } catch (Exception e) {
            log.error("User-Agent 파싱 실패 - WEB으로 간주: {}", userAgentString, e);
            return DeviceType.WEB;
        }
    }

    public DeviceType validateAndExtractDeviceType(String userAgentString) {
        DeviceType deviceType = extractDeviceType(userAgentString);

        log.info("기기 타입 자동 결정 완료: {} (User-Agent: {})",
                deviceType,
                userAgentString != null && userAgentString.length() > 50
                        ? userAgentString.substring(0, 50) + "..."
                        : userAgentString);

        return deviceType;
    }
}