package com.wms.security;

import com.wms.entity.Permission;
import com.wms.entity.Role;
import com.wms.entity.RolePermission;
import com.wms.entity.User;
import com.wms.entity.UserRole;
import com.wms.mapper.PermissionMapper;
import com.wms.mapper.RoleMapper;
import com.wms.mapper.RolePermissionMapper;
import com.wms.mapper.UserMapper;
import com.wms.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectByUsername(username);

        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        List<UserRole> userRoles = userRoleMapper.selectByUserId(user.getId());
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());

        if (!roleIds.isEmpty()) {
            List<Role> roles = roleMapper.selectByIds(roleIds).stream()
                    .filter(r -> r.getStatus() != null && r.getStatus() == 1)
                    .collect(Collectors.toList());

            for (Role role : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleCode()));
            }

            List<Long> roleIdList = roles.stream().map(Role::getId).collect(Collectors.toList());
            if (!roleIdList.isEmpty()) {
                Set<Long> permissionIds = rolePermissionMapper.selectByRoleIds(roleIdList)
                        .stream().map(RolePermission::getPermissionId).collect(Collectors.toSet());

                if (!permissionIds.isEmpty()) {
                    permissionMapper.selectByIds(new ArrayList<>(permissionIds)).stream()
                            .filter(p -> p.getStatus() != null && p.getStatus() == 1)
                            .map(Permission::getPermissionCode)
                            .map(SimpleGrantedAuthority::new)
                            .forEach(authorities::add);
                }
            }
        }

        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus(),
                authorities);
    }
}
