package tetoandeggens.seeyouagainbe.board.repository.custom;

import static tetoandeggens.seeyouagainbe.board.entity.QBoard.*;

import java.util.List;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.animal.entity.QAnimal;
import tetoandeggens.seeyouagainbe.animal.entity.QAnimalLocation;
import tetoandeggens.seeyouagainbe.animal.entity.QAnimalS3Profile;
import tetoandeggens.seeyouagainbe.animal.entity.QBreedType;
import tetoandeggens.seeyouagainbe.board.dto.response.BoardDetailResponse;
import tetoandeggens.seeyouagainbe.board.dto.response.BoardResponse;
import tetoandeggens.seeyouagainbe.board.dto.response.ProfileInfo;
import tetoandeggens.seeyouagainbe.board.dto.response.TagInfo;
import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.board.entity.QBoardTag;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.common.enums.ContentType;
import tetoandeggens.seeyouagainbe.member.entity.QMember;

@RequiredArgsConstructor
public class BoardRepositoryCustomImpl implements BoardRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<BoardResponse> getAnimalBoards(
		CursorPageRequest request, SortDirection sortDirection, ContentType contentType
	) {
		BooleanExpression cursorCondition = createCursorCondition(request.cursorId(), sortDirection);
		BooleanExpression typeCondition = createContentTypeCondition(contentType);
		OrderSpecifier<Long> orderSpecifier = createOrderSpecifier(sortDirection);

		QAnimal animal = QAnimal.animal;
		QBreedType bt = QBreedType.breedType;
		QMember member = QMember.member;
		QAnimalS3Profile profileEntity = QAnimalS3Profile.animalS3Profile;
		QAnimalS3Profile subProfile = new QAnimalS3Profile("subProfile");
		QAnimalLocation animalLocation = QAnimalLocation.animalLocation;

		return queryFactory
			.select(Projections.constructor(
				BoardResponse.class,
				board.id,
				board.title,
				animal.species,
				bt.name,
				animal.sex,
				animalLocation.address,
				Expressions.numberTemplate(Double.class, "ST_Y({0})", animalLocation.coordinates),
				Expressions.numberTemplate(Double.class, "ST_X({0})", animalLocation.coordinates),
				animal.animalType,
				member.nickName,
				profileEntity.profile,
				board.createdAt,
				board.updatedAt,
				Expressions.constant(java.util.Collections.emptyList())
			))
			.from(board)
			.join(board.animal, animal)
			.join(board.member, member)
			.leftJoin(animal.animalLocation, animalLocation)
			.leftJoin(animal.breedType, bt)
			.leftJoin(profileEntity).on(
				profileEntity.animal.eq(animal),
				profileEntity.id.eq(
					JPAExpressions.select(subProfile.id.min())
						.from(subProfile)
						.where(subProfile.animal.eq(animal))
				)
			)
			.where(
				typeCondition,
				cursorCondition,
				board.isDeleted.eq(false)
			)
			.orderBy(orderSpecifier)
			.limit(request.size() + 1)
			.fetch();
	}

	@Override
	public Long getAnimalBoardsCount(ContentType contentType) {
		BooleanExpression typeCondition = createContentTypeCondition(contentType);

		return queryFactory
			.select(board.count())
			.from(board)
			.where(
				typeCondition,
				board.isDeleted.eq(false)
			)
			.fetchOne();
	}

	@Override
	public BoardDetailResponse getAnimalBoard(Long boardId) {
		QAnimal animal = QAnimal.animal;
		QBreedType bt = QBreedType.breedType;
		QMember member = QMember.member;
		QAnimalLocation animalLocation = QAnimalLocation.animalLocation;
		QAnimalS3Profile profileEntity = QAnimalS3Profile.animalS3Profile;
		QBoardTag boardTag = QBoardTag.boardTag;

		List<ProfileInfo> profiles = queryFactory
			.select(Projections.constructor(
				ProfileInfo.class,
				profileEntity.id,
				profileEntity.profile
			))
			.from(profileEntity)
			.where(profileEntity.animal.id.in(
				JPAExpressions.select(board.animal.id)
					.from(board)
					.where(board.id.eq(boardId), board.isDeleted.eq(false))
			))
			.orderBy(profileEntity.id.asc())
			.limit(3)
			.fetch();

		List<TagInfo> tags = queryFactory
			.select(Projections.constructor(
				TagInfo.class,
				boardTag.id,
				boardTag.name
			))
			.from(boardTag)
			.where(boardTag.board.id.eq(boardId))
			.fetch();

		return queryFactory
			.select(Projections.constructor(
				BoardDetailResponse.class,
				board.id,
				board.title,
				board.content,
				animal.species,
				bt.name,
				animal.sex,
				animal.color,
				animalLocation.address,
				Expressions.numberTemplate(Double.class, "ST_Y({0})", animalLocation.coordinates),
				Expressions.numberTemplate(Double.class, "ST_X({0})", animalLocation.coordinates),
				animal.animalType,
				member.nickName,
				board.createdAt,
				board.updatedAt,
				Expressions.constant(tags),
				Expressions.constant(profiles)
			))
			.from(board)
			.join(board.animal, animal)
			.join(board.member, member)
			.leftJoin(animal.animalLocation, animalLocation)
			.leftJoin(animal.breedType, bt)
			.where(board.id.eq(boardId), board.isDeleted.eq(false))
			.fetchOne();
	}

	@Override
	public long countValidImageIds(java.util.List<Long> imageIds, Long animalId) {
		QAnimalS3Profile profileEntity = QAnimalS3Profile.animalS3Profile;

		Long count = queryFactory
			.select(profileEntity.count())
			.from(profileEntity)
			.where(
				profileEntity.id.in(imageIds),
				profileEntity.animal.id.eq(animalId),
				profileEntity.isDeleted.eq(false)
			)
			.fetchOne();

		return count != null ? count : 0L;
	}

	@Override
	public long countValidTagIds(java.util.List<Long> tagIds, Long boardId) {
		QBoardTag boardTag = QBoardTag.boardTag;

		Long count = queryFactory
			.select(boardTag.count())
			.from(boardTag)
			.where(
				boardTag.id.in(tagIds),
				boardTag.board.id.eq(boardId),
				boardTag.isDeleted.eq(false)
			)
			.fetchOne();

		return count != null ? count : 0L;
	}

	@Override
	public Board findByIdWithAnimal(Long boardId) {
		QAnimal animal = QAnimal.animal;
		QAnimalLocation animalLocation = QAnimalLocation.animalLocation;
		QBreedType breedType = QBreedType.breedType;

		return queryFactory
			.selectFrom(board)
			.join(board.animal, animal).fetchJoin()
			.leftJoin(animal.animalLocation, animalLocation).fetchJoin()
			.leftJoin(animal.breedType, breedType).fetchJoin()
			.where(board.id.eq(boardId))
			.fetchOne();
	}

	private BooleanExpression createContentTypeCondition(ContentType contentType) {
		if (contentType == null) {
			return null;
		}
		return board.contentType.eq(contentType);
	}

	private BooleanExpression createCursorCondition(Long cursorId, SortDirection sortDirection) {
		if (cursorId == null) {
			return null;
		}

		return sortDirection == SortDirection.LATEST
			? board.id.lt(cursorId)
			: board.id.gt(cursorId);
	}

	private OrderSpecifier<Long> createOrderSpecifier(SortDirection sortDirection) {
		return sortDirection == SortDirection.LATEST
			? board.id.desc()
			: board.id.asc();
	}
}