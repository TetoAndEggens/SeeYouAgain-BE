package tetoandeggens.seeyouagainbe.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImageType {

	JPG("image/jpeg"),
	JPEG("image/jpeg"),
	PNG("image/png"),
	WEBP("image/webp"),
	HEIC("image/heic"),
	SVG("image/svg+xml");

	private final String type;
}