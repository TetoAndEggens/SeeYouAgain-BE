package tetoandeggens.seeyouagainbe.violation.repository;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.board.repository.BoardRepository;
import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;
import tetoandeggens.seeyouagainbe.chat.repository.ChatRoomRepository;
import tetoandeggens.seeyouagainbe.common.enums.ReportReason;
import tetoandeggens.seeyouagainbe.common.enums.ViolatedStatus;
import tetoandeggens.seeyouagainbe.global.RepositoryTest;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;
import tetoandeggens.seeyouagainbe.violation.entity.Violation;

@DisplayName("ViolationRepository 단위 테스트")
class ViolationRepositoryTest extends RepositoryTest {

    @Autowired
    private ViolationRepository violationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    private Member reporter;
    private Member reportedMember;
    private Board board;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        violationRepository.deleteAll();
        chatRoomRepository.deleteAll();
        boardRepository.deleteAll();
        memberRepository.deleteAll();

        reporter = Member.builder()
                .loginId("reporter")
                .password("password123!")
                .nickName("신고자")
                .phoneNumber("01012345678")
                .build();

        reportedMember = Member.builder()
                .loginId("reported")
                .password("password123!")
                .nickName("피신고자")
                .phoneNumber("01087654321")
                .build();

        memberRepository.save(reporter);
        memberRepository.save(reportedMember);

        board = createBoard(reportedMember, "테스트 게시글", "테스트 내용");
        boardRepository.save(board);

        chatRoom = createChatRoom(reporter, reportedMember);
        chatRoomRepository.save(chatRoom);
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
    @DisplayName("게시글 중복 신고 확인 테스트")
    class ExistsByReporterAndBoardTests {

        @Test
        @DisplayName("게시글 중복 신고 확인 - 신고 내역이 없으면 false 반환")
        void existsByReporterAndBoard_NotExists_ReturnsFalse() {
            // when
            boolean result = violationRepository.existsByReporterAndBoard(reporter.getId(), board.getId());

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("게시글 중복 신고 확인 - 신고 내역이 있으면 true 반환")
        void existsByReporterAndBoard_Exists_ReturnsTrue() {
            // given
            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .board(board)
                    .violatedStatus(ViolatedStatus.WAITING)
                    .reason(ReportReason.SPAM)
                    .detailReason("스팸 게시글입니다.")
                    .build();

            violationRepository.save(violation);

            // when
            boolean result = violationRepository.existsByReporterAndBoard(reporter.getId(), board.getId());

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("게시글 중복 신고 확인 - 다른 신고자의 신고 내역은 무시")
        void existsByReporterAndBoard_DifferentReporter_ReturnsFalse() {
            // given
            Member anotherReporter = Member.builder()
                    .loginId("another")
                    .password("password123!")
                    .nickName("다른신고자")
                    .phoneNumber("01011112222")
                    .build();

            memberRepository.save(anotherReporter);

            Violation violation = Violation.builder()
                    .reporter(anotherReporter)
                    .reportedMember(reportedMember)
                    .board(board)
                    .violatedStatus(ViolatedStatus.WAITING)
                    .reason(ReportReason.SPAM)
                    .detailReason("스팸 게시글입니다.")
                    .build();

            violationRepository.save(violation);

            // when
            boolean result = violationRepository.existsByReporterAndBoard(reporter.getId(), board.getId());

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("게시글 중복 신고 확인 - 다른 게시글의 신고 내역은 무시")
        void existsByReporterAndBoard_DifferentBoard_ReturnsFalse() {
            // given
            Board anotherBoard = createBoard(reportedMember, "다른 게시글", "다른 내용");
            boardRepository.save(anotherBoard);

            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .board(anotherBoard)
                    .violatedStatus(ViolatedStatus.WAITING)
                    .reason(ReportReason.SPAM)
                    .detailReason("스팸 게시글입니다.")
                    .build();

            violationRepository.save(violation);

            // when
            boolean result = violationRepository.existsByReporterAndBoard(reporter.getId(), board.getId());

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("게시글 중복 신고 확인 - 채팅방 신고 내역은 무시")
        void existsByReporterAndBoard_ChatRoomViolation_ReturnsFalse() {
            // given
            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .chatRoom(chatRoom)
                    .violatedStatus(ViolatedStatus.WAITING)
                    .reason(ReportReason.ABUSE)
                    .detailReason("욕설을 사용했습니다.")
                    .build();

            violationRepository.save(violation);

            // when
            boolean result = violationRepository.existsByReporterAndBoard(reporter.getId(), board.getId());

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("채팅방 중복 신고 확인 테스트")
    class ExistsByReporterAndChatRoomTests {

        @Test
        @DisplayName("채팅방 중복 신고 확인 - 신고 내역이 없으면 false 반환")
        void existsByReporterAndChatRoom_NotExists_ReturnsFalse() {
            // when
            boolean result = violationRepository.existsByReporterAndChatRoom(reporter.getId(), chatRoom.getId());

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("채팅방 중복 신고 확인 - 신고 내역이 있으면 true 반환")
        void existsByReporterAndChatRoom_Exists_ReturnsTrue() {
            // given
            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .chatRoom(chatRoom)
                    .violatedStatus(ViolatedStatus.WAITING)
                    .reason(ReportReason.ABUSE)
                    .detailReason("욕설을 사용했습니다.")
                    .build();

            violationRepository.save(violation);

            // when
            boolean result = violationRepository.existsByReporterAndChatRoom(reporter.getId(), chatRoom.getId());

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("채팅방 중복 신고 확인 - 다른 신고자의 신고 내역은 무시")
        void existsByReporterAndChatRoom_DifferentReporter_ReturnsFalse() {
            // given
            Member anotherReporter = Member.builder()
                    .loginId("another")
                    .password("password123!")
                    .nickName("다른신고자")
                    .phoneNumber("01011112222")
                    .build();

            memberRepository.save(anotherReporter);

            Violation violation = Violation.builder()
                    .reporter(anotherReporter)
                    .reportedMember(reportedMember)
                    .chatRoom(chatRoom)
                    .violatedStatus(ViolatedStatus.WAITING)
                    .reason(ReportReason.ABUSE)
                    .detailReason("욕설을 사용했습니다.")
                    .build();

            violationRepository.save(violation);

            // when
            boolean result = violationRepository.existsByReporterAndChatRoom(reporter.getId(), chatRoom.getId());

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("채팅방 중복 신고 확인 - 다른 채팅방의 신고 내역은 무시")
        void existsByReporterAndChatRoom_DifferentChatRoom_ReturnsFalse() {
            // given
            Member anotherMember = Member.builder()
                    .loginId("another")
                    .password("password123!")
                    .nickName("다른사용자")
                    .phoneNumber("01011112222")
                    .build();

            memberRepository.save(anotherMember);

            ChatRoom anotherChatRoom = createChatRoom(reporter, anotherMember);
            chatRoomRepository.save(anotherChatRoom);

            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(anotherMember)
                    .chatRoom(anotherChatRoom)
                    .violatedStatus(ViolatedStatus.WAITING)
                    .reason(ReportReason.ABUSE)
                    .detailReason("욕설을 사용했습니다.")
                    .build();

            violationRepository.save(violation);

            // when
            boolean result = violationRepository.existsByReporterAndChatRoom(reporter.getId(), chatRoom.getId());

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("채팅방 중복 신고 확인 - 게시글 신고 내역은 무시")
        void existsByReporterAndChatRoom_BoardViolation_ReturnsFalse() {
            // given
            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .board(board)
                    .violatedStatus(ViolatedStatus.WAITING)
                    .reason(ReportReason.SPAM)
                    .detailReason("스팸 게시글입니다.")
                    .build();

            violationRepository.save(violation);

            // when
            boolean result = violationRepository.existsByReporterAndChatRoom(reporter.getId(), chatRoom.getId());

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("신고 저장 테스트")
    class SaveViolationTests {

        @Test
        @DisplayName("게시글 신고 저장 - 성공")
        void saveViolation_Board_Success() {
            // given
            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .board(board)
                    .violatedStatus(ViolatedStatus.WAITING)
                    .reason(ReportReason.SPAM)
                    .detailReason("스팸 게시글입니다.")
                    .build();

            // when
            Violation savedViolation = violationRepository.save(violation);

            // then
            assertThat(savedViolation.getId()).isNotNull();
            assertThat(savedViolation.getReporter()).isEqualTo(reporter);
            assertThat(savedViolation.getReportedMember()).isEqualTo(reportedMember);
            assertThat(savedViolation.getBoard()).isEqualTo(board);
            assertThat(savedViolation.getChatRoom()).isNull();
            assertThat(savedViolation.getViolatedStatus()).isEqualTo(ViolatedStatus.WAITING);
            assertThat(savedViolation.getReason()).isEqualTo(ReportReason.SPAM);
            assertThat(savedViolation.getDetailReason()).isEqualTo("스팸 게시글입니다.");
        }

        @Test
        @DisplayName("채팅방 신고 저장 - 성공")
        void saveViolation_ChatRoom_Success() {
            // given
            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .chatRoom(chatRoom)
                    .violatedStatus(ViolatedStatus.WAITING)
                    .reason(ReportReason.ABUSE)
                    .detailReason("욕설을 사용했습니다.")
                    .build();

            // when
            Violation savedViolation = violationRepository.save(violation);

            // then
            assertThat(savedViolation.getId()).isNotNull();
            assertThat(savedViolation.getReporter()).isEqualTo(reporter);
            assertThat(savedViolation.getReportedMember()).isEqualTo(reportedMember);
            assertThat(savedViolation.getBoard()).isNull();
            assertThat(savedViolation.getChatRoom()).isEqualTo(chatRoom);
            assertThat(savedViolation.getViolatedStatus()).isEqualTo(ViolatedStatus.WAITING);
            assertThat(savedViolation.getReason()).isEqualTo(ReportReason.ABUSE);
            assertThat(savedViolation.getDetailReason()).isEqualTo("욕설을 사용했습니다.");
        }

        @Test
        @DisplayName("신고 저장 - detailReason이 null이어도 성공")
        void saveViolation_NullDetailReason_Success() {
            // given
            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .board(board)
                    .violatedStatus(ViolatedStatus.WAITING)
                    .reason(ReportReason.ETC)
                    .detailReason(null)
                    .build();

            // when
            Violation savedViolation = violationRepository.save(violation);

            // then
            assertThat(savedViolation.getId()).isNotNull();
            assertThat(savedViolation.getDetailReason()).isNull();
        }
    }
}