package edu.cuit.client.dto.clientobject.course;

import com.alibaba.cola.dto.ClientObject;
import edu.cuit.client.dto.data.course.CourseType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 自己的一门教学课程信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class SelfTeachCourseCO extends ClientObject {

    /**
     * 课程id
     */
    private Integer id;


    /**
     * 课程名称
     */
    private String name;

    /**
     * 课程类型数组
     */
    private List<CourseType> typeList;


    /**
     * 课程性质
     */
    private Integer nature;


    /**
     * 评教数量
     */
    private Integer evaNum;



}
