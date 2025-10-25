package tetoandeggens.seeyouagainbe.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportReason {
	SPAM("스팸/광고"),
	SEXUAL_CONTENT("음란물/선정성"),
	ABUSE("욕설/혐오"),
	PRIVACY_VIOLATION("개인정보 노출"),
	ILLEGAL_CONTENT("불법 정보"),
	FRAUD("사기/사칭"),
	VIOLENCE("폭력/잔인함"),
	ETC("기타");

	private final String reason;
}