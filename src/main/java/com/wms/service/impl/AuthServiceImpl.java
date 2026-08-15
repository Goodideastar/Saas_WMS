package com.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.dto.LoginDto;
import com.wms.dto.RegisterDto;
import com.wms.entity.Permission;
import com.wms.entity.Role;
import com.wms.entity.RolePermission;
import com.wms.entity.User;
import com.wms.entity.UserRole;
import com.wms.exception.BusinessException;
import com.wms.mapper.PermissionMapper;
import com.wms.mapper.RoleMapper;
import com.wms.mapper.RolePermissionMapper;
import com.wms.mapper.UserMapper;
import com.wms.mapper.UserRoleMapper;
import com.wms.service.AuthService;
import com.wms.utils.CaptchaUtil;
import com.wms.utils.JwtUtils;
import com.wms.vo.LoginVo;
import com.wms.vo.UserInfoVo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;
    private final CaptchaUtil captchaUtil;

    private static final String TOKEN_BLACKLIST_PREFIX = "auth:blacklist:";
    private static final String USER_PERM_KEY = "user:perm:";

    @Override
    public LoginVo login(LoginDto loginDto) {
        captchaUtil.verify(redisTemplate, loginDto.getCaptchaKey(), loginDto.getCaptchaCode());

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginDto.getUsername());
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new BusinessException(401, "User not found");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "Account is disabled");
        }

        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "Invalid password");
        }

        String accessToken = jwtUtils.generateToken(user.getUsername(), user.getId());
        String refreshToken = jwtUtils.generateRefreshToken(user.getUsername(), user.getId());

        LoginVo loginVo = new LoginVo();
        loginVo.setAccessToken(accessToken);
        loginVo.setRefreshToken(refreshToken);
        loginVo.setExpiresIn(jwtUtils.getAccessTokenExpiration());

        return loginVo;
    }

    @Override
    public void register(RegisterDto registerDto) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, registerDto.getUsername());
        Long count = userMapper.selectCount(queryWrapper);

        if (count > 0) {
            throw new BusinessException(400, "Username already exists");
        }

        User user = new User();
        user.setUsername(registerDto.getUsername());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setEmail(registerDto.getEmail());
        user.setPhone(registerDto.getPhone());
        user.setStatus(1);

        userMapper.insert(user);
    }

    @Override
    public void logout(String token) {
        redisTemplate.opsForValue().set(TOKEN_BLACKLIST_PREFIX + token, "1", 24, TimeUnit.HOURS);
    }

    @Override
    public UserInfoVo getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException(404, "User not found");
        }

        List<Long> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).collect(Collectors.toList());

        List<Role> roles = roleIds.isEmpty()
                ? List.of()
                : roleMapper.selectBatchIds(roleIds).stream()
                        .filter(r -> r.getStatus() != null && r.getStatus() == 1)
                        .collect(Collectors.toList());

        List<String> roleNames = roles.stream().map(Role::getRoleName).collect(Collectors.toList());

        List<Long> roleIdList = roles.stream().map(Role::getId).collect(Collectors.toList());
        Set<Long> permissionIds = roleIdList.isEmpty()
                ? Set.of()
                : rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIdList))
                        .stream().map(RolePermission::getPermissionId).collect(Collectors.toSet());

        List<Permission> permissions = permissionIds.isEmpty()
                ? List.of()
                : permissionMapper.selectBatchIds(permissionIds).stream()
                        .filter(p -> p.getStatus() != null && p.getStatus() == 1)
                        .collect(Collectors.toList());

        List<String> permissionCodes = permissions.stream()
                .map(Permission::getPermissionCode).collect(Collectors.toList());

        redisTemplate.opsForValue().set(USER_PERM_KEY + userId,
                String.join(",", permissionCodes), 2, TimeUnit.HOURS);

        UserInfoVo userInfoVo = new UserInfoVo();
        userInfoVo.setId(user.getId());
        userInfoVo.setUsername(user.getUsername());
        userInfoVo.setEmail(user.getEmail());
        userInfoVo.setPhone(user.getPhone());
        userInfoVo.setRoles(roleNames);
        userInfoVo.setPermissions(permissionCodes);

        return userInfoVo;
    }
}
