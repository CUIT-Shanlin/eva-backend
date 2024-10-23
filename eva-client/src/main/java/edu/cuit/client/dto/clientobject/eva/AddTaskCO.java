package edu.cuit.client.dto.clientobject.eva;
import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 一门课的评教分数
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class AddTaskCO extends ClientObject{
    /**
     * 课程信息id
     */
    private Integer courInfId;
    /**
     * 评教老师id
     */
    private Integer teacherId;
}
