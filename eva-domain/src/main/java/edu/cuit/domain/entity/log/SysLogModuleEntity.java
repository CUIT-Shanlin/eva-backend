package edu.cuit.domain.entity.log;

import com.alibaba.cola.domain.Entity;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * 系统日志domain entity
 */
@Entity
@Data
@RequiredArgsConstructor
public class SysLogModuleEntity {

    /**
     * id
     */
    private Integer id;

    /**
     * 模块名称
     */
    private String name;

}
