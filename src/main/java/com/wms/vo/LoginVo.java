package com.wms.vo;

import lombok.Data;

@Data
public class LoginVo {

    private String accessToken;

    private String refreshToken;

    private Long expiresIn;
}
