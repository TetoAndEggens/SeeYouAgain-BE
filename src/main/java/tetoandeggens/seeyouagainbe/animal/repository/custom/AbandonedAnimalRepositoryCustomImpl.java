package tetoandeggens.seeyouagainbe.animal.repository.custom;

import static tetoandeggens.seeyouagainbe.animal.entity.QAbandonedAnimal.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import tetoandeggens.seeyouagainbe.animal.dto.response.AbandonedAnimalDetailResponse;
import tetoandeggens.seeyouagainbe.animal.dto.response.AbandonedAnimalResponse;
import tetoandeggens.seeyouagainbe.animal.entity.NeuteredState;
import tetoandeggens.seeyouagainbe.animal.entity.QAbandonedAnimalS3Profile;
import tetoandeggens.seeyouagainbe.animal.entity.QBreedType;
import tetoandeggens.seeyouagainbe.animal.entity.QCenterLocation;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;

@RequiredArgsConstructor
public class AbandonedAnimalRepositoryCustomImpl implements AbandonedAnimalRepositoryCustom {

	private final JPAQueryFactory queryFactory;
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

	@Override
	public List<AbandonedAnimalResponse> getAbandonedAnimals(
		CursorPageRequest request, SortDirection sortDirection,
		String startDate, String endDate, Species species, String breedType,
		NeuteredState neuteredState, Sex sex, String city, String town
	) {

		BooleanBuilder builder = createFilterConditions(startDate, endDate, species, breedType, neuteredState, sex,
			city, town);

		BooleanExpression cursorCondition = null;
		if (request.cursorId() != null) {
			cursorCondition = (sortDirection == SortDirection.LATEST)
				? abandonedAnimal.id.lt(request.cursorId())
				: abandonedAnimal.id.gt(request.cursorId());
		}

		OrderSpecifier<Long> orderSpecifier = (sortDirection == SortDirection.LATEST)
			? abandonedAnimal.id.desc()
			: abandonedAnimal.id.asc();

		QBreedType bt = QBreedType.breedType;
		QAbandonedAnimalS3Profile profileEntity = QAbandonedAnimalS3Profile.abandonedAnimalS3Profile;
		QAbandonedAnimalS3Profile subProfile = new QAbandonedAnimalS3Profile("subProfile");

		return queryFactory
			.select(Projections.constructor(
				AbandonedAnimalResponse.class,
				abandonedAnimal.id,
				abandonedAnimal.happenDate,
				abandonedAnimal.species,
				bt.name,
				abandonedAnimal.birth,
				abandonedAnimal.city,
				abandonedAnimal.town,
				abandonedAnimal.sex,
				abandonedAnimal.processState,
				profileEntity.profile
			))
			.from(abandonedAnimal)
			.leftJoin(abandonedAnimal.breedType, bt)
			.leftJoin(profileEntity).on(
				profileEntity.abandonedAnimal.eq(abandonedAnimal),
				profileEntity.id.eq(
					JPAExpressions.select(subProfile.id.min())
						.from(subProfile)
						.where(subProfile.abandonedAnimal.eq(abandonedAnimal))
				)
			)
			.where(builder, cursorCondition)
			.orderBy(orderSpecifier)
			.limit(request.size() + 1)
			.fetch();
	}

	@Override
	public Long getAbandonedAnimalsCount(String startDate, String endDate, Species species, String breedType,
		NeuteredState neuteredState, Sex sex, String city, String town) {

		BooleanBuilder builder = createFilterConditions(startDate, endDate, species, breedType, neuteredState, sex,
			city, town);

		return queryFactory
			.select(abandonedAnimal.count())
			.from(abandonedAnimal)
			.leftJoin(abandonedAnimal.breedType, QBreedType.breedType)
			.where(builder)
			.fetchOne();
	}

	@Override
	public AbandonedAnimalDetailResponse getAbandonedAnimal(Long abandonedAnimalId) {
		QBreedType bt = QBreedType.breedType;
		QCenterLocation cl = QCenterLocation.centerLocation;
		QAbandonedAnimalS3Profile profileEntity = QAbandonedAnimalS3Profile.abandonedAnimalS3Profile;

		List<String> profiles = queryFactory
			.select(profileEntity.profile)
			.from(profileEntity)
			.where(profileEntity.abandonedAnimal.id.eq(abandonedAnimalId))
			.orderBy(profileEntity.id.asc())
			.limit(3)
			.fetch();

		return queryFactory
			.select(Projections.constructor(
				AbandonedAnimalDetailResponse.class,
				abandonedAnimal.id,
				abandonedAnimal.happenDate,
				abandonedAnimal.species,
				bt.name,
				abandonedAnimal.birth,
				abandonedAnimal.happenPlace,
				abandonedAnimal.sex,
				abandonedAnimal.processState,
				Expressions.constant(profiles),
				abandonedAnimal.color,
				abandonedAnimal.noticeNo,
				abandonedAnimal.noticeStartDate,
				abandonedAnimal.noticeEndDate,
				abandonedAnimal.specialMark,
				abandonedAnimal.weight,
				abandonedAnimal.neuteredState,
				cl.name,
				cl.address,
				abandonedAnimal.centerPhone
			))
			.from(abandonedAnimal)
			.leftJoin(abandonedAnimal.breedType, bt)
			.leftJoin(abandonedAnimal.centerLocation, cl)
			.where(abandonedAnimal.id.eq(abandonedAnimalId))
			.fetchOne();
	}

	@Override
	public List<AbandonedAnimalResponse> getAbandonedAnimalListWithCoordinates(
		CursorPageRequest request, SortDirection sortDirection,
		Double minLongitude, Double minLatitude, Double maxLongitude, Double maxLatitude,
		String startDate, String endDate, Species species, String breedType,
		NeuteredState neuteredState, Sex sex, String city, String town
	) {
		QCenterLocation cl = QCenterLocation.centerLocation;

		BooleanExpression withinBounds = Expressions.booleanTemplate(
			"ST_X({0}) BETWEEN {1} AND {2} AND ST_Y({0}) BETWEEN {3} AND {4}",
			cl.coordinates,
			minLatitude,
			maxLatitude,
			minLongitude,
			maxLongitude
		);

		BooleanBuilder builder = createFilterConditionsWithCoordinates(
			startDate, endDate, species, breedType, neuteredState, sex, city, town
		);
		builder.and(withinBounds);

		BooleanExpression cursorCondition = null;
		if (request.cursorId() != null) {
			cursorCondition = (sortDirection == SortDirection.LATEST)
				? abandonedAnimal.id.lt(request.cursorId())
				: abandonedAnimal.id.gt(request.cursorId());
		}

		OrderSpecifier<Long> orderSpecifier = (sortDirection == SortDirection.LATEST)
			? abandonedAnimal.id.desc()
			: abandonedAnimal.id.asc();

		QBreedType bt = QBreedType.breedType;
		QAbandonedAnimalS3Profile profileEntity = QAbandonedAnimalS3Profile.abandonedAnimalS3Profile;
		QAbandonedAnimalS3Profile subProfile = new QAbandonedAnimalS3Profile("subProfile");

		return queryFactory
			.select(Projections.constructor(
				AbandonedAnimalResponse.class,
				abandonedAnimal.id,
				abandonedAnimal.happenDate,
				abandonedAnimal.species,
				bt.name,
				abandonedAnimal.birth,
				abandonedAnimal.city,
				abandonedAnimal.town,
				abandonedAnimal.sex,
				abandonedAnimal.processState,
				profileEntity.profile
			))
			.from(abandonedAnimal)
			.innerJoin(abandonedAnimal.centerLocation, cl)
			.innerJoin(abandonedAnimal.breedType, bt)
			.leftJoin(profileEntity).on(
				profileEntity.abandonedAnimal.eq(abandonedAnimal),
				profileEntity.id.eq(
					JPAExpressions.select(subProfile.id.min())
						.from(subProfile)
						.where(subProfile.abandonedAnimal.eq(abandonedAnimal))
				)
			)
			.where(builder, cursorCondition)
			.orderBy(orderSpecifier)
			.limit(request.size() + 1)
			.fetch();
	}

	@Override
	public Long getAbandonedAnimalsCountWithCoordinates(
		Double minLongitude, Double minLatitude, Double maxLongitude, Double maxLatitude,
		String startDate, String endDate, Species species, String breedType,
		NeuteredState neuteredState, Sex sex, String city, String town
	) {
		QCenterLocation cl = QCenterLocation.centerLocation;

		BooleanExpression withinBounds = Expressions.booleanTemplate(
			"ST_X({0}) BETWEEN {1} AND {2} AND ST_Y({0}) BETWEEN {3} AND {4}",
			cl.coordinates,
			minLatitude,
			maxLatitude,
			minLongitude,
			maxLongitude
		);

		BooleanBuilder builder = createFilterConditionsWithCoordinates(
			startDate, endDate, species, breedType, neuteredState, sex, city, town
		);
		builder.and(withinBounds);

		return queryFactory
			.select(abandonedAnimal.count())
			.from(abandonedAnimal)
			.innerJoin(abandonedAnimal.centerLocation, cl)
			.leftJoin(abandonedAnimal.breedType, QBreedType.breedType)
			.where(builder)
			.fetchOne();
	}

	private BooleanBuilder createFilterConditions(String startDate, String endDate, Species species, String breedType,
		NeuteredState neuteredState, Sex sex, String city, String town) {

		BooleanBuilder builder = new BooleanBuilder();

		if (startDate != null && !startDate.isBlank()) {
			LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
			builder.and(abandonedAnimal.happenDate.goe(start));
		}

		if (endDate != null && !endDate.isBlank()) {
			LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
			builder.and(abandonedAnimal.happenDate.loe(end));
		}

		if (species != null) {
			builder.and(abandonedAnimal.species.eq(species));
		}

		if (breedType != null && !breedType.isBlank()) {
			builder.and(abandonedAnimal.breedType.name.eq(breedType));
		}

		if (neuteredState != null) {
			builder.and(abandonedAnimal.neuteredState.eq(neuteredState));
		}

		if (sex != null) {
			builder.and(abandonedAnimal.sex.eq(sex));
		}

		if (city != null && !city.isBlank()) {
			builder.and(abandonedAnimal.city.eq(city));
		}

		if (town != null && !town.isBlank()) {
			builder.and(abandonedAnimal.town.eq(town));
		}

		return builder;
	}

	private BooleanBuilder createFilterConditionsWithCoordinates(
		String startDate, String endDate, Species species, String breedType,
		NeuteredState neuteredState, Sex sex, String city, String town
	) {
		BooleanBuilder builder = new BooleanBuilder();

		if (startDate != null && !startDate.isBlank()) {
			LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
			builder.and(abandonedAnimal.happenDate.goe(start));
		}

		if (endDate != null && !endDate.isBlank()) {
			LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
			builder.and(abandonedAnimal.happenDate.loe(end));
		}

		if (species != null) {
			builder.and(abandonedAnimal.species.eq(species));
		}

		if (breedType != null && !breedType.isBlank()) {
			builder.and(abandonedAnimal.breedType.name.eq(breedType));
		}

		if (neuteredState != null) {
			builder.and(abandonedAnimal.neuteredState.eq(neuteredState));
		}

		if (sex != null) {
			builder.and(abandonedAnimal.sex.eq(sex));
		}

		if (city != null && !city.isBlank()) {
			builder.and(abandonedAnimal.city.eq(city));
		}

		if (town != null && !town.isBlank()) {
			builder.and(abandonedAnimal.town.eq(town));
		}

		return builder;
	}
}
