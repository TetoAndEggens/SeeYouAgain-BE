package tetoandeggens.seeyouagainbe.fcm.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum DeviceType {
    WEB("WEB", "웹"),
    ANDROID("ANDROID", "안드로이드"),
    IOS("IOS", "iOS"),
    ETC("ETC", "그외");

    private final String code;
    private final String description;

    public static DeviceType fromCode(String code) {
        return Arrays.stream(DeviceType.values())
                .filter(deviceType -> deviceType.code.equals(code))
                .findFirst()
                .orElse(WEB);
    }
}