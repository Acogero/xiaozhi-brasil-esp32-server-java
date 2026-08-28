package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * BO de informações de autorização de terceiros do usuário (correspondente à tabela sys_user_auth).
 */
@Data
public class UserAuthBO {

    private Long id;
    private Integer userId;
    private String openId;
    private String unionId;
    private String platform;
    private String profile;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
