package tetoandeggens.seeyouagainbe.board.repository.custom;

import static tetoandeggens.seeyouagainbe.board.entity.QBoard.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.animal.entity.NeuteredState;
import tetoandeggens.seeyouagainbe.animal.entity.QAnimal;
import tetoandeggens.seeyouagainbe.animal.entity.QAnimalLocation;
import tetoandeggens.seeyouagainbe.animal.entity.QAnimalS3Profile;
import tetoandeggens.seeyouagainbe.animal.entity.QBookMark;
import tetoandeggens.seeyouagainbe.animal.entity.QBreedType;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;
import tetoandeggens.seeyouagainbe.board.dto.response.BoardDetailResponse;
import tetoandeggens.seeyouagainbe.board.dto.response.BoardResponse;
import tetoandeggens.seeyouagainbe.board.dto.response.MyBoardResponse;
import tetoandeggens.seeyouagainbe.board.dto.response.ProfileInfo;
import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.board.entity.QBoardTag;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;
import tetoandeggens.seeyouagainbe.common.enums.ContentType;
import tetoandeggens.seeyouagainbe.member.entity.QMember;

@RequiredArgsConstructor
public class BoardRepositoryCustomImpl implements BoardRepositoryCustom {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final JPAQueryFactory queryFactory;

	@Override
	public List<BoardResponse> getAnimalBoards(
		CursorPageRequest request, SortDirection sortDirection, ContentType contentType,
		String startDate, String endDate, Species species, String breedType,
		NeuteredState neuteredState, Sex sex, String city, String town, Long memberId
	) {
		BooleanBuilder builder = createFilterConditions(contentType, startDate, endDate, species, breedType,
			neuteredState, sex, city, town);

		BooleanExpression cursorCondition = createCursorCondition(request.cursorId(), sortDirection);
		OrderSpecifier<Long> orderSpecifier = createOrderSpecifier(sortDirection);

		QAnimal animal = QAnimal.animal;
		QBreedType bt = QBreedType.breedType;
		QMember member = QMember.member;
		QAnimalS3Profile profileEntity = QAnimalS3Profile.animalS3Profile;
		QAnimalS3Profile subProfile = new QAnimalS3Profile("subProfile");
		QAnimalLocation animalLocation = QAnimalLocation.animalLocation;
		QBookMark bookMark = QBookMark.bookMark;

		BooleanBuilder bookMarkExistsCondition = createBookMarkCondition(bookMark, animal, memberId);

		return queryFactory
			.select(Projections.constructor(
				BoardResponse.class,
				board.id,
				board.title,
				animal.species,
				bt.name,
				animal.sex,
				animal.neuteredState,
				animalLocation.address,
				animal.city,
				animal.town,
				Expressions.numberTemplate(Double.class, "ST_Y({0})", animalLocation.coordinates),
				Expressions.numberTemplate(Double.class, "ST_X({0})", animalLocation.coordinates),
				animal.animalType,
				member.nickName,
				profileEntity.profile,
				board.createdAt,
				board.updatedAt,
				Expressions.constant(Collections.emptyList()),
				JPAExpressions.selectOne()
					.from(bookMark)
					.where(bookMarkExistsCondition)
					.exists()
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
				builder,
				cursorCondition,
				board.isDeleted.eq(false)
			)
			.orderBy(orderSpecifier)
			.limit(request.size() + 1)
			.fetch();
	}

	@Override
	public Long getAnimalBoardsCount(ContentType contentType, String startDate, String endDate, Species species,
		String breedType, NeuteredState neuteredState, Sex sex, String city, String town) {
		BooleanBuilder builder = createFilterConditions(contentType, startDate, endDate, species, breedType,
			neuteredState, sex, city, town);

		QAnimal animal = QAnimal.animal;
		QBreedType bt = QBreedType.breedType;

		return queryFactory
			.select(board.count())
			.from(board)
			.join(board.animal, animal)
			.leftJoin(animal.breedType, bt)
			.where(
				builder,
				board.isDeleted.eq(false)
			)
			.fetchOne();
	}

	@Override
	public BoardDetailResponse getAnimalBoard(Long boardId, Long memberId) {
		QAnimal animal = QAnimal.animal;
		QBreedType bt = QBreedType.breedType;
		QMember member = QMember.member;
		QAnimalLocation animalLocation = QAnimalLocation.animalLocation;
		QAnimalS3Profile profileEntity = QAnimalS3Profile.animalS3Profile;
		QBoardTag boardTag = QBoardTag.boardTag;
		QBookMark bookMark = QBookMark.bookMark;

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

		List<String> tags = queryFactory
			.select(boardTag.name)
			.from(boardTag)
			.where(boardTag.board.id.eq(boardId))
			.fetch();

		BooleanBuilder bookMarkExistsCondition = createBookMarkCondition(bookMark, animal, memberId);

		return queryFactory
			.select(Projections.constructor(
				BoardDetailResponse.class,
				board.id,
				board.title,
				board.content,
				animal.species,
				bt.name,
				animal.sex,
				animal.neuteredState,
				animal.color,
				animalLocation.address,
				animal.city,
				animal.town,
				Expressions.numberTemplate(Double.class, "ST_X({0})", animalLocation.coordinates),
				Expressions.numberTemplate(Double.class, "ST_Y({0})", animalLocation.coordinates),
				animal.animalType,
				member.nickName,
				board.createdAt,
				board.updatedAt,
				Expressions.constant(tags),
				Expressions.constant(profiles),
				JPAExpressions.selectOne()
					.from(bookMark)
					.where(bookMarkExistsCondition)
					.exists()
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
	public long countValidImageIds(List<Long> imageIds, Long animalId) {
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
	public long countValidTagIds(List<Long> tagIds, Long boardId) {
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

	@Override
	public Board findByIdWithMember(Long boardId) {
		QMember member = QMember.member;

		return queryFactory
			.selectFrom(board)
			.join(board.member, member).fetchJoin()
			.where(board.id.eq(boardId), board.isDeleted.eq(false))
			.fetchOne();
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

	private BooleanBuilder createBookMarkCondition(QBookMark bookMark, QAnimal animal, Long memberId) {
		BooleanBuilder condition = new BooleanBuilder();
		condition.and(bookMark.animal.eq(animal));
		if (memberId != null) {
			condition.and(bookMark.member.id.eq(memberId));
		}
		condition.and(bookMark.isDeleted.eq(false));
		return condition;
	}

	private BooleanBuilder createFilterConditions(ContentType contentType, String startDate, String endDate,
		Species species, String breedType, NeuteredState neuteredState, Sex sex, String city, String town) {

		QAnimal animal = QAnimal.animal;
		BooleanBuilder builder = new BooleanBuilder();

		if (contentType != null) {
			builder.and(board.contentType.eq(contentType));
		}

		if (startDate != null && !startDate.isBlank()) {
			LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
			builder.and(board.createdAt.goe(start.atStartOfDay()));
		}

		if (endDate != null && !endDate.isBlank()) {
			LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
			builder.and(board.createdAt.loe(end.plusDays(1).atStartOfDay()));
		}

		if (species != null) {
			builder.and(animal.species.eq(species));
		}

		if (breedType != null && !breedType.isBlank()) {
			builder.and(animal.breedType.name.eq(breedType));
		}

		if (neuteredState != null) {
			builder.and(animal.neuteredState.eq(neuteredState));
		}

		if (sex != null) {
			builder.and(animal.sex.eq(sex));
		}

		if (city != null && !city.isBlank()) {
			builder.and(animal.city.eq(city));
		}

		if (town != null && !town.isBlank()) {
			builder.and(animal.town.eq(town));
		}

		return builder;
	}

	@Override
	public List<MyBoardResponse> getMyBoards(CursorPageRequest request, SortDirection sortDirection, Long memberId) {
		BooleanExpression cursorCondition = createCursorCondition(request.cursorId(), sortDirection);
		OrderSpecifier<Long> orderSpecifier = createOrderSpecifier(sortDirection);

		QAnimal animal = QAnimal.animal;
		QAnimalLocation animalLocation = QAnimalLocation.animalLocation;
		QAnimalS3Profile profileEntity = QAnimalS3Profile.animalS3Profile;
		QAnimalS3Profile subProfile = new QAnimalS3Profile("subProfile");

		return queryFactory
			.select(Projections.constructor(
				MyBoardResponse.class,
				board.id,
				animal.animalType,
				board.title,
				animalLocation.address,
				board.createdAt,
				board.updatedAt,
				Expressions.constant(Collections.emptyList()), // tags는 서비스에서 채움
				profileEntity.profile
			))
			.from(board)
			.join(board.animal, animal)
			.leftJoin(animal.animalLocation, animalLocation)
			.leftJoin(profileEntity).on(
				profileEntity.animal.eq(animal),
				profileEntity.id.eq(
					JPAExpressions.select(subProfile.id.min())
						.from(subProfile)
						.where(subProfile.animal.eq(animal))
				)
			)
			.where(
				board.member.id.eq(memberId),
				cursorCondition,
				board.isDeleted.eq(false)
			)
			.orderBy(orderSpecifier)
			.limit(request.size() + 1)
			.fetch();
	}

	@Override
	public Long getMyBoardsCount(Long memberId) {
		return queryFactory
			.select(board.count())
			.from(board)
			.where(
				board.member.id.eq(memberId),
				board.isDeleted.eq(false)
			)
			.fetchOne();
	}
}