package edu.cuit.client.dto.clientobject.eva;

import com.alibaba.cola.dto.ClientObject;
import edu.cuit.client.dto.clientobject.FormPropCO;
import edu.cuit.client.dto.data.course.CourseTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 提交评教表单
 */
@EqualsAndHashCode(callSuper = false)
@Data
@Accessors(chain = true)
public class EvaTaskFormCO extends ClientObject{
    /**
     * 表单评教指标对应的分值，JSON表示的字符串形式
     */
    private String textValue;
    /**
     * 表单评教指标对应的分值，JSON表示的字符串形式
     */
    private List<FormPropCO> formPropsValues;
    /**
     * 任务id
     */
    private Integer taskId;
}
