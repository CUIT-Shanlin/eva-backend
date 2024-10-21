package edu.cuit.client.dto.data.msg;

import edu.cuit.client.dto.clientobject.course.SingleCourseCO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 评教响应消息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class EvaResponseMsg extends GenericResponseMsg {

    /**
     * 一节课的模型
     */
    private SingleCourseCO courseInfo;

}
