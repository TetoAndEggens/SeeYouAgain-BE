package tetoandeggens.seeyouagainbe.chat.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageType {

	TEXT("텍스트"),
	IMAGE("이미지");

	private final String type;
}