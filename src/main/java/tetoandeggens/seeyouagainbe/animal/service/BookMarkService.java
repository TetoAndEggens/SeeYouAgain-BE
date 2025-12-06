package tetoandeggens.seeyouagainbe.animal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tetoandeggens.seeyouagainbe.animal.dto.response.BookMarkAnimalResponse;
import tetoandeggens.seeyouagainbe.animal.dto.response.BookMarkResponse;
import tetoandeggens.seeyouagainbe.animal.entity.Animal;
import tetoandeggens.seeyouagainbe.animal.entity.AnimalType;
import tetoandeggens.seeyouagainbe.animal.entity.BookMark;
import tetoandeggens.seeyouagainbe.animal.repository.AnimalRepository;
import tetoandeggens.seeyouagainbe.animal.repository.BookMarkRepository;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AnimalErrorCode;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.BookMarkErrorCode;
import tetoandeggens.seeyouagainbe.member.entity.Member;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookMarkService {

    private final BookMarkRepository bookMarkRepository;
    private final AnimalRepository animalRepository;

    @Transactional(readOnly = true)
    public List<BookMarkAnimalResponse> getMyBookMarks(Long memberId) {

        List<BookMark> bookMarks = bookMarkRepository.findAllByMemberIdAndNotDeleted(memberId);
        List<BookMarkAnimalResponse> responses = new ArrayList<>();

        for (BookMark bookMark : bookMarks) {
            responses.add(BookMarkAnimalResponse.from(bookMark));
        }

        return responses;
    }

    @Transactional
    public BookMarkResponse toggleBookMark(Long memberId, Long animalId) {

        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new CustomException(AnimalErrorCode.ANIMAL_NOT_FOUND));

        // 현재 피그마 상태는 유기동물만 북마크 가능이기에 유기동물만 북마크 가능하도록 제한
        if (animal.getAnimalType() != AnimalType.ABANDONED) {
            throw new CustomException(BookMarkErrorCode.ONLY_ABANDONED_ANIMAL_CAN_BE_BOOKMARKED);
        }

        Optional<BookMark> existingBookMark = bookMarkRepository.findByMemberIdAndAnimalId(memberId, animalId);

        if (existingBookMark.isPresent()) {
            BookMark bookMark = existingBookMark.get();
            bookMark.toggleDelete();
            return BookMarkResponse.from(bookMark);
        } else {
            BookMark newBookMark = BookMark.builder()
                    .member(new Member(memberId))
                    .animal(animal)
                    .build();
            bookMarkRepository.save(newBookMark);
            return BookMarkResponse.from(newBookMark);
        }
    }
}
