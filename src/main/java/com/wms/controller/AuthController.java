package com.wms.controller;

import com.wms.common.Result;
import com.wms.dto.LoginDto;
import com.wms.dto.RegisterDto;
import com.wms.security.UserDetailsImpl;
import com.wms.service.AuthService;
import com.wms.utils.CaptchaUtil;
import com.wms.vo.LoginVo;
import com.wms.vo.UserInfoVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;
    private final CaptchaUtil captchaUtil;

    @GetMapping("/captcha")
    public Result<CaptchaUtil.CaptchaResult> getCaptcha() {
        return Result.success(captchaUtil.generate());
    }

    @PostMapping("/login")
    public Result<LoginVo> login(@Valid @RequestBody LoginDto loginDto) {
        LoginVo loginVo = authService.login(loginDto);
        return Result.success(loginVo);
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDto registerDto) {
        authService.register(registerDto);
        return Result.success();
    }

    @GetMapping("/userInfo")
    public Result<UserInfoVo> getUserInfo(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        UserInfoVo userInfoVo = authService.getUserInfo(userDetails.getId());
        return Result.success(userInfoVo);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            authService.logout(token);
        }
        return Result.success();
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}