package tetoandeggens.seeyouagainbe.animal.repository.custom;

import static tetoandeggens.seeyouagainbe.animal.entity.QAnimal.*;

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
import tetoandeggens.seeyouagainbe.animal.dto.response.AnimalDetailResponse;
import tetoandeggens.seeyouagainbe.animal.dto.response.AnimalResponse;
import tetoandeggens.seeyouagainbe.animal.entity.AnimalType;
import tetoandeggens.seeyouagainbe.animal.entity.NeuteredState;
import tetoandeggens.seeyouagainbe.animal.entity.QAnimalLocation;
import tetoandeggens.seeyouagainbe.animal.entity.QAnimalS3Profile;
import tetoandeggens.seeyouagainbe.animal.entity.QBreedType;
import tetoandeggens.seeyouagainbe.animal.entity.Sex;
import tetoandeggens.seeyouagainbe.animal.entity.Species;
import tetoandeggens.seeyouagainbe.common.dto.CursorPageRequest;
import tetoandeggens.seeyouagainbe.common.dto.SortDirection;

@RequiredArgsConstructor
public class AnimalRepositoryCustomImpl implements AnimalRepositoryCustom {

	private final JPAQueryFactory queryFactory;
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

	@Override
	public List<AnimalResponse> getAbandonedAnimals(
		CursorPageRequest request, SortDirection sortDirection, AnimalType animalType,
		String startDate, String endDate, Species species, String breedType,
		NeuteredState neuteredState, Sex sex, String city, String town
	) {

		BooleanBuilder builder = createFilterConditions(animalType, startDate, endDate, species, breedType,
			neuteredState, sex,
			city, town);

		BooleanExpression cursorCondition = createCursorCondition(request.cursorId(), sortDirection);
		OrderSpecifier<Long> orderSpecifier = createOrderSpecifier(sortDirection);

		QBreedType bt = QBreedType.breedType;
		QAnimalS3Profile profileEntity = QAnimalS3Profile.animalS3Profile;
		QAnimalS3Profile subProfile = new QAnimalS3Profile("subProfile");

		return queryFactory
			.select(Projections.constructor(
				AnimalResponse.class,
				animal.id,
				animal.happenDate,
				animal.species,
				bt.name,
				animal.birth,
				animal.city,
				animal.town,
				animal.sex,
				animal.processState,
				profileEntity.profile,
				animal.animalType
			))
			.from(animal)
			.leftJoin(animal.breedType, bt)
			.leftJoin(profileEntity).on(
				profileEntity.animal.eq(animal),
				profileEntity.id.eq(
					JPAExpressions.select(subProfile.id.min())
						.from(subProfile)
						.where(subProfile.animal.eq(animal))
				)
			)
			.where(builder, cursorCondition)
			.orderBy(orderSpecifier)
			.limit(request.size() + 1)
			.fetch();
	}

	@Override
	public Long getAbandonedAnimalsCount(AnimalType animalType, String startDate, String endDate, Species species,
		String breedType,
		NeuteredState neuteredState, Sex sex, String city, String town) {

		BooleanBuilder builder = createFilterConditions(animalType, startDate, endDate, species, breedType,
			neuteredState, sex,
			city, town);

		return queryFactory
			.select(animal.count())
			.from(animal)
			.leftJoin(animal.breedType, QBreedType.breedType)
			.where(builder)
			.fetchOne();
	}

	@Override
	public AnimalDetailResponse getAnimal(Long animalId) {
		QBreedType bt = QBreedType.breedType;
		QAnimalLocation al = QAnimalLocation.animalLocation;
		QAnimalS3Profile profileEntity = QAnimalS3Profile.animalS3Profile;

		List<String> profiles = queryFactory
			.select(profileEntity.profile)
			.from(profileEntity)
			.where(profileEntity.animal.id.eq(animalId))
			.orderBy(profileEntity.id.asc())
			.limit(3)
			.fetch();

		return queryFactory
			.select(Projections.constructor(
				AnimalDetailResponse.class,
				animal.id,
				animal.animalType,
				animal.happenDate,
				animal.species,
				bt.name,
				animal.birth,
				animal.happenPlace,
				animal.sex,
				animal.processState,
				Expressions.constant(profiles),
				animal.color,
				animal.noticeNo,
				animal.noticeStartDate,
				animal.noticeEndDate,
				animal.specialMark,
				animal.weight,
				animal.neuteredState,
				al.name,
				al.address,
				animal.centerPhone
			))
			.from(animal)
			.leftJoin(animal.breedType, bt)
			.leftJoin(animal.animalLocation, al)
			.where(animal.id.eq(animalId))
			.fetchOne();
	}

	@Override
	public List<AnimalResponse> getAnimalListWithCoordinates(
		CursorPageRequest request, SortDirection sortDirection, AnimalType animalType,
		Double minLongitude, Double minLatitude, Double maxLongitude, Double maxLatitude,
		String startDate, String endDate, Species species, String breedType,
		NeuteredState neuteredState, Sex sex, String city, String town
	) {
		QAnimalLocation al = QAnimalLocation.animalLocation;

		BooleanExpression withinBounds = createWithinBoundsCondition(al, minLongitude, minLatitude, maxLongitude,
			maxLatitude);

		BooleanBuilder builder = createFilterConditions(
			animalType, startDate, endDate, species, breedType, neuteredState, sex, city, town
		);
		builder.and(withinBounds);

		BooleanExpression cursorCondition = createCursorCondition(request.cursorId(), sortDirection);
		OrderSpecifier<Long> orderSpecifier = createOrderSpecifier(sortDirection);

		QBreedType bt = QBreedType.breedType;
		QAnimalS3Profile profileEntity = QAnimalS3Profile.animalS3Profile;
		QAnimalS3Profile subProfile = new QAnimalS3Profile("subProfile");

		return queryFactory
			.select(Projections.constructor(
				AnimalResponse.class,
				animal.id,
				animal.happenDate,
				animal.species,
				bt.name,
				animal.birth,
				animal.city,
				animal.town,
				animal.sex,
				animal.processState,
				profileEntity.profile,
				animal.animalType
			))
			.from(animal)
			.innerJoin(animal.animalLocation, al)
			.innerJoin(animal.breedType, bt)
			.leftJoin(profileEntity).on(
				profileEntity.animal.eq(animal),
				profileEntity.id.eq(
					JPAExpressions.select(subProfile.id.min())
						.from(subProfile)
						.where(subProfile.animal.eq(animal))
				)
			)
			.where(builder, cursorCondition)
			.orderBy(orderSpecifier)
			.limit(request.size() + 1)
			.fetch();
	}

	@Override
	public Long getAnimalsCountWithCoordinates(
		AnimalType animalType, Double minLongitude, Double minLatitude, Double maxLongitude, Double maxLatitude,
		String startDate, String endDate, Species species, String breedType,
		NeuteredState neuteredState, Sex sex, String city, String town
	) {
		QAnimalLocation al = QAnimalLocation.animalLocation;

		BooleanExpression withinBounds = createWithinBoundsCondition(al, minLongitude, minLatitude, maxLongitude,
			maxLatitude);

		BooleanBuilder builder = createFilterConditions(
			animalType, startDate, endDate, species, breedType, neuteredState, sex, city, town
		);
		builder.and(withinBounds);

		return queryFactory
			.select(animal.count())
			.from(animal)
			.innerJoin(animal.animalLocation, al)
			.leftJoin(animal.breedType, QBreedType.breedType)
			.where(builder)
			.fetchOne();
	}

	private BooleanBuilder createFilterConditions(AnimalType animalType, String startDate, String endDate,
		Species species, String breedType,
		NeuteredState neuteredState, Sex sex, String city, String town) {

		BooleanBuilder builder = new BooleanBuilder();

		if (startDate != null && !startDate.isBlank()) {
			LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
			builder.and(animal.happenDate.goe(start));
		}

		if (endDate != null && !endDate.isBlank()) {
			LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
			builder.and(animal.happenDate.loe(end));
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

		if (animalType != null) {
			builder.and(animal.animalType.eq(animalType));
		}

		return builder;
	}

	private BooleanExpression createCursorCondition(Long cursorId, SortDirection sortDirection) {
		if (cursorId == null) {
			return null;
		}
		return (sortDirection == SortDirection.LATEST)
			? animal.id.lt(cursorId)
			: animal.id.gt(cursorId);
	}

	private OrderSpecifier<Long> createOrderSpecifier(SortDirection sortDirection) {
		return (sortDirection == SortDirection.LATEST)
			? animal.id.desc()
			: animal.id.asc();
	}

	private BooleanExpression createWithinBoundsCondition(
		QAnimalLocation al, Double minLongitude, Double minLatitude, Double maxLongitude, Double maxLatitude
	) {
		return Expressions.booleanTemplate(
			"ST_X({0}) BETWEEN {1} AND {2} AND ST_Y({0}) BETWEEN {3} AND {4}",
			al.coordinates,
			minLatitude,
			maxLatitude,
			minLongitude,
			maxLongitude
		);
	}
}