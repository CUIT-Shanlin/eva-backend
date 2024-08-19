package edu.cuit.client.dto.query.course;

import com.alibaba.cola.dto.DTO;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Range;

/**
 * 课程时间模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CourseTime extends DTO {

    /**
     * 课程周数
     */
    @Min(value = 1,message = "周数不能小于1")
    @NotNull(message = "周数不能为空")
    private Integer week;

    /**
     * 课程星期数（星期几）
     */
    @Range(min = 1,max = 7,message = "星期数必须在1和7之间")
    @NotNull(message = "星期数不能为空")
    private Integer day;

    /**
     * 开始时间（第几节）
     */
    @Min(value = 1,message = "开始时间必须大于等于1")
    @NotNull(message = "开始时间不能为空")
    private Integer startTime;

    /**
     * 结束时间（第几节）
     */
    @Min(value = 1,message = "结束时间必须大于等于1")
    @NotNull(message = "结束时间不能为空")
    private Integer endTime;
}
