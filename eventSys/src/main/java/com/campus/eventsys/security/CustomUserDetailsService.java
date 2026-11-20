package com.campus.eventsys.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.eventsys.entity.User;
import com.campus.eventsys.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        String role = user.getRole() != null && user.getRole() == 1 ? "ROLE_ADMIN" : "ROLE_USER";

        // 校验账号封禁状态：status=0 表示已被禁用（封禁），禁用账号无法通过认证
        // 既拦截登录（DaoAuthenticationProvider 抛 DisabledException），也使已签发的 JWT 在后续请求中失效
        boolean enabled = user.getStatus() == null || user.getStatus() == 1;

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(role)))
                .disabled(!enabled)
                .build();
    }
}
