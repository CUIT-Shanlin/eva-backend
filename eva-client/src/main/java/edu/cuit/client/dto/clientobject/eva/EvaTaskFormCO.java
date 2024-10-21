package edu.cuit.client.dto.clientobject.eva;

import com.alibaba.cola.dto.ClientObject;
import edu.cuit.client.dto.data.course.CourseTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 提交评教表单
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
     * 任务id
     */
    private Integer taskId;
}
