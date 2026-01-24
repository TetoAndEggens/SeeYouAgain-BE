package tetoandeggens.seeyouagainbe.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MyInfoResponse", description = "마이페이지 정보 응답 Dto")
public record MyInfoResponse(
        @Schema(description = "닉네임", example = "귀여운강아지")
        String nickName,

        @Schema(description = "프로필 이미지 URL", example = "https://s3.amazonaws.com/profile/image.jpg")
        String profile
) {
    public static MyInfoResponse from(String nickName, String profile) {
        return new MyInfoResponse(nickName, profile);
    }
}