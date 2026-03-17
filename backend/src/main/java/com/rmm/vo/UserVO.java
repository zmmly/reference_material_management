package com.rmm.vo;

import lombok.Data;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private Long roleId;
    private String roleName;
    private String roleCode;
}
