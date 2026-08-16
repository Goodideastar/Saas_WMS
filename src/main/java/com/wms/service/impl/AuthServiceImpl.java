package com.wms.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.dto.LoginDto;
import com.wms.dto.RegisterDto;
import com.wms.entity.Permission;
import com.wms.entity.Role;
import com.wms.entity.RolePermission;
import com.wms.entity.UserRole;
import com.wms.exception.BusinessException;
import com.wms.mapper.PermissionMapper;
import com.wms.mapper.RoleMapper;
import com.wms.mapper.RolePermissionMapper;
import com.wms.mapper.UserMapper;
import com.wms.mapper.UserRoleMapper;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;
    private final CaptchaUtil captchaUtil;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthServiceImpl(PasswordEncoder passwordEncoder, JwtUtils jwtUtils,
                           StringRedisTemplate redisTemplate, CaptchaUtil captchaUtil,
                           UserMapper userMapper, UserRoleMapper userRoleMapper,
                           RoleMapper roleMapper, RolePermissionMapper rolePermissionMapper,
                           PermissionMapper permissionMapper) {
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.redisTemplate = redisTemplate;
        this.captchaUtil = captchaUtil;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionMapper = permissionMapper;
    }

    private List<Role> loadEnabledRoles(Long userId) {
        List<Long> roleIds = userRoleMapper.selectByUserId(userId).stream()
                .map(UserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectByIds(roleIds).stream()
                .filter(r -> r.getStatus() != null && r.getStatus() == 1)
                .collect(Collectors.toList());
    }

    private List<String> loadPermissionCodes(Long userId) {
        List<Role> roles = loadEnabledRoles(userId);
        if (roles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIdList = roles.stream().map(Role::getId).collect(Collectors.toList());
        Set<Long> permissionIds = rolePermissionMapper.selectByRoleIds(roleIdList).stream()
                .map(RolePermission::getPermissionId).collect(Collectors.toSet());
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return permissionMapper.selectByIds(new ArrayList<>(permissionIds)).stream()
                .filter(p -> p.getStatus() != null && p.getStatus() == 1)
                .map(Permission::getPermissionCode)
                .collect(Collectors.toList());
    }

    @Override
    public LoginVo login(LoginDto loginDto) {
        captchaUtil.verify(loginDto.getCaptchaKey(), loginDto.getCaptchaCode());

        var user = userMapper.selectByUsername(loginDto.getUsername());
        if (user == null || !passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            throw new BusinessException(4003, "用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BusinessException(4004, "账号已被禁用");
        }

        UserDetailsImpl userDetails = new UserDetailsImpl(
                user.getId(), user.getUsername(), user.getPassword(),
                user.getEmail(), user.getPhone(), user.getStatus(),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        String token = jwtUtils.generateToken(user.getUsername(), user.getId());
        redisTemplate.opsForValue().set("user:token:" + token, String.valueOf(user.getId()), 2, TimeUnit.HOURS);
        try {
            redisTemplate.opsForValue().set("user:perm:" + user.getId(),
                    objectMapper.writeValueAsString(loadPermissionCodes(user.getId())), 2, TimeUnit.HOURS);
        } catch (Exception e) {
            redisTemplate.opsForValue().set("user:perm:" + user.getId(), "[]", 2, TimeUnit.HOURS);
        }

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
        List<String> roleCodes = loadEnabledRoles(userId).stream()
                .map(Role::getRoleCode)
                .collect(Collectors.toList());
        return UserInfoVo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .roles(roleCodes)
                .permissions(loadPermissionCodes(userId))
                .build();
    }
}
