package com.wms.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserInfoVo {

    private Long id;

    private String username;

    private String email;

    private String phone;

    private List<String> roles;

    private List<String> permissions;
}
