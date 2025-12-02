package tetoandeggens.seeyouagainbe.fcm.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import tetoandeggens.seeyouagainbe.fcm.dto.request.FcmTokenRequest;
import tetoandeggens.seeyouagainbe.fcm.dto.response.FcmTokenResponse;
import tetoandeggens.seeyouagainbe.fcm.entity.DeviceType;
import tetoandeggens.seeyouagainbe.fcm.service.FcmTokenService;
import tetoandeggens.seeyouagainbe.global.ControllerTest;
import tetoandeggens.seeyouagainbe.global.exception.CustomException;
import tetoandeggens.seeyouagainbe.global.exception.errorcode.FcmErrorCode;

@WebMvcTest(FcmTokenController.class)
@DisplayName("FcmTokenController 단위 테스트")
class FcmTokenControllerTest extends ControllerTest {

    private static final String BASE_URL = "/fcm/tokens";
    private static final Long TEST_MEMBER_ID = 1L;
    private static final String TEST_TOKEN = "test-fcm-token-12345";
    private static final String TEST_DEVICE_ID = "test-device-id-12345";
    private static final String TEST_USER_AGENT_ANDROID = "Mozilla/5.0 (Linux; Android 10)";

    @MockitoBean
    private FcmTokenService fcmTokenService;

    @Nested
    @DisplayName("FCM 토큰 등록 API 테스트")
    class RegisterTokenTests {

        @Test
        @DisplayName("FCM 토큰 등록 - 성공")
        void registerToken_Success() throws Exception {
            // given
            FcmTokenRequest request = new FcmTokenRequest(TEST_TOKEN, TEST_DEVICE_ID);
            FcmTokenResponse response = new FcmTokenResponse(
                    1L, TEST_DEVICE_ID, DeviceType.ANDROID,
                    LocalDateTime.now(), LocalDateTime.now()
            );

            given(fcmTokenService.saveOrUpdateToken(anyLong(), any(FcmTokenRequest.class), anyString()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post(BASE_URL)
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.USER_AGENT, TEST_USER_AGENT_ANDROID)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.data.deviceId").value(TEST_DEVICE_ID))
                    .andExpect(jsonPath("$.data.deviceType").value("ANDROID"));

            verify(fcmTokenService).saveOrUpdateToken(anyLong(), any(FcmTokenRequest.class), anyString());
        }

        @Test
        @DisplayName("FCM 토큰 등록 - token이 null이면 실패")
        void registerToken_ValidationFail_NullToken() throws Exception {
            // given
            String requestBody = "{\"token\":null,\"deviceId\":\"" + TEST_DEVICE_ID + "\"}";

            // when & then
            mockMvc.perform(post(BASE_URL)
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.USER_AGENT, TEST_USER_AGENT_ANDROID)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());

            verify(fcmTokenService, never()).saveOrUpdateToken(anyLong(), any(), anyString());
        }

        @Test
        @DisplayName("FCM 토큰 등록 - deviceId가 null이면 실패")
        void registerToken_ValidationFail_NullDeviceId() throws Exception {
            // given
            String requestBody = "{\"token\":\"" + TEST_TOKEN + "\",\"deviceId\":null}";

            // when & then
            mockMvc.perform(post(BASE_URL)
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.USER_AGENT, TEST_USER_AGENT_ANDROID)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());

            verify(fcmTokenService, never()).saveOrUpdateToken(anyLong(), any(), anyString());
        }

        @Test
        @DisplayName("FCM 토큰 등록 - token이 빈 문자열이면 실패")
        void registerToken_ValidationFail_BlankToken() throws Exception {
            // given
            FcmTokenRequest request = new FcmTokenRequest("", TEST_DEVICE_ID);

            // when & then
            mockMvc.perform(post(BASE_URL)
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.USER_AGENT, TEST_USER_AGENT_ANDROID)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest());

            verify(fcmTokenService, never()).saveOrUpdateToken(anyLong(), any(), anyString());
        }
    }

    @Nested
    @DisplayName("FCM 토큰 갱신 API 테스트")
    class RefreshTokenTests {

        @Test
        @DisplayName("FCM 토큰 갱신 - 성공")
        void refreshToken_Success() throws Exception {
            // given
            doNothing().when(fcmTokenService).refreshTokenIfNeeded(anyLong(), anyString());

            // when & then
            mockMvc.perform(put(BASE_URL + "/" + TEST_DEVICE_ID + "/refresh")
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(204));

            verify(fcmTokenService).refreshTokenIfNeeded(anyLong(), eq(TEST_DEVICE_ID));
        }

        @Test
        @DisplayName("FCM 토큰 갱신 - 토큰이 존재하지 않으면 실패")
        void refreshToken_Fail_TokenNotFound() throws Exception {
            // given
            doThrow(new CustomException(FcmErrorCode.TOKEN_NOT_FOUND))
                    .when(fcmTokenService).refreshTokenIfNeeded(anyLong(), anyString());

            // when & then
            mockMvc.perform(put(BASE_URL + "/" + TEST_DEVICE_ID + "/refresh")
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(fcmTokenService).refreshTokenIfNeeded(anyLong(), eq(TEST_DEVICE_ID));
        }
    }

    @Nested
    @DisplayName("FCM 토큰 삭제 API 테스트")
    class DeleteTokenTests {

        @Test
        @DisplayName("FCM 토큰 삭제 - 성공")
        void deleteToken_Success() throws Exception {
            // given
            doNothing().when(fcmTokenService).deleteToken(anyLong(), anyString());

            // when & then
            mockMvc.perform(delete(BASE_URL + "/" + TEST_DEVICE_ID)
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(204));

            verify(fcmTokenService).deleteToken(anyLong(), eq(TEST_DEVICE_ID));
        }

        @Test
        @DisplayName("FCM 토큰 삭제 - 토큰이 존재하지 않으면 실패")
        void deleteToken_Fail_TokenNotFound() throws Exception {
            // given
            doThrow(new CustomException(FcmErrorCode.TOKEN_NOT_FOUND))
                    .when(fcmTokenService).deleteToken(anyLong(), anyString());

            // when & then
            mockMvc.perform(delete(BASE_URL + "/" + TEST_DEVICE_ID)
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(fcmTokenService).deleteToken(anyLong(), eq(TEST_DEVICE_ID));
        }
    }

    @Nested
    @DisplayName("FCM 토큰 목록 조회 API 테스트")
    class GetTokensTests {

        @Test
        @DisplayName("FCM 토큰 목록 조회 - 성공")
        void getTokens_Success() throws Exception {
            // given
            List<FcmTokenResponse> responses = List.of(
                    new FcmTokenResponse(1L, TEST_DEVICE_ID,
                            DeviceType.ANDROID, LocalDateTime.now(), LocalDateTime.now()),
                    new FcmTokenResponse(2L, "another-device-id",
                            DeviceType.IOS, LocalDateTime.now(), LocalDateTime.now())
            );

            given(fcmTokenService.getTokensByMemberId(anyLong())).willReturn(responses);

            // when & then
            mockMvc.perform(get(BASE_URL)
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].deviceId").value(TEST_DEVICE_ID))
                    .andExpect(jsonPath("$.data[0].deviceType").value("ANDROID"))
                    .andExpect(jsonPath("$.data[1].deviceType").value("IOS"));

            verify(fcmTokenService).getTokensByMemberId(anyLong());
        }

        @Test
        @DisplayName("FCM 토큰 목록 조회 - 빈 리스트 반환")
        void getTokens_ReturnsEmptyList() throws Exception {
            // given
            given(fcmTokenService.getTokensByMemberId(anyLong())).willReturn(List.of());

            // when & then
            mockMvc.perform(get(BASE_URL)
                            .with(mockUser(TEST_MEMBER_ID))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(fcmTokenService).getTokensByMemberId(anyLong());
        }
    }
}