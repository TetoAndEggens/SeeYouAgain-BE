package tetoandeggens.seeyouagainbe.fcm.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tetoandeggens.seeyouagainbe.fcm.entity.DeviceType;

@Slf4j
@Component
public class DeviceTypeValidator {

    public DeviceType validateAndExtractDeviceType(String userAgentString) {

        if (userAgentString == null || userAgentString.trim().isEmpty()) {
            return DeviceType.ETC;
        }

        try {
            String lowerCaseUA = userAgentString.toLowerCase().trim();

            if (lowerCaseUA.contains("android")) {
                return DeviceType.ANDROID;
            }

            if (lowerCaseUA.contains("iphone") ||lowerCaseUA.contains("ipad") || lowerCaseUA.contains("ipod")) {
                return DeviceType.IOS;
            }

            return DeviceType.WEB;
        } catch (Exception e) {
            log.warn("User-Agent 파싱 실패 - ETC으로 간주: {}", userAgentString, e);
            return DeviceType.ETC;
        }
    }
}