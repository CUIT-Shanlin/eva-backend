package edu.cuit.client.dto.clientobject.eva;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 用户的某节课的评分
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class UserSingleCourseScoreCO extends ClientObject {

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 该课程的分数
     */
    private Double score;

    /**
     * 评教次数
     */
    private Integer evaNum;

}
