package com.wms.service.impl;

import com.wms.dto.LoginDto;
import com.wms.dto.RegisterDto;
import com.wms.mapper.UserMapper;
import com.wms.security.UserDetailsImpl;
import com.wms.service.AuthService;
import com.wms.utils.CaptchaUtil;
import com.wms.utils.JwtUtils;
import com.wms.vo.LoginVo;
import com.wms.vo.UserInfoVo;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;
    private final CaptchaUtil captchaUtil;
    private final UserMapper userMapper;

    public AuthServiceImpl(PasswordEncoder passwordEncoder, JwtUtils jwtUtils,
                           StringRedisTemplate redisTemplate, CaptchaUtil captchaUtil,
                           UserMapper userMapper) {
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.redisTemplate = redisTemplate;
        this.captchaUtil = captchaUtil;
        this.userMapper = userMapper;
    }

    @Override
    public LoginVo login(LoginDto loginDto) {
        captchaUtil.verify(redisTemplate, loginDto.getCaptchaKey(), loginDto.getCaptchaCode());

        var user = userMapper.selectByUsername(loginDto.getUsername());
        if (user == null || !passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        if (user.getStatus() != 1) {
            throw new IllegalArgumentException("Account is disabled");
        }

        UserDetailsImpl userDetails = new UserDetailsImpl(
                user.getId(), user.getUsername(), user.getPassword(),
                user.getEmail(), user.getPhone(), user.getStatus(),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        String token = jwtUtils.generateToken(user.getUsername(), user.getId());
        redisTemplate.opsForValue().set("user:token:" + token, String.valueOf(user.getId()), 2, TimeUnit.HOURS);
        redisTemplate.opsForValue().set("user:perm:" + user.getId(), "[]", 2, TimeUnit.HOURS);

        LoginVo loginVo = new LoginVo();
        loginVo.setAccessToken(token);
        loginVo.setExpiresIn(7200L);
        return loginVo;
    }

    @Override
    public void register(RegisterDto dto) {
        var existing = userMapper.selectByUsername(dto.getUsername());
        if (existing != null) {
            throw new RuntimeException("Username already exists");
        }
        var user = new com.wms.entity.User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setStatus(1);
        userMapper.insert(user);
    }

    @Override
    public void logout(String token) {
        redisTemplate.delete("user:token:" + token);
    }

    @Override
    public UserInfoVo getUserInfo(Long userId) {
        var user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("User not found");
        return UserInfoVo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .roles(List.of())
                .permissions(List.of())
                .build();
    }
}
