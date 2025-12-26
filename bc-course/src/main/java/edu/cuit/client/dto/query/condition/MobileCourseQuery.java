package edu.cuit.client.dto.query.condition;

import com.alibaba.cola.dto.ClientObject;
import com.alibaba.cola.dto.Query;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 针对于“获取某个指定时间段的课程”接口衍生的条件接受类
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MobileCourseQuery extends Query {
    /**
     * 查询关键字
     */
    private String keyword;

    /**
     * 老师的id
     */
    @Min(value = 0,message = "该课程的教学教师的id最小为0")
    @Max(value = 20,message = "该课程的教学教师的id最大为20")
    private Integer teacherId;

    /**
     * 学院名称
     */
    private String departmentName;

    /**
     * 排序方式0: 选过次数升序；1：时间升序；2：时间降序
     */
    @Min(value = 0,message = "排序方式sort最小为0")
    @Max(value = 2,message = "排序方式sort最大为2")
    private Integer sort;

    /**
     * 开始日期
     */
    private LocalDateTime startDay;

    /**
     * 结束日期
     */
    private LocalDateTime endDay;

    /**
     * 课程类型id
     */
    @Min(value = 0,message = "课程类型id最小为0")
    @Max(value = 20,message = "课程类型id最大为20")
    private Integer typeId;

}
