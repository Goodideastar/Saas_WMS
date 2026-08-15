package com.wms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDto {

    @NotBlank(message = "username is required")
    private String username;

    @NotBlank(message = "password is required")
    private String password;

    @NotBlank(message = "captchaKey is required")
    private String captchaKey;

    @NotBlank(message = "captchaCode is required")
    private String captchaCode;
}
