package tetoandeggens.seeyouagainbe.animal.repository.custom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.animal.entity.BookMark;
import tetoandeggens.seeyouagainbe.animal.entity.QAnimal;

import java.util.List;
import java.util.Optional;

import static tetoandeggens.seeyouagainbe.animal.entity.QBookMark.bookMark;

@RequiredArgsConstructor
public class BookMarkRepositoryCustomImpl implements BookMarkRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<BookMark> findAllByMemberIdAndNotDeleted(Long memberId) {
        QAnimal animal = QAnimal.animal;

        return queryFactory
                .selectFrom(bookMark)
                .join(bookMark.animal, animal).fetchJoin()
                .join(animal.breedType).fetchJoin()
                .where(
                        bookMark.member.id.eq(memberId),
                        bookMark.isDeleted.eq(false)
                )
                .fetch();
    }

    @Override
    public Optional<BookMark> findByMemberIdAndAnimalId(Long memberId, Long animalId) {
        BookMark result = queryFactory
                .selectFrom(bookMark)
                .where(
                        bookMark.member.id.eq(memberId),
                        bookMark.animal.id.eq(animalId)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }
}
