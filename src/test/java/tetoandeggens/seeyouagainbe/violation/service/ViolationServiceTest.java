package tetoandeggens.seeyouagainbe.violation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.board.repository.BoardRepository;
import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;
import tetoandeggens.seeyouagainbe.chat.repository.ChatRoomRepository;
import tetoandeggens.seeyouagainbe.common.enums.ReportReason;
import tetoandeggens.seeyouagainbe.common.enums.ViolatedStatus;
import tetoandeggens.seeyouagainbe.global.ServiceTest;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.ViolationErrorCode;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;
import tetoandeggens.seeyouagainbe.violation.dto.request.ViolationCreateRequest;
import tetoandeggens.seeyouagainbe.violation.entity.Violation;
import tetoandeggens.seeyouagainbe.violation.repository.ViolationRepository;

@DisplayName("ViolationService 단위 테스트")
class ViolationServiceTest extends ServiceTest {

    private static final Long REPORTER_MEMBER_ID = 1L;
    private static final Long REPORTED_MEMBER_ID = 2L;

    @Autowired
    private ViolationService violationService;

    @MockitoBean
    private ViolationRepository violationRepository;

    @MockitoBean
    private BoardRepository boardRepository;

    @MockitoBean
    private ChatRoomRepository chatRoomRepository;

    private Member reporter;
    private Member reportedMember;
    private Board board;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        reporter = Member.builder()
                .loginId("reporter")
                .password("password123!")
                .nickName("신고자")
                .phoneNumber("01012345678")
                .build();
        ReflectionTestUtils.setField(reporter, "id", REPORTER_MEMBER_ID);

        reportedMember = Member.builder()
                .loginId("reported")
                .password("password123!")
                .nickName("피신고자")
                .phoneNumber("01087654321")
                .build();
        ReflectionTestUtils.setField(reportedMember, "id", REPORTED_MEMBER_ID);

        board = createBoard(reportedMember, "테스트 게시글", "테스트 내용");
        ReflectionTestUtils.setField(board, "id", 1L);

        chatRoom = createChatRoom(reporter, reportedMember);
        ReflectionTestUtils.setField(chatRoom, "id", 1L);
    }

    private Board createBoard(Member member, String title, String content) {
        try {
            java.lang.reflect.Constructor<Board> constructor = Board.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Board board = constructor.newInstance();
            ReflectionTestUtils.setField(board, "member", member);
            ReflectionTestUtils.setField(board, "title", title);
            ReflectionTestUtils.setField(board, "content", content);
            return board;
        } catch (Exception e) {
            throw new RuntimeException("Board 생성 실패", e);
        }
    }

    private ChatRoom createChatRoom(Member sender, Member receiver) {
        try {
            java.lang.reflect.Constructor<ChatRoom> constructor = ChatRoom.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            ChatRoom chatRoom = constructor.newInstance();
            ReflectionTestUtils.setField(chatRoom, "sender", sender);
            ReflectionTestUtils.setField(chatRoom, "receiver", receiver);
            return chatRoom;
        } catch (Exception e) {
            throw new RuntimeException("ChatRoom 생성 실패", e);
        }
    }

    @Nested
    @DisplayName("신고 생성 테스트 - 게시글")
    class CreateViolationBoardTests {

        @Test
        @DisplayName("게시글 신고 생성 - 성공")
        void createViolation_Board_Success() {
            // given
            ViolationCreateRequest request = new ViolationCreateRequest(
                    1L,
                    null,
                    ReportReason.SPAM,
                    "스팸 게시글입니다."
            );

            given(violationRepository.existsByReporterAndBoard(REPORTER_MEMBER_ID, 1L))
                    .willReturn(false);
            given(boardRepository.findById(1L))
                    .willReturn(Optional.of(board));
            given(violationRepository.save(any(Violation.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when & then
            assertThatCode(() -> violationService.createViolation(REPORTER_MEMBER_ID, request))
                    .doesNotThrowAnyException();

            verify(violationRepository).save(any(Violation.class));
        }

        @Test
        @DisplayName("게시글 신고 생성 - 게시글을 찾을 수 없으면 예외 발생")
        void createViolation_Board_BoardNotFound_ThrowsException() {
            // given
            ViolationCreateRequest request = new ViolationCreateRequest(
                    1L,
                    null,
                    ReportReason.SPAM,
                    "스팸 게시글입니다."
            );

            given(violationRepository.existsByReporterAndBoard(REPORTER_MEMBER_ID, 1L))
                    .willReturn(false);
            given(boardRepository.findById(1L))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> violationService.createViolation(REPORTER_MEMBER_ID, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ViolationErrorCode.BOARD_NOT_FOUND);

            verify(violationRepository, never()).save(any(Violation.class));
        }

        @Test
        @DisplayName("게시글 신고 생성 - 중복 신고면 예외 발생")
        void createViolation_Board_Duplicate_ThrowsException() {
            // given
            ViolationCreateRequest request = new ViolationCreateRequest(
                    1L,
                    null,
                    ReportReason.SPAM,
                    "스팸 게시글입니다."
            );

            given(violationRepository.existsByReporterAndBoard(REPORTER_MEMBER_ID, 1L))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> violationService.createViolation(REPORTER_MEMBER_ID, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ViolationErrorCode.DUPLICATE_VIOLATION);

            verify(boardRepository, never()).findById(anyLong());
            verify(violationRepository, never()).save(any(Violation.class));
        }

        @Test
        @DisplayName("게시글 신고 생성 - 자기 자신을 신고하면 예외 발생")
        void createViolation_Board_SelfReport_ThrowsException() {
            // given
            ViolationCreateRequest request = new ViolationCreateRequest(
                    1L,
                    null,
                    ReportReason.SPAM,
                    "스팸 게시글입니다."
            );

            Board selfBoard = createBoard(reporter, "내 게시글", "내용");
            ReflectionTestUtils.setField(selfBoard, "id", 1L);

            given(violationRepository.existsByReporterAndBoard(REPORTER_MEMBER_ID, 1L))
                    .willReturn(false);
            given(boardRepository.findById(1L))
                    .willReturn(Optional.of(selfBoard));

            // when & then
            assertThatThrownBy(() -> violationService.createViolation(REPORTER_MEMBER_ID, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ViolationErrorCode.SELF_REPORT_NOT_ALLOWED);

            verify(violationRepository, never()).save(any(Violation.class));
        }
    }

    @Nested
    @DisplayName("신고 생성 테스트 - 채팅방")
    class CreateViolationChatRoomTests {

        @Test
        @DisplayName("채팅방 신고 생성 - 성공 (신고자가 sender)")
        void createViolation_ChatRoom_Success_ReporterIsSender() {
            // given
            ViolationCreateRequest request = new ViolationCreateRequest(
                    null,
                    1L,
                    ReportReason.ABUSE,
                    "욕설을 사용했습니다."
            );

            given(violationRepository.existsByReporterAndChatRoom(REPORTER_MEMBER_ID, 1L))
                    .willReturn(false);
            given(chatRoomRepository.findByIdWithMembers(1L))
                    .willReturn(Optional.of(chatRoom));
            given(violationRepository.save(any(Violation.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when & then
            assertThatCode(() -> violationService.createViolation(REPORTER_MEMBER_ID, request))
                    .doesNotThrowAnyException();

            verify(violationRepository).save(any(Violation.class));
        }

        @Test
        @DisplayName("채팅방 신고 생성 - 성공 (신고자가 receiver)")
        void createViolation_ChatRoom_Success_ReporterIsReceiver() {
            // given
            ViolationCreateRequest request = new ViolationCreateRequest(
                    null,
                    1L,
                    ReportReason.ABUSE,
                    "욕설을 사용했습니다."
            );

            ChatRoom reverseChatRoom = createChatRoom(reportedMember, reporter);
            ReflectionTestUtils.setField(reverseChatRoom, "id", 1L);

            given(violationRepository.existsByReporterAndChatRoom(REPORTER_MEMBER_ID, 1L))
                    .willReturn(false);
            given(chatRoomRepository.findByIdWithMembers(1L))
                    .willReturn(Optional.of(reverseChatRoom));
            given(violationRepository.save(any(Violation.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when & then
            assertThatCode(() -> violationService.createViolation(REPORTER_MEMBER_ID, request))
                    .doesNotThrowAnyException();

            verify(violationRepository).save(any(Violation.class));
        }

        @Test
        @DisplayName("채팅방 신고 생성 - 채팅방을 찾을 수 없으면 예외 발생")
        void createViolation_ChatRoom_ChatRoomNotFound_ThrowsException() {
            // given
            ViolationCreateRequest request = new ViolationCreateRequest(
                    null,
                    1L,
                    ReportReason.ABUSE,
                    "욕설을 사용했습니다."
            );

            given(violationRepository.existsByReporterAndChatRoom(REPORTER_MEMBER_ID, 1L))
                    .willReturn(false);
            given(chatRoomRepository.findByIdWithMembers(1L))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> violationService.createViolation(REPORTER_MEMBER_ID, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ViolationErrorCode.CHATROOM_NOT_FOUND);

            verify(violationRepository, never()).save(any(Violation.class));
        }

        @Test
        @DisplayName("채팅방 신고 생성 - 중복 신고면 예외 발생")
        void createViolation_ChatRoom_Duplicate_ThrowsException() {
            // given
            ViolationCreateRequest request = new ViolationCreateRequest(
                    null,
                    1L,
                    ReportReason.ABUSE,
                    "욕설을 사용했습니다."
            );

            given(violationRepository.existsByReporterAndChatRoom(REPORTER_MEMBER_ID, 1L))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> violationService.createViolation(REPORTER_MEMBER_ID, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ViolationErrorCode.DUPLICATE_VIOLATION);

            verify(chatRoomRepository, never()).findByIdWithMembers(anyLong());
            verify(violationRepository, never()).save(any(Violation.class));
        }

        @Test
        @DisplayName("채팅방 신고 생성 - 채팅방 참여자가 아니면 예외 발생")
        void createViolation_ChatRoom_UnauthorizedUser_ThrowsException() {
            // given
            ViolationCreateRequest request = new ViolationCreateRequest(
                    null,
                    1L,
                    ReportReason.ABUSE,
                    "욕설을 사용했습니다."
            );

            Long unauthorizedMemberId = 3L;

            given(violationRepository.existsByReporterAndChatRoom(unauthorizedMemberId, 1L))
                    .willReturn(false);
            given(chatRoomRepository.findByIdWithMembers(1L))
                    .willReturn(Optional.of(chatRoom));

            // when & then
            assertThatThrownBy(() -> violationService.createViolation(unauthorizedMemberId, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ViolationErrorCode.UNAUTHORIZED_CHAT_REPORT);

            verify(violationRepository, never()).save(any(Violation.class));
        }
    }

    @Nested
    @DisplayName("신고 대상 검증 테스트")
    class ValidateViolationTargetTests {

        @Test
        @DisplayName("신고 대상 검증 - boardId와 chatRoomId 둘 다 null이면 예외 발생")
        void validateViolationTarget_BothNull_ThrowsException() {
            // given
            ViolationCreateRequest request = new ViolationCreateRequest(
                    null,
                    null,
                    ReportReason.SPAM,
                    "테스트"
            );

            // when & then
            assertThatThrownBy(() -> violationService.createViolation(REPORTER_MEMBER_ID, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ViolationErrorCode.VIOLATION_TARGET_REQUIRED);

            verify(violationRepository, never()).save(any(Violation.class));
        }

        @Test
        @DisplayName("신고 대상 검증 - boardId와 chatRoomId 둘 다 있으면 예외 발생")
        void validateViolationTarget_BothExist_ThrowsException() {
            // given
            ViolationCreateRequest request = new ViolationCreateRequest(
                    1L,
                    1L,
                    ReportReason.SPAM,
                    "테스트"
            );

            // when & then
            assertThatThrownBy(() -> violationService.createViolation(REPORTER_MEMBER_ID, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ViolationErrorCode.VIOLATION_TARGET_CONFLICT);

            verify(violationRepository, never()).save(any(Violation.class));
        }
    }
}