package edu.cuit.client.dto.query.condition;

import edu.cuit.client.dto.data.course.CourseTime;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 条件查询-评教任务
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EvaTaskConditionalQuery extends ConditionalQuery{
    /**
     * 输入框中输入的查询关键字
     */
    private String keyword;
    /**
     * 开始的创建时间
     */
    private LocalDateTime startCreateTime;
    /**
     * 结束的创建时间
     */
    private LocalDateTime endCreateTime;
    /**
     * 任务状态
     */
    private Integer taskStatus;
}
