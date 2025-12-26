package tetoandeggens.seeyouagainbe.chat.dto;

import java.security.Principal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class WebSocketPrincipal implements Principal {

	private final Long memberId;
	private final String uuid;

	@Override
	public String getName() {
		return memberId.toString();
	}
}