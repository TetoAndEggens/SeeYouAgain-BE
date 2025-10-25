package tetoandeggens.seeyouagainbe.chat.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import tetoandeggens.seeyouagainbe.common.enums.ContentType;
import tetoandeggens.seeyouagainbe.common.enums.ImageType;
import tetoandeggens.seeyouagainbe.global.entity.BaseEntity;
import tetoandeggens.seeyouagainbe.member.entity.Member;

@Entity
@Table(name = "CHAT_MESSAGE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "chat_message_id")
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "message_type")
	private MessageType messageType;

	@Column(name = "content")
	private String content;

	@Column(name = "image_key", unique = true)
	private String imageKey;

	@Enumerated(EnumType.STRING)
	@Column(name = "image_type")
	private ImageType imageType;

	@Column(name = "image_size")
	private Double imageSize;

	@Column(name = "is_read")
	private Boolean isRead;

	@Column(name = "is_deleted")
	private Boolean isDeleted;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sender_id")
	private Member sender;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "chat_room_id")
	private ChatRoom chatRoom;
}