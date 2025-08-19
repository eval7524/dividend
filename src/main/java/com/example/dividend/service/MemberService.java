package com.example.dividend.service;

import com.example.dividend.exception.impl.AlreadyExistUserException;
import com.example.dividend.exception.impl.PasswordMisMatchException;
import com.example.dividend.model.Auth;
import com.example.dividend.persist.MemberRepository;
import com.example.dividend.persist.entity.MemberEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class MemberService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.memberRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("사용자가 없습니다. : {}", username);
                    return new UsernameNotFoundException("couldn't find user -> " + username);
                });
    }

    public MemberEntity register(Auth.SignUp member) {
        log.info("회원 가입 중 - {}", member.getUsername());
        boolean exists = this.memberRepository.existsByUsername(member.getUsername());
        if (exists) {
            log.warn("회원 가입 실패 - 이미 존재하는 사용자 입니다. : {}", member.getUsername());
            throw new AlreadyExistUserException();
        }

        member.setPassword(this.passwordEncoder.encode(member.getPassword()));
        var result = this.memberRepository.save(member.toEntity());
        log.info("회원 가입 성공 - {}", result.getUsername());
        return result;

    }

    public MemberEntity authenticate(Auth.SignIn member) {
        var OptionalUser = this.memberRepository.findByUsername(member.getUsername());
        if (OptionalUser.isEmpty()) {
            log.warn("회원 인증 실패 , 존재하지 않는 ID 입니다. ID : {}", member.getUsername());
            throw new UsernameNotFoundException("유저를 찾을 수 없습니다. : " + member.getUsername());
        }
        var user = OptionalUser.get();

        if (!this.passwordEncoder.matches(member.getPassword(), user.getPassword())) {
            log.warn("회원 인증 실패 , 비밀번호가 일치하지 않습니다.");
            throw new PasswordMisMatchException(member.getUsername());
        }

        return user;
    }
}
