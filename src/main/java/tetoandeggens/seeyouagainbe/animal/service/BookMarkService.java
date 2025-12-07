package tetoandeggens.seeyouagainbe.animal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tetoandeggens.seeyouagainbe.animal.dto.response.BookMarkAnimalResponse;
import tetoandeggens.seeyouagainbe.animal.entity.Animal;
import tetoandeggens.seeyouagainbe.animal.entity.AnimalType;
import tetoandeggens.seeyouagainbe.animal.entity.BookMark;
import tetoandeggens.seeyouagainbe.animal.repository.AnimalRepository;
import tetoandeggens.seeyouagainbe.animal.repository.BookMarkRepository;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AnimalErrorCode;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.BookMarkErrorCode;
import tetoandeggens.seeyouagainbe.member.entity.Member;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookMarkService {

    private final BookMarkRepository bookMarkRepository;
    private final AnimalRepository animalRepository;

    @Transactional(readOnly = true)
    public List<BookMarkAnimalResponse> getMyBookMarks(Long memberId) {
        return bookMarkRepository.findAllByMemberIdAndNotDeleted(memberId);
    }

    @Transactional
    public void toggleBookMark(Long memberId, Long animalId) {
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new CustomException(AnimalErrorCode.ANIMAL_NOT_FOUND));

        if (animal.getAnimalType() != AnimalType.ABANDONED) {
            throw new CustomException(BookMarkErrorCode.ONLY_ABANDONED_ANIMAL_CAN_BE_BOOKMARKED);
        }

        Optional<BookMark> existingBookMark = bookMarkRepository.findByMemberIdAndAnimalId(memberId, animalId);

        if (existingBookMark.isPresent()) {
            BookMark bookMark = existingBookMark.get();
            bookMark.toggleDelete();
        } else {
            BookMark newBookMark = BookMark.builder()
                    .member(new Member(memberId))
                    .animal(animal)
                    .build();
            bookMarkRepository.save(newBookMark);
        }
    }
}
