package edu.cuit.client.dto.clientobject.course;

import com.alibaba.cola.dto.ClientObject;
import edu.cuit.client.dto.data.course.CoursePeriod;
import edu.cuit.client.dto.data.course.CourseTime;
import edu.cuit.client.dto.data.course.CourseType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 推荐课程的模型
* */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class RecommendCourseCO extends ClientObject {

    /**
     * 课程详情ID
     */
    private Integer id;

    /**
     * 课程名称
     */
    private String name;

    /**
     * 老师姓名
     */
    private String teacherName;

    /**
     * 评教数目
     */
    private Integer evaNum;

    /**
     * 上课位置
     */
    private String location;

    /**
     * 一节课的时间
     */
    private CourseTime time;

    /**
     * 该教学老师被评教的次数
     */
    private Integer evaTeacherNum;

    /**
     * 课程类型集合
     */
    private List<CourseType> typeList;

    /**
     * 这节课被推荐的优先级
     */
    private Double priority;

    /**
     * 这节课和自己的教学课程的类型相似度
     */
    private Double typeSimilarity;

    /**
     * 课程性质(0:理论课,1:实验课,3:其他)
     */
    private Integer nature;

}
