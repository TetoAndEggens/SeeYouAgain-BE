package tetoandeggens.seeyouagainbe.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tetoandeggens.seeyouagainbe.notification.entity.KeywordCategoryType;
import tetoandeggens.seeyouagainbe.notification.entity.KeywordType;
import tetoandeggens.seeyouagainbe.notification.entity.NotificationKeyword;
import tetoandeggens.seeyouagainbe.notification.repository.custom.NotificationKeywordRepositoryCustom;

import java.util.List;
import java.util.Optional;

public interface NotificationKeywordRepository extends JpaRepository<NotificationKeyword, Long>, NotificationKeywordRepositoryCustom {

    List<NotificationKeyword> findAllByMemberId(Long memberId);

    boolean existsByMemberIdAndKeywordAndKeywordTypeAndKeywordCategoryType(
            Long memberId, String keyword, KeywordType keywordType, KeywordCategoryType keywordCategoryType);

    Optional<NotificationKeyword> findByIdAndMemberId(Long id, Long memberId);
}