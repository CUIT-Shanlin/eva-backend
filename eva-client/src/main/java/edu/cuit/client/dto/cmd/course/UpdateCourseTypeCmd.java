package edu.cuit.client.dto.cmd.course;

import com.alibaba.cola.dto.ClientObject;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 修改课程类型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class UpdateCourseTypeCmd extends ClientObject {
    /**
     * id
     */
    @NotNull(message = "课程类型id不能为空")
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
