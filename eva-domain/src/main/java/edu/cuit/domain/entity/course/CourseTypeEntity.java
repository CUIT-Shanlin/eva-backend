package edu.cuit.domain.entity.course;

import com.alibaba.cola.domain.Entity;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 课程类型domain entity
 */
@Data
@RequiredArgsConstructor
@Entity
public class CourseTypeEntity {

    /**
     * id
     */
    private Integer id;

    /**
     * 类型名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 删除标记（0:不可用 1:可用）
     */
    private Integer isDeleted;

}
