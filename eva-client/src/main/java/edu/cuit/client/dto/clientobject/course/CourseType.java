package edu.cuit.client.dto.clientobject.course;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 课程类型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseType {
    /**
     * 课程id
     */
    @NotNull(message = "课程id不能为空")
    private Integer id;

    /**
     * 课程名称
     */
    @NotNull(message = "课程名称不能为空")
    private String name;

    /**
     * 课程描述
     */
    @NotNull(message = "课程描述不能为空")
    private String description;

    /**
     * 课程创建时间
     */
    @NotNull(message = "课程创建时间不能为空")
    private String createTime;

    /**
     * 课程更新时间
     */
    @NotNull(message = "课程更新时间不能为空")
    private String updateTime;
}
