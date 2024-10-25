package edu.cuit.client.dto.clientobject;
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
public class FormPropCO extends ClientObject{
    /**
     * 评教指标
     */
    private String prop;
    /**
     * 该指标对应的分数
     */
    private Number score;
}
