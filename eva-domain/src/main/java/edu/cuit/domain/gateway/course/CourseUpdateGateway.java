package edu.cuit.domain.gateway.course;

import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;
import edu.cuit.client.dto.cmd.course.AlignTeacherCmd;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;
import edu.cuit.client.dto.cmd.course.UpdateSingleCourseCmd;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import jakarta.validation.Valid;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.InputStream;
import java.util.List;

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
    Void updateCourse(Integer semId, UpdateCourseCmd updateCourseCmd);

    /**
     * 修改一门课程
     *@param semId 学期id
     *@param updateSingleCourseCmd 修改课程信息
     *@param userName 用户名
     *
     * */
    Void updateSingleCourse(String userName,Integer semId, UpdateSingleCourseCmd updateSingleCourseCmd);

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
    Void assignTeacher(Integer semId, AlignTeacherCmd alignTeacherCmd);

    /**
     * 导入课表文件
     *
     *  @param file 课表文件
     *
     * */
    Void importCourseFile(InputStream file);

    /**
     * 修改自己的一门课程信息及其课程时段
     *@param selfTeachCourseCO 用于确定是导入实验课表还是理论课表，0：理论课，1：实验课
     *  @param timeList 课表文件
     * */
    Void updateSelfCourse(String userName,SelfTeachCourseCO selfTeachCourseCO, List<SelfTeachCourseTimeCO> timeList);
}
