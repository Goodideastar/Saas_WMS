package com.wms.service;

import com.wms.dto.LoginDto;
import com.wms.dto.RegisterDto;
import com.wms.vo.LoginVo;
import com.wms.vo.UserInfoVo;

public interface AuthService {

    LoginVo login(LoginDto loginDto);

    void register(RegisterDto registerDto);

    void logout(String token);

    UserInfoVo getUserInfo(Long userId);
}
