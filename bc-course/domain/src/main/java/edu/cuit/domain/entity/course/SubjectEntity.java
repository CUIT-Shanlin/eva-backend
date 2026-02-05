package edu.cuit.domain.entity.course;

import com.alibaba.cola.domain.Entity;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 科目domain entity
 */
@Entity
@Data
@RequiredArgsConstructor
public class SubjectEntity {

    /**
     * 课程名称id
     */
    private Integer id;

    /**
     * 课程名称
     */
    private String name;

    /**
     * 课程性质(0:理论课,1:实验课,3:其他)
     */
    private Integer nature;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
