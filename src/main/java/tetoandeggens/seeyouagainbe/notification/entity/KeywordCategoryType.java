package tetoandeggens.seeyouagainbe.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum KeywordCategoryType {
    BREED("BREED", "품종"),
    LOCATION("LOCATION", "지역");

    private final String code;
    private final String description;

    public static KeywordCategoryType fromCode(String code) {
        return Arrays.stream(KeywordCategoryType.values())
                .filter(categoryType -> categoryType.code.equals(code))
                .findFirst()
                .orElse(BREED);
    }
}