package edu.cuit.client.dto.data.course;

import com.alibaba.cola.dto.ClientObject;
import com.alibaba.cola.dto.DTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 课程类型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class CourseType extends DTO {
    /**
     * 课程id
     */
    private Integer id;

    /**
     * 类型名称
     */
    private String name;

    /**
     * 课程描述
     */
    private String description;

    /**
     * 课程创建时间
     */
    private LocalDateTime createTime;

    /**
     * 课程更新时间
     */
    private LocalDateTime updateTime;
}
