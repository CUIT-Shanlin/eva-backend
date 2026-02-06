package edu.cuit.domain.entity.log;

import com.alibaba.cola.domain.Entity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统日志domain entity
 */
@Entity
@Data
@RequiredArgsConstructor
public class SysLogEntity {

    /**
     * id
     */
    private Integer id;

    /**
     * 日志模块
     */
    private SysLogModuleEntity module;

    /**
     * 操作类型, 0123 对应 增删改查，4是其他
     */
    private Integer type;

    /**
     * 操作者
     */
    private UserEntity user;

    /**
     * 操作者IP
     */
    private String ip;

    /**
     * 操作内容
     */
    private String content;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
