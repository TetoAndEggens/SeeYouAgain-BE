package tetoandeggens.seeyouagainbe.violation.entity;

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
import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;
import tetoandeggens.seeyouagainbe.common.enums.ReportReason;
import tetoandeggens.seeyouagainbe.common.enums.ViolatedStatus;
import tetoandeggens.seeyouagainbe.global.entity.BaseEntity;
import tetoandeggens.seeyouagainbe.member.entity.Member;

@Entity
@Table(name = "VIOLATION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Violation extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "violation_id")
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "violated_status")
	private ViolatedStatus violatedStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "reason")
	private ReportReason reason;

	@Column(name = "detail_reason")
	private String detailReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id") // 신고자
    private Member reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_member_id") // 피신고자
    private Member reportedMember;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "board_id")
	private Board board;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "chat_room_id")
	private ChatRoom chatRoom;

    @Builder
    public Violation(ViolatedStatus violatedStatus, ReportReason reason, String detailReason,
                     Member reporter, Member reportedMember, Board board, ChatRoom chatRoom) {
        this.violatedStatus = violatedStatus;
        this.reason = reason;
        this.detailReason = detailReason;
        this.reporter = reporter;
        this.reportedMember = reportedMember;
        this.board = board;
        this.chatRoom = chatRoom;
    }

    public void updateViolatedStatus(ViolatedStatus status) {
        this.violatedStatus = status;
    }

    public boolean isBoard() {
        return this.board != null;
    }

    public boolean isChatRoom() {
        return this.chatRoom != null;
    }
}