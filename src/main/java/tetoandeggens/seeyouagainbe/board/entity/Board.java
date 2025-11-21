package tetoandeggens.seeyouagainbe.board.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tetoandeggens.seeyouagainbe.animal.entity.Animal;
import tetoandeggens.seeyouagainbe.common.enums.ContentType;
import tetoandeggens.seeyouagainbe.common.enums.ViolatedStatus;
import tetoandeggens.seeyouagainbe.global.entity.BaseEntity;
import tetoandeggens.seeyouagainbe.member.entity.Member;

@Entity
@Table(name = "BOARD")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "board_id")
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "type")
	private ContentType contentType;

	@Column(name = "title")
	private String title;

	@Column(name = "content")
	private String content;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "animal_id")
	private Animal animal;

	@Column(name = "is_deleted")
	private Boolean isDeleted;

	@Enumerated(EnumType.STRING)
	@Column(name = "violated_status")
	private ViolatedStatus violatedStatus;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private Member member;

	@Builder
	public Board(ContentType contentType, String title, String content, Animal animal,
		Member member) {
		this.contentType = contentType;
		this.title = title;
		this.content = content;
		this.animal = animal;
		this.isDeleted = false;
		this.violatedStatus = ViolatedStatus.NORMAL;
		this.member = member;
	}

	public void updateIsDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	public void updateTitle(String title) {
		this.title = title;
	}

	public void updateContent(String content) {
		this.content = content;
	}

	public void updateContentType(ContentType contentType) {
		this.contentType = contentType;
	}
}