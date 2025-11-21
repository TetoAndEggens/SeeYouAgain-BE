package tetoandeggens.seeyouagainbe.global;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.global.config.TestSecurityConfig;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
public abstract class ControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String asJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected RequestPostProcessor mockUser(Long memberId) {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUuid()).thenReturn(String.valueOf(memberId));
        when(userDetails.getMemberId()).thenReturn(memberId);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, List.of()
        );

        return authentication(auth);
    }
}
