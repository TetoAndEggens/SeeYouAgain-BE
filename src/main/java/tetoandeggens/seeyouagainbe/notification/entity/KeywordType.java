package tetoandeggens.seeyouagainbe.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum KeywordType {
    ABANDONED("ABANDONED", "유기 동물"),
    WITNESS("WITNESS", "목격 정보");

    private final String code;
    private final String description;

    public static KeywordType fromCode(String code) {
        return Arrays.stream(KeywordType.values())
                .filter(keywordType -> keywordType.code.equals(code))
                .findFirst()
                .orElse(ABANDONED);
    }
}