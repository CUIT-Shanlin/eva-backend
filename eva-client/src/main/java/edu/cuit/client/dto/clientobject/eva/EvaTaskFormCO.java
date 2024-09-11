package edu.cuit.client.dto.clientobject.eva;

import com.alibaba.cola.dto.ClientObject;
import edu.cuit.client.dto.data.course.CourseTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 接口-评教相关-其他操作-提交评教表单 需要
 * 评教表单评价分值信息
 */
@EqualsAndHashCode(callSuper = false)
@Data
@Accessors(chain = true)
public class EvaTaskFormCO extends ClientObject{
    /**
     * id
     */
    private Long id;
    /**
     * 表单评教指标对应的分值，JSON表示的字符串形式
     */
    private String textValue;
    /**
     * 表单评教指标对应的分值，JSON表示的字符串形式
     */
    private String formPropsValues;
    /**
     * 上课的时间，xx周 星期x x节课到x节课
     */
    private CourseTime courseTime;
    /**
     * 任务id
     */
    private Integer taskId;
}
