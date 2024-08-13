package edu.cuit.domain.entity.user.bizuser;

import lombok.Data;

/**
 * 用户头像value object
 */
@Data
public class UserAvatarVO {

    /**
     * 头像标识符
     */
    public String avatarName;
    //TODO 考虑如何存储图片


}
