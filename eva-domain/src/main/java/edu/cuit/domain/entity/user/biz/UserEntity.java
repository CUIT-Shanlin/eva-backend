package edu.cuit.domain.entity.user.biz;

import com.alibaba.cola.domain.Entity;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户domain entity
 */
@Entity
@Data
@RequiredArgsConstructor
public class UserEntity {

    /**
     * 用户id
     */
    private Integer id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String name;

    /**
     * 职称
     */
    private String profTitle;

    /**
     * 系
     */
    private String department;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否已被删除
     */
    private Integer isDeleted;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 状态，1为禁止，0为正常
     */
    private Integer status;

    /**
     * 角色列表
     */
    private List<RoleEntity> roles;

    /**
     * 用户账号是否被禁用
     */
    public Boolean isBanned() {
        return status == 1;
    }

}
