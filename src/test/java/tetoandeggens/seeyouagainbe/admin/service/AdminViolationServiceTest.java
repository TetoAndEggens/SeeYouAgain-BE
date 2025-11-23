package tetoandeggens.seeyouagainbe.admin.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import tetoandeggens.seeyouagainbe.admin.dto.request.ViolationProcessRequest;
import tetoandeggens.seeyouagainbe.admin.dto.response.ViolationDetailResponse;
import tetoandeggens.seeyouagainbe.admin.dto.response.ViolationListResponse;
import tetoandeggens.seeyouagainbe.board.entity.Board;
import tetoandeggens.seeyouagainbe.board.repository.BoardRepository;
import tetoandeggens.seeyouagainbe.chat.entity.ChatRoom;
import tetoandeggens.seeyouagainbe.chat.repository.ChatRoomRepository;
import tetoandeggens.seeyouagainbe.common.enums.ReportReason;
import tetoandeggens.seeyouagainbe.common.enums.ViolatedStatus;
import tetoandeggens.seeyouagainbe.global.ServiceTest;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.AdminErrorCode;
import tetoandeggens.seeyouagainbe.member.entity.Member;
import tetoandeggens.seeyouagainbe.member.repository.MemberRepository;
import tetoandeggens.seeyouagainbe.violation.entity.Violation;
import tetoandeggens.seeyouagainbe.violation.repository.ViolationRepository;

import java.util.List;

@DisplayName("AdminViolationService 단위 테스트")
class AdminViolationServiceTest extends ServiceTest {

    @Autowired
    private AdminViolationService adminViolationService;

    @MockitoBean
    private ViolationRepository violationRepository;

    @MockitoBean
    private MemberRepository memberRepository;

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
                .loginId("reporter123")
                .password("password123!")
                .nickName("신고자")
                .phoneNumber("01012345678")
                .build();
        ReflectionTestUtils.setField(reporter, "id", 1L);

        reportedMember = Member.builder()
                .loginId("reported123")
                .password("password123!")
                .nickName("피신고자")
                .phoneNumber("01087654321")
                .build();
        ReflectionTestUtils.setField(reportedMember, "id", 2L);

        board = Board.builder()
                .member(reportedMember)
                .title("테스트 게시글")
                .content("테스트 내용")
                .build();
        ReflectionTestUtils.setField(board, "id", 1L);

        // ChatRoom은 protected 생성자이므로 리플렉션으로 생성
        try {
            var constructor = ChatRoom.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            chatRoom = constructor.newInstance();
            ReflectionTestUtils.setField(chatRoom, "sender", reporter);
            ReflectionTestUtils.setField(chatRoom, "receiver", reportedMember);
            ReflectionTestUtils.setField(chatRoom, "board", board);
            ReflectionTestUtils.setField(chatRoom, "id", 1L);
        } catch (Exception e) {
            throw new RuntimeException("ChatRoom 생성 실패", e);
        }
    }

    @Nested
    @DisplayName("신고 목록 조회 테스트")
    class GetViolationListTests {

        @Test
        @DisplayName("신고 목록 조회 - 성공 (필터 없음)")
        void getViolationList_Success_WithoutFilter() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            ViolationListResponse response1 = new ViolationListResponse(
                    1L,
                    ViolatedStatus.WAITING,
                    ReportReason.SPAM,
                    "신고자1",
                    "피신고자1",
                    "BOARD",
                    1L,
                    null
            );

            Page<ViolationListResponse> expectedPage = new PageImpl<>(
                    List.of(response1),
                    pageable,
                    1
            );

            given(violationRepository.findViolationList(null, pageable))
                    .willReturn(expectedPage);

            // when
            Page<ViolationListResponse> result = adminViolationService.getViolationList(null, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).violationId()).isEqualTo(1L);
            assertThat(result.getContent().get(0).violatedStatus()).isEqualTo(ViolatedStatus.WAITING);

            verify(violationRepository).findViolationList(null, pageable);
        }

        @Test
        @DisplayName("신고 목록 조회 - 성공 (WAITING 상태 필터)")
        void getViolationList_Success_WithWaitingFilter() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            ViolationListResponse response1 = new ViolationListResponse(
                    1L,
                    ViolatedStatus.WAITING,
                    ReportReason.SPAM,
                    "신고자1",
                    "피신고자1",
                    "BOARD",
                    1L,
                    null
            );

            Page<ViolationListResponse> expectedPage = new PageImpl<>(
                    List.of(response1),
                    pageable,
                    1
            );

            given(violationRepository.findViolationList(ViolatedStatus.WAITING, pageable))
                    .willReturn(expectedPage);

            // when
            Page<ViolationListResponse> result = adminViolationService.getViolationList(
                    ViolatedStatus.WAITING,
                    pageable
            );

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).violatedStatus()).isEqualTo(ViolatedStatus.WAITING);

            verify(violationRepository).findViolationList(ViolatedStatus.WAITING, pageable);
        }
    }

    @Nested
    @DisplayName("신고 상세 조회 테스트")
    class GetViolationDetailTests {

        @Test
        @DisplayName("신고 상세 조회 - 성공 (게시글 신고)")
        void getViolationDetail_Success_BoardViolation() {
            // given
            Long violationId = 1L;

            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .board(board)
                    .reason(ReportReason.SPAM)
                    .detailReason("스팸 게시글입니다.")
                    .build();
            ReflectionTestUtils.setField(violation, "id", violationId);

            given(violationRepository.findById(violationId))
                    .willReturn(Optional.of(violation));

            // when
            ViolationDetailResponse result = adminViolationService.getViolationDetail(violationId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.violationId()).isEqualTo(violationId);
            assertThat(result.reason()).isEqualTo(ReportReason.SPAM);
            assertThat(result.detailReason()).isEqualTo("스팸 게시글입니다.");

            verify(violationRepository).findById(violationId);
        }

        @Test
        @DisplayName("신고 상세 조회 - 성공 (채팅방 신고)")
        void getViolationDetail_Success_ChatRoomViolation() {
            // given
            Long violationId = 1L;

            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .chatRoom(chatRoom)
                    .reason(ReportReason.ABUSE)
                    .detailReason("욕설을 사용했습니다.")
                    .build();
            ReflectionTestUtils.setField(violation, "id", violationId);

            given(violationRepository.findById(violationId))
                    .willReturn(Optional.of(violation));

            // when
            ViolationDetailResponse result = adminViolationService.getViolationDetail(violationId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.violationId()).isEqualTo(violationId);
            assertThat(result.reason()).isEqualTo(ReportReason.ABUSE);

            verify(violationRepository).findById(violationId);
        }

        @Test
        @DisplayName("신고 상세 조회 - 실패 (신고 내역을 찾을 수 없음)")
        void getViolationDetail_Fail_ViolationNotFound() {
            // given
            Long violationId = 999L;

            given(violationRepository.findById(violationId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminViolationService.getViolationDetail(violationId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AdminErrorCode.VIOLATION_NOT_FOUND);

            verify(violationRepository).findById(violationId);
        }
    }

    @Nested
    @DisplayName("신고 처리 테스트 - 게시글")
    class ProcessViolationBoardTests {

        @Test
        @DisplayName("신고 처리 - 성공 (위반 처리 + 게시글 삭제)")
        void processViolation_Success_ViolatedWithDelete_Board() {
            // given
            Long violationId = 1L;

            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .board(board)
                    .reason(ReportReason.SPAM)
                    .detailReason("스팸 게시글입니다.")
                    .build();
            ReflectionTestUtils.setField(violation, "id", violationId);

            ViolationProcessRequest request = new ViolationProcessRequest(
                    ViolatedStatus.VIOLATED,
                    true
            );

            given(violationRepository.findById(violationId))
                    .willReturn(Optional.of(violation));
            given(memberRepository.save(any(Member.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(boardRepository.save(any(Board.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            adminViolationService.processViolation(violationId, request);

            // then
            assertThat(violation.getViolatedStatus()).isEqualTo(ViolatedStatus.VIOLATED);
            assertThat(reportedMember.getViolatedCount()).isEqualTo(1);
            assertThat(board.getIsDeleted()).isTrue();
            assertThat(board.getViolatedStatus()).isEqualTo(ViolatedStatus.VIOLATED);

            verify(violationRepository).findById(violationId);
            verify(memberRepository).save(reportedMember);
            verify(boardRepository).save(board);
        }

        @Test
        @DisplayName("신고 처리 - 성공 (위반 처리 + 게시글 삭제 안함)")
        void processViolation_Success_ViolatedWithoutDelete_Board() {
            // given
            Long violationId = 1L;

            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .board(board)
                    .reason(ReportReason.SPAM)
                    .detailReason("스팸 게시글입니다.")
                    .build();
            ReflectionTestUtils.setField(violation, "id", violationId);

            ViolationProcessRequest request = new ViolationProcessRequest(
                    ViolatedStatus.VIOLATED,
                    false
            );

            given(violationRepository.findById(violationId))
                    .willReturn(Optional.of(violation));
            given(memberRepository.save(any(Member.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(boardRepository.save(any(Board.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            adminViolationService.processViolation(violationId, request);

            // then
            assertThat(violation.getViolatedStatus()).isEqualTo(ViolatedStatus.VIOLATED);
            assertThat(reportedMember.getViolatedCount()).isEqualTo(1);
            assertThat(board.getIsDeleted()).isFalse();
            assertThat(board.getViolatedStatus()).isEqualTo(ViolatedStatus.VIOLATED);

            verify(violationRepository).findById(violationId);
            verify(memberRepository).save(reportedMember);
            verify(boardRepository).save(board);
        }

        @Test
        @DisplayName("신고 처리 - 성공 (위반 아님 처리)")
        void processViolation_Success_Normal_Board() {
            // given
            Long violationId = 1L;

            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .board(board)
                    .reason(ReportReason.SPAM)
                    .detailReason("스팸 게시글입니다.")
                    .build();
            ReflectionTestUtils.setField(violation, "id", violationId);

            ViolationProcessRequest request = new ViolationProcessRequest(
                    ViolatedStatus.NORMAL,
                    false
            );

            given(violationRepository.findById(violationId))
                    .willReturn(Optional.of(violation));
            given(boardRepository.save(any(Board.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            adminViolationService.processViolation(violationId, request);

            // then
            assertThat(violation.getViolatedStatus()).isEqualTo(ViolatedStatus.NORMAL);
            assertThat(reportedMember.getViolatedCount()).isEqualTo(0);
            assertThat(board.getIsDeleted()).isFalse();
            assertThat(board.getViolatedStatus()).isEqualTo(ViolatedStatus.NORMAL);

            verify(violationRepository).findById(violationId);
            verify(boardRepository).save(board);
            verify(memberRepository, never()).save(any(Member.class));
        }

        @Test
        @DisplayName("신고 처리 - 성공 (deleteContent null이면 기본값 적용)")
        void processViolation_Success_DeleteContentNull_Board() {
            // given
            Long violationId = 1L;

            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .board(board)
                    .reason(ReportReason.SPAM)
                    .detailReason("스팸 게시글입니다.")
                    .build();
            ReflectionTestUtils.setField(violation, "id", violationId);

            ViolationProcessRequest request = new ViolationProcessRequest(
                    ViolatedStatus.VIOLATED,
                    null  // null인 경우 기본값으로 true가 적용되어야 함
            );

            given(violationRepository.findById(violationId))
                    .willReturn(Optional.of(violation));
            given(memberRepository.save(any(Member.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(boardRepository.save(any(Board.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            adminViolationService.processViolation(violationId, request);

            // then
            assertThat(violation.getViolatedStatus()).isEqualTo(ViolatedStatus.VIOLATED);
            assertThat(board.getIsDeleted()).isTrue();
            assertThat(board.getViolatedStatus()).isEqualTo(ViolatedStatus.VIOLATED);

            verify(violationRepository).findById(violationId);
            verify(memberRepository).save(reportedMember);
            verify(boardRepository).save(board);
        }
    }

    @Nested
    @DisplayName("신고 처리 테스트 - 채팅방")
    class ProcessViolationChatRoomTests {

        @Test
        @DisplayName("신고 처리 - 성공 (위반 처리 + 채팅방 삭제)")
        void processViolation_Success_ViolatedWithDelete_ChatRoom() {
            // given
            Long violationId = 1L;

            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .chatRoom(chatRoom)
                    .reason(ReportReason.ABUSE)
                    .detailReason("욕설을 사용했습니다.")
                    .build();
            ReflectionTestUtils.setField(violation, "id", violationId);

            ViolationProcessRequest request = new ViolationProcessRequest(
                    ViolatedStatus.VIOLATED,
                    true
            );

            given(violationRepository.findById(violationId))
                    .willReturn(Optional.of(violation));
            given(memberRepository.save(any(Member.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(chatRoomRepository.save(any(ChatRoom.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            adminViolationService.processViolation(violationId, request);

            // then
            assertThat(violation.getViolatedStatus()).isEqualTo(ViolatedStatus.VIOLATED);
            assertThat(reportedMember.getViolatedCount()).isEqualTo(1);
            assertThat(chatRoom.getIsDeleted()).isTrue();
            assertThat(chatRoom.getViolatedStatus()).isEqualTo(ViolatedStatus.VIOLATED);

            verify(violationRepository).findById(violationId);
            verify(memberRepository).save(reportedMember);
            verify(chatRoomRepository).save(chatRoom);
        }

        @Test
        @DisplayName("신고 처리 - 성공 (위반 처리 + 채팅방 삭제 안함)")
        void processViolation_Success_ViolatedWithoutDelete_ChatRoom() {
            // given
            Long violationId = 1L;

            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .chatRoom(chatRoom)
                    .reason(ReportReason.ABUSE)
                    .detailReason("욕설을 사용했습니다.")
                    .build();
            ReflectionTestUtils.setField(violation, "id", violationId);

            ViolationProcessRequest request = new ViolationProcessRequest(
                    ViolatedStatus.VIOLATED,
                    false
            );

            given(violationRepository.findById(violationId))
                    .willReturn(Optional.of(violation));
            given(memberRepository.save(any(Member.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(chatRoomRepository.save(any(ChatRoom.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            adminViolationService.processViolation(violationId, request);

            // then
            assertThat(violation.getViolatedStatus()).isEqualTo(ViolatedStatus.VIOLATED);
            assertThat(reportedMember.getViolatedCount()).isEqualTo(1);
            assertThat(chatRoom.getIsDeleted()).isFalse();
            assertThat(chatRoom.getViolatedStatus()).isEqualTo(ViolatedStatus.VIOLATED);

            verify(violationRepository).findById(violationId);
            verify(memberRepository).save(reportedMember);
            verify(chatRoomRepository).save(chatRoom);
        }

        @Test
        @DisplayName("신고 처리 - 성공 (위반 아님 처리)")
        void processViolation_Success_Normal_ChatRoom() {
            // given
            Long violationId = 1L;

            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .chatRoom(chatRoom)
                    .reason(ReportReason.ABUSE)
                    .detailReason("욕설을 사용했습니다.")
                    .build();
            ReflectionTestUtils.setField(violation, "id", violationId);

            ViolationProcessRequest request = new ViolationProcessRequest(
                    ViolatedStatus.NORMAL,
                    false
            );

            given(violationRepository.findById(violationId))
                    .willReturn(Optional.of(violation));
            given(chatRoomRepository.save(any(ChatRoom.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            adminViolationService.processViolation(violationId, request);

            // then
            assertThat(violation.getViolatedStatus()).isEqualTo(ViolatedStatus.NORMAL);
            assertThat(reportedMember.getViolatedCount()).isEqualTo(0);
            assertThat(chatRoom.getIsDeleted()).isFalse();
            assertThat(chatRoom.getViolatedStatus()).isEqualTo(ViolatedStatus.NORMAL);

            verify(violationRepository).findById(violationId);
            verify(chatRoomRepository).save(chatRoom);
            verify(memberRepository, never()).save(any(Member.class));
        }
    }

    @Nested
    @DisplayName("신고 처리 예외 테스트")
    class ProcessViolationExceptionTests {

        @Test
        @DisplayName("신고 처리 - 실패 (신고 내역을 찾을 수 없음)")
        void processViolation_Fail_ViolationNotFound() {
            // given
            Long violationId = 999L;
            ViolationProcessRequest request = new ViolationProcessRequest(
                    ViolatedStatus.VIOLATED,
                    true
            );

            given(violationRepository.findById(violationId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminViolationService.processViolation(violationId, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AdminErrorCode.VIOLATION_NOT_FOUND);

            verify(violationRepository).findById(violationId);
            verify(memberRepository, never()).save(any(Member.class));
            verify(boardRepository, never()).save(any(Board.class));
            verify(chatRoomRepository, never()).save(any(ChatRoom.class));
        }
    }

    @Nested
    @DisplayName("위반 횟수 증가 및 자동 정지 테스트")
    class ViolatedCountAndBanTests {

        @Test
        @DisplayName("위반 횟수 증가 - 1회 위반 시 정지되지 않음")
        void increaseViolatedCount_NotBanned_AfterFirstViolation() {
            // given
            Long violationId = 1L;

            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .board(board)
                    .reason(ReportReason.SPAM)
                    .detailReason("스팸 게시글입니다.")
                    .build();
            ReflectionTestUtils.setField(violation, "id", violationId);

            ViolationProcessRequest request = new ViolationProcessRequest(
                    ViolatedStatus.VIOLATED,
                    true
            );

            given(violationRepository.findById(violationId))
                    .willReturn(Optional.of(violation));
            given(memberRepository.save(any(Member.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(boardRepository.save(any(Board.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            adminViolationService.processViolation(violationId, request);

            // then
            assertThat(reportedMember.getViolatedCount()).isEqualTo(1);
            assertThat(reportedMember.getIsBanned()).isFalse();

            verify(memberRepository).save(reportedMember);
        }

        @Test
        @DisplayName("위반 횟수 증가 - 3회 위반 시 자동 정지")
        void increaseViolatedCount_Banned_After3Violations() {
            // given
            Long violationId = 1L;

            // 이미 2회 위반한 상태
            ReflectionTestUtils.setField(reportedMember, "violatedCount", 2L);

            Violation violation = Violation.builder()
                    .reporter(reporter)
                    .reportedMember(reportedMember)
                    .board(board)
                    .reason(ReportReason.SPAM)
                    .detailReason("스팸 게시글입니다.")
                    .build();
            ReflectionTestUtils.setField(violation, "id", violationId);

            ViolationProcessRequest request = new ViolationProcessRequest(
                    ViolatedStatus.VIOLATED,
                    true
            );

            given(violationRepository.findById(violationId))
                    .willReturn(Optional.of(violation));
            given(memberRepository.save(any(Member.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(boardRepository.save(any(Board.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            adminViolationService.processViolation(violationId, request);

            // then
            assertThat(reportedMember.getViolatedCount()).isEqualTo(3L);
            assertThat(reportedMember.getIsBanned()).isTrue();

            verify(memberRepository).save(reportedMember);
        }
    }
}