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

    @Nested
    @DisplayName("신고 상세 조회 (Fetch Join) 테스트")
    class FindByIdWithAllFetchTests {

        @Test
        @DisplayName("신고 상세 조회 - 성공 (게시글 신고, 모든 연관 엔티티 로드)")
        void findByIdWithAllFetch_Success_BoardViolation() {
            // given
            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .board(board)
                    .violatedStatus(ViolatedStatus.WAITING)
                    .reason(ReportReason.SPAM)
                    .detailReason("스팸 게시글입니다.")
                    .build();

            Violation savedViolation = violationRepository.save(violation);

            // when
            var result = violationRepository.findByIdWithAllFetch(savedViolation.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(savedViolation.getId());
            assertThat(result.get().getReporter()).isEqualTo(reporter);
            assertThat(result.get().getReportedMember()).isEqualTo(reportedMember);
            assertThat(result.get().getBoard()).isEqualTo(board);
            assertThat(result.get().getChatRoom()).isNull();
            assertThat(result.get().getViolatedStatus()).isEqualTo(ViolatedStatus.WAITING);
            assertThat(result.get().getReason()).isEqualTo(ReportReason.SPAM);
        }

        @Test
        @DisplayName("신고 상세 조회 - 성공 (채팅방 신고, 모든 연관 엔티티 로드)")
        void findByIdWithAllFetch_Success_ChatRoomViolation() {
            // given
            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .chatRoom(chatRoom)
                    .violatedStatus(ViolatedStatus.WAITING)
                    .reason(ReportReason.ABUSE)
                    .detailReason("욕설을 사용했습니다.")
                    .build();

            Violation savedViolation = violationRepository.save(violation);

            // when
            var result = violationRepository.findByIdWithAllFetch(savedViolation.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(savedViolation.getId());
            assertThat(result.get().getReporter()).isEqualTo(reporter);
            assertThat(result.get().getReportedMember()).isEqualTo(reportedMember);
            assertThat(result.get().getBoard()).isNull();
            assertThat(result.get().getChatRoom()).isEqualTo(chatRoom);
            assertThat(result.get().getViolatedStatus()).isEqualTo(ViolatedStatus.WAITING);
            assertThat(result.get().getReason()).isEqualTo(ReportReason.ABUSE);
        }

        @Test
        @DisplayName("신고 상세 조회 - 신고 내역이 없으면 empty 반환")
        void findByIdWithAllFetch_NotFound_ReturnsEmpty() {
            // when
            var result = violationRepository.findByIdWithAllFetch(999L);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("신고 상세 조회 - Reporter와 ReportedMember 완전히 로드됨")
        void findByIdWithAllFetch_ReporterAndReportedMemberFullyLoaded() {
            // given
            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .board(board)
                    .violatedStatus(ViolatedStatus.WAITING)
                    .reason(ReportReason.SPAM)
                    .detailReason("스팸 게시글입니다.")
                    .build();

            Violation savedViolation = violationRepository.save(violation);

            // when
            var result = violationRepository.findByIdWithAllFetch(savedViolation.getId());

            // then
            assertThat(result).isPresent();
            Violation loadedViolation = result.get();

            // Fetch Join으로 모든 연관 엔티티 로드됨
            assertThat(loadedViolation.getReporter()).isNotNull();
            assertThat(loadedViolation.getReporter().getLoginId()).isEqualTo("reporter");
            assertThat(loadedViolation.getReporter().getNickName()).isEqualTo("신고자");
            assertThat(loadedViolation.getReporter().getPhoneNumber()).isEqualTo("01012345678");

            assertThat(loadedViolation.getReportedMember()).isNotNull();
            assertThat(loadedViolation.getReportedMember().getLoginId()).isEqualTo("reported");
            assertThat(loadedViolation.getReportedMember().getNickName()).isEqualTo("피신고자");
            assertThat(loadedViolation.getReportedMember().getViolatedCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("신고 상세 조회 - Board 객체 LEFT JOIN으로 선택적 로드")
        void findByIdWithAllFetch_BoardOptionalLoad() {
            // given: 게시글 신고
            Violation boardViolation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .board(board)
                    .violatedStatus(ViolatedStatus.WAITING)
                    .reason(ReportReason.SPAM)
                    .detailReason("스팸 게시글입니다.")
                    .build();

            Violation savedBoardViolation = violationRepository.save(boardViolation);

            // when
            var result = violationRepository.findByIdWithAllFetch(savedBoardViolation.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getBoard()).isNotNull();
            assertThat(result.get().getBoard().getTitle()).isEqualTo("테스트 게시글");
            assertThat(result.get().getBoard().getContent()).isEqualTo("테스트 내용");
        }

        @Test
        @DisplayName("신고 상세 조회 - ChatRoom 객체 LEFT JOIN으로 선택적 로드")
        void findByIdWithAllFetch_ChatRoomOptionalLoad() {
            // given: 채팅방 신고
            Violation chatViolation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .chatRoom(chatRoom)
                    .violatedStatus(ViolatedStatus.WAITING)
                    .reason(ReportReason.ABUSE)
                    .detailReason("욕설을 사용했습니다.")
                    .build();

            Violation savedChatViolation = violationRepository.save(chatViolation);

            // when
            var result = violationRepository.findByIdWithAllFetch(savedChatViolation.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getChatRoom()).isNotNull();
            assertThat(result.get().getChatRoom().getSender()).isEqualTo(reporter);
            assertThat(result.get().getChatRoom().getReceiver()).isEqualTo(reportedMember);
        }

        @Test
        @DisplayName("신고 상세 조회 - 게시글 신고에서 ChatRoom은 null")
        void findByIdWithAllFetch_BoardViolationChatRoomIsNull() {
            // given
            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .board(board)
                    .violatedStatus(ViolatedStatus.WAITING)
                    .reason(ReportReason.SPAM)
                    .detailReason("스팸 게시글입니다.")
                    .build();

            Violation savedViolation = violationRepository.save(violation);

            // when
            var result = violationRepository.findByIdWithAllFetch(savedViolation.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getChatRoom()).isNull();
            assertThat(result.get().getBoard()).isNotNull();
        }

        @Test
        @DisplayName("신고 상세 조회 - 채팅방 신고에서 Board는 null")
        void findByIdWithAllFetch_ChatRoomViolationBoardIsNull() {
            // given
            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .chatRoom(chatRoom)
                    .violatedStatus(ViolatedStatus.WAITING)
                    .reason(ReportReason.ABUSE)
                    .detailReason("욕설을 사용했습니다.")
                    .build();

            Violation savedViolation = violationRepository.save(violation);

            // when
            var result = violationRepository.findByIdWithAllFetch(savedViolation.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getBoard()).isNull();
            assertThat(result.get().getChatRoom()).isNotNull();
        }

        @Test
        @DisplayName("신고 상세 조회 - 모든 필드 정보 완전히 로드됨")
        void findByIdWithAllFetch_AllFieldsLoaded() {
            // given
            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .board(board)
                    .violatedStatus(ViolatedStatus.WAITING)
                    .reason(ReportReason.SPAM)
                    .detailReason("스팸 게시글입니다.")
                    .build();

            Violation savedViolation = violationRepository.save(violation);

            // when
            var result = violationRepository.findByIdWithAllFetch(savedViolation.getId());

            // then
            assertThat(result).isPresent();
            Violation loadedViolation = result.get();

            // 모든 필드 검증
            assertThat(loadedViolation.getId()).isNotNull();
            assertThat(loadedViolation.getReporter()).isNotNull();
            assertThat(loadedViolation.getReportedMember()).isNotNull();
            assertThat(loadedViolation.getBoard()).isNotNull();
            assertThat(loadedViolation.getViolatedStatus()).isEqualTo(ViolatedStatus.WAITING);
            assertThat(loadedViolation.getReason()).isEqualTo(ReportReason.SPAM);
            assertThat(loadedViolation.getDetailReason()).isEqualTo("스팸 게시글입니다.");
            assertThat(loadedViolation.getCreatedAt()).isNotNull();
        }
    }
}