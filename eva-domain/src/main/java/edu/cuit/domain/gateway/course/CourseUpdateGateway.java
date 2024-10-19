package edu.cuit.domain.gateway.course;

import edu.cuit.client.bo.CourseExcelBO;
import edu.cuit.client.dto.clientobject.SemesterCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;
import edu.cuit.client.dto.clientobject.course.SubjectCO;
import edu.cuit.client.dto.cmd.course.AlignTeacherCmd;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;
import edu.cuit.client.dto.cmd.course.UpdateCoursesCmd;
import edu.cuit.client.dto.cmd.course.UpdateSingleCourseCmd;
import edu.cuit.client.dto.data.Term;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import jakarta.validation.Valid;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 课程更新相关数据门户接口
 */
@Component
public interface CourseUpdateGateway {
    /**
     * 修改一门课程
     *@param semId 学期id
     *@param updateCourseCmd 修改课程信息
     *
     * */
    String updateCourse(Integer semId, UpdateCourseCmd updateCourseCmd);

    /**
     * 批量修改课程的模板
     *@param semId 学期id
     *  @param updateCoursesCmd 批量修改课程信息
     *
     * */
    void updateCourses(Integer semId, UpdateCoursesCmd updateCoursesCmd);

    /**
     * 修改一节课程
     *@param semId 学期id
     *@param updateSingleCourseCmd 修改课程信息
     *@param userName 用户名
     *
     * */
    Map<String,List<Integer>> updateSingleCourse(String userName,Integer semId, UpdateSingleCourseCmd updateSingleCourseCmd);

    /**
     * 修改一节课的类型
     *  @param courseType 修改课课程类型
     *
     * */
    Void updateCourseType(CourseType courseType);

    /**
     * 新建一个课程类型
     *
     *  @param courseType 课程类型
     *
     * */
    Void addCourseType(CourseType courseType);

    /**
     * 新建一门课程
     *  @param semId 学期id
     *
     * */
    Void addCourse(Integer semId);

    /**
     * 分配听课/评教老师
     *  @param semId 学期id
     *  @param alignTeacherCmd 内涵课程id，以及听课老师集合
     *
     * */
    Map<String,List<Integer>> assignTeacher(Integer semId, AlignTeacherCmd alignTeacherCmd);

    /**
     * 导入课表文件
     *
     *  @param courseExce 科目对应的课程信息
     *  @param semester 学期
     *
     * */
    Void importCourseFile( Map<String, List<CourseExcelBO>> courseExce, SemesterCO semester, Integer type);

    /**
     * 修改自己的一门课程信息及其课程时段
     *@param selfTeachCourseCO 用于确定是导入实验课表还是理论课表，0：理论课，1：实验课
     *  @param timeList 课表文件
     *  @param userName 用户名
     * */
    Map<String,Map<Integer,Integer>> updateSelfCourse(String userName,SelfTeachCourseCO selfTeachCourseCO, List<SelfTeachCourseTimeCO> timeList);

    /**
     * 批量新建多节课(已有课程)
     *  @param courseId 课程id
     *  @param timeCO 课程对应授课时间
     * */
    Void addExistCoursesDetails( Integer courseId, SelfTeachCourseTimeCO timeCO);

    /**
     * 批量新建多节课(新课程)
     *  @param semId 学期ID
     *  @param teacherId 教学老师ID
     *  @param courseInfo 一门课程的可修改信息(一门课程的可修改信息)
     *  @param dateArr 自己教学的一门课程的一个课程时段模型集合
     * */
    void addNotExistCoursesDetails(Integer semId,Integer teacherId, UpdateCourseCmd courseInfo,  List<SelfTeachCourseTimeCO> dateArr);

}
