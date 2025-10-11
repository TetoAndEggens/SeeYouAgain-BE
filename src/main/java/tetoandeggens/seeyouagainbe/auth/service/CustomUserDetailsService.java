package tetoandeggens.seeyouagainbe.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tetoandeggens.seeyouagainbe.auth.dto.CustomUserDetails;
import tetoandeggens.seeyouagainbe.domain.member.entity.Member;
import tetoandeggens.seeyouagainbe.domain.member.repository.MemberRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        Member member = memberRepository.findByLoginIdAndIsDeletedFalse(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("회원을 찾을 수 없습니다: " + loginId));

        return new CustomUserDetails(member);
    }
}
