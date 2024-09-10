package edu.cuit.client.dto.data.course;

import com.alibaba.cola.dto.ClientObject;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Range;

/**
 * 课程的一段时间的模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class CoursePeriod extends ClientObject {
    /**
     * 从哪周开始
     */
    @Range(min = 1,max = 20,message = "周数必须在1和20之间")
    private Integer startWeek;

    /**
     * 从哪周结束
     */
    @Range(min = 1,max = 20,message = "周数必须在1和20之间")
    private Integer endWeek;

    /**
     * 星期几
     */
    @Range(min = 1,max = 7,message = "星期数必须在1和7之间")
    private Integer day;

    /**
     * 开始时间
     */
    @Range(min = 1,max = 11,message = "开始时间必须在1和11之间")
    private Integer startTime;

    /**
     * 结束时间
     */
    @Range(min = 1,max = 11,message = "开始时间必须在1和11之间")
    private Integer endTime;




}
