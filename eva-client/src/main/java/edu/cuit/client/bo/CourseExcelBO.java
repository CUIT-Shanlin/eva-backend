package edu.cuit.client.bo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Objects;

/**
 * excel表课程信息
 */
@Data
@Accessors(chain = true)
public class CourseExcelBO {

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 老师名称
     */
    private String teacherName;

    /**
     * 职称（根据老师和职称确认老师，极大避免重名的情况）
     * 只有教授和副教授和null三个值
     */
    private String profTitle;

    /**
     * 开始节数
     */
    private Integer startTime;

    /**
     * 结束节数
     */
    private Integer endTime;

    /**
     * 星期几
     */
    private Integer day;

    /**
     * 周数
     */
    private List<Integer> weeks;

    /**
     * 教室
     */
    private String classroom;

    /**
     * 班级（只用来判断是否为同一节课）
     */
    private String courseClass;

    /**
     * 判断与目标课程是否相邻
     * @param target 目标课程
     * @return 是否相邻
     */
    public boolean isAdjoin(CourseExcelBO target) {
        return Objects.equals(courseName, target.courseName) &&
                Objects.equals(day,target.day) &&
                Objects.equals(weeks,target.weeks) &&
                Objects.equals(courseClass,target.courseClass) &&
                (startTime == target.endTime + 1 || endTime == target.startTime - 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CourseExcelBO that = (CourseExcelBO) o;
        return Objects.equals(courseName, that.courseName) &&
                Objects.equals(teacherName, that.teacherName) &&
                Objects.equals(profTitle, that.profTitle) &&
                Objects.equals(day, that.day) &&
                Objects.equals(weeks, that.weeks) &&
                Objects.equals(classroom, that.classroom) &&
                Objects.equals(courseClass,that.courseClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseName, teacherName, profTitle, day, weeks, classroom);
    }
}
