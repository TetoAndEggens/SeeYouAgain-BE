package tetoandeggens.seeyouagainbe.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tetoandeggens.seeyouagainbe.global.entity.BaseEntity;
import tetoandeggens.seeyouagainbe.member.entity.Member;

@Entity
@Table(name = "NOTIFICATION_KEYWORD")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationKeyword extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "notification_keyword_id")
	private Long id;

	@Column(name = "keyword")
	private String keyword;

    @Enumerated(EnumType.STRING)
    @Column(name = "keyword_type", nullable = false)
    private KeywordType keywordType;

    @Enumerated(EnumType.STRING)
    @Column(name = "keyword_category_type", nullable = false)
    private KeywordCategoryType keywordCategoryType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private Member member;

    @Builder
    public NotificationKeyword(String keyword, KeywordType keywordType,
                               KeywordCategoryType keywordCategoryType, Member member) {
        this.keyword = keyword;
        this.keywordType = keywordType;
        this.keywordCategoryType = keywordCategoryType;
        this.member = member;
    }

    // 동일한 키워드인지 확인
    public boolean isSameKeyword(String keyword, KeywordType keywordType,
                                 KeywordCategoryType keywordCategoryType) {
        return this.keyword.equals(keyword)
                && this.keywordType == keywordType
                && this.keywordCategoryType == keywordCategoryType;
    }
}