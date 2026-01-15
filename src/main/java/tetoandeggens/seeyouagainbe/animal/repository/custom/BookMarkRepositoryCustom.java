package tetoandeggens.seeyouagainbe.animal.repository.custom;

import tetoandeggens.seeyouagainbe.animal.dto.response.BookMarkAnimalResponse;
import tetoandeggens.seeyouagainbe.animal.entity.BookMark;

import java.util.List;
import java.util.Optional;

public interface BookMarkRepositoryCustom {

    List<BookMarkAnimalResponse> findAllByMemberIdAndNotDeleted(Long memberId);

    Optional<BookMark> findByMemberIdAndAnimalId(Long memberId, Long animalId);
}
