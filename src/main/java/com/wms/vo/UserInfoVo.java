package com.wms.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserInfoVo {

    private Long id;

    private String username;

    private String email;

    private String phone;

    private Integer status;

    private List<String> roles;

    private List<String> permissions;
}
