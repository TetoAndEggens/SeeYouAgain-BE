package tetoandeggens.seeyouagainbe.animal.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tetoandeggens.seeyouagainbe.global.entity.BaseEntity;

@Entity
@Table(name = "ANIMAL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Animal extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "animal_id")
	private Long id;

	@Column(name = "desertion_no")
	private String desertionNo;

	@Column(name = "happen_date")
	private LocalDate happenDate;

	@Column(name = "happen_place")
	private String happenPlace;

	@Enumerated(EnumType.STRING)
	@Column(name = "animal_type")
	private AnimalType animalType;

	@Column(name = "city")
	private String city;

	@Column(name = "town")
	private String town;

	@Enumerated(EnumType.STRING)
	@Column(name = "species")
	private Species species;

	@Column(name = "color")
	private String color;

	@Column(name = "birth")
	private String birth;

	@Column(name = "weight")
	private String weight;

	@Column(name = "notice_no")
	private String noticeNo;

	@Column(name = "notice_start_date")
	private String noticeStartDate;

	@Column(name = "notice_end_date")
	private String noticeEndDate;

	@Column(name = "process_state")
	private String processState;

	@Enumerated(EnumType.STRING)
	@Column(name = "sex")
	private Sex sex;

	@Enumerated(EnumType.STRING)
	@Column(name = "neutered_state")
	private NeuteredState neuteredState;

	@Column(name = "special_mark")
	private String specialMark;

	@Column(name = "center_phone")
	private String centerPhone;

	@Column(name = "final_updated_at")
	private LocalDateTime finalUpdatedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "animal_location_id")
	private AnimalLocation animalLocation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "breed_type_id")
	private BreedType breedType;

	@Column(name = "is_deleted")
	private Boolean isDeleted;

	@Builder
	public Animal(String desertionNo, LocalDate happenDate, String happenPlace, AnimalType animalType,
		String city, String town, Species species, String color, String birth, String weight, String noticeNo,
		String noticeStartDate, String noticeEndDate, String processState, Sex sex, NeuteredState neuteredState,
		String specialMark, String centerPhone, LocalDateTime finalUpdatedAt, AnimalLocation animalLocation,
		BreedType breedType) {
		this.desertionNo = desertionNo;
		this.happenDate = happenDate;
		this.happenPlace = happenPlace;
		this.animalType = animalType;
		this.city = city;
		this.town = town;
		this.species = species;
		this.color = color;
		this.birth = birth;
		this.weight = weight;
		this.noticeNo = noticeNo;
		this.noticeStartDate = noticeStartDate;
		this.noticeEndDate = noticeEndDate;
		this.processState = processState;
		this.sex = sex;
		this.neuteredState = neuteredState;
		this.specialMark = specialMark;
		this.centerPhone = centerPhone;
		this.finalUpdatedAt = finalUpdatedAt;
		this.animalLocation = animalLocation;
		this.breedType = breedType;
		this.isDeleted = false;
	}
}