package tetoandeggens.seeyouagainbe.animal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tetoandeggens.seeyouagainbe.global.entity.BaseEntity;
import tetoandeggens.seeyouagainbe.member.entity.Member;

@Entity
@Table(name = "BOOK_MARK")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookMark extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "book_mark_id")
	private Long id;

	@Column(name = "is_deleted")
	private Boolean isDeleted;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "abandoned_animal_id")
	private AbandonedAnimal abandonedAnimal;
}