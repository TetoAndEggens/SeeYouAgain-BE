package tetoandeggens.seeyouagainbe.violation.repository.custom;

public interface ViolationRepositoryCustom {
    // 중복 신고 확인 - 게시글
    boolean existsByReporterAndBoard(Long reporterId, Long boardId);

    // 중복 신고 확인 - 채팅방
    boolean existsByReporterAndChatRoom(Long reporterId, Long chatRoomId);
}