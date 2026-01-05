package edu.cuit.infra.gateway.impl.course;

import edu.cuit.client.bo.CourseExcelBO;
import edu.cuit.client.dto.clientobject.SemesterCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeInfoCO;
import edu.cuit.client.dto.cmd.course.*;
import edu.cuit.client.dto.data.Term;
import edu.cuit.client.dto.data.course.CourseType;
import edu.cuit.domain.gateway.course.CourseUpdateGateway;
import edu.cuit.bc.course.application.usecase.UpdateCoursesEntryUseCase;
import edu.cuit.bc.course.domain.ChangeCourseTemplateException;
import edu.cuit.bc.course.application.usecase.AssignTeacherGatewayEntryUseCase;
import edu.cuit.bc.course.domain.AssignEvaTeachersException;
import edu.cuit.bc.course.application.model.ImportCourseFileCommand;
import edu.cuit.bc.course.application.usecase.ImportCourseFileUseCase;
import edu.cuit.bc.course.domain.ImportCourseFileException;
import edu.cuit.bc.course.application.usecase.IsCourseImportedUseCase;
import edu.cuit.bc.course.application.usecase.UpdateCourseGatewayEntryUseCase;
import edu.cuit.bc.course.domain.UpdateCourseInfoException;
import edu.cuit.bc.course.application.usecase.UpdateCourseTypeEntryUseCase;
import edu.cuit.bc.course.application.usecase.UpdateCoursesTypeEntryUseCase;
import edu.cuit.bc.course.application.model.UpdateSingleCourseCommand;
import edu.cuit.bc.course.application.usecase.UpdateSingleCourseUseCase;
import edu.cuit.bc.course.domain.UpdateSingleCourseException;
import edu.cuit.bc.course.application.usecase.AddCourseTypeEntryUseCase;
import edu.cuit.bc.course.application.model.AddNotExistCoursesDetailsCommand;
import edu.cuit.bc.course.application.usecase.AddNotExistCoursesDetailsUseCase;
import edu.cuit.bc.course.application.model.AddExistCoursesDetailsCommand;
import edu.cuit.bc.course.application.usecase.AddExistCoursesDetailsUseCase;
import edu.cuit.bc.course.application.model.UpdateSelfCourseCommand;
import edu.cuit.bc.course.application.usecase.UpdateSelfCourseUseCase;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
public class CourseUpdateGatewayImpl implements CourseUpdateGateway {
    private final UpdateCoursesEntryUseCase updateCoursesEntryUseCase;
    private final AssignTeacherGatewayEntryUseCase assignTeacherGatewayEntryUseCase;
    private final UpdateSingleCourseUseCase updateSingleCourseUseCase;
    private final ImportCourseFileUseCase importCourseFileUseCase;
    private final UpdateCourseGatewayEntryUseCase updateCourseGatewayEntryUseCase;
    private final UpdateCourseTypeEntryUseCase updateCourseTypeEntryUseCase;
    private final UpdateCoursesTypeEntryUseCase updateCoursesTypeEntryUseCase;
    private final UpdateSelfCourseUseCase updateSelfCourseUseCase;
    private final AddCourseTypeEntryUseCase addCourseTypeEntryUseCase;
    private final AddNotExistCoursesDetailsUseCase addNotExistCoursesDetailsUseCase;
    private final AddExistCoursesDetailsUseCase addExistCoursesDetailsUseCase;
    private final IsCourseImportedUseCase isCourseImportedUseCase;


    /**
     * 修改一门课程
     *@param semId 学期id
     *@param updateCourseCmd 修改课程信息
     *
     * */
    @Override
    @Transactional
    public Map<String,Map<Integer,Integer>> updateCourse(Integer semId, UpdateCourseCmd updateCourseCmd) {
        // 历史路径：收敛到 bc-course 用例，基础设施层避免继续堆“修改课程信息”业务流程
        try {
            return updateCourseGatewayEntryUseCase.updateCourse(semId, updateCourseCmd);
        } catch (UpdateCourseInfoException e) {
            throw new UpdateException(e.getMessage());
        }
    }


    /**
     * 批量修改课程的模板
     *@param semId 学期id
     *  @param updateCoursesCmd 批量修改课程信息
     *
     * */
    @Override
    @Transactional
    public void updateCourses(Integer semId, UpdateCoursesCmd updateCoursesCmd) {
        // 历史路径：收敛到 bc-course 用例，避免基础设施层重复实现与重复校验
        try {
            updateCoursesEntryUseCase.updateCourses(semId, updateCoursesCmd);
        } catch (ChangeCourseTemplateException e) {
            throw new UpdateException(e.getMessage());
        }

    }

    @Override
    @Transactional
    public Map<String,Map<Integer,Integer>> updateSingleCourse(String userName,Integer semId, UpdateSingleCourseCmd updateSingleCourseCmd) {
        // 历史路径：收敛到 bc-course 用例（保持行为不变）
        try {
            return updateSingleCourseUseCase.execute(new UpdateSingleCourseCommand(
                    semId,
                    updateSingleCourseCmd.getId(),
                    updateSingleCourseCmd.getTime() == null ? null : updateSingleCourseCmd.getTime().getWeek(),
                    updateSingleCourseCmd.getTime() == null ? null : updateSingleCourseCmd.getTime().getDay(),
                    updateSingleCourseCmd.getTime() == null ? null : updateSingleCourseCmd.getTime().getStartTime(),
                    updateSingleCourseCmd.getTime() == null ? null : updateSingleCourseCmd.getTime().getEndTime(),
                    updateSingleCourseCmd.getLocation()
            ));
        } catch (UpdateSingleCourseException e) {
            throw new UpdateException(e.getMessage());
        }
    }

    @Override
    @Transactional
    public Void updateCourseType(UpdateCourseTypeCmd courseType) {
        // 历史路径：收敛到 bc-course 用例，基础设施层避免继续堆“课程类型修改”业务流程
        updateCourseTypeEntryUseCase.updateCourseType(courseType);
        return null;
    }

    @Override
    @Transactional
    public Void addCourseType(CourseType courseType) {
        // 历史路径：收敛到 bc-course 用例，基础设施层避免继续堆“新增课程类型”业务流程（行为不变）
        addCourseTypeEntryUseCase.addCourseType(courseType);
        return null;
    }

    @Override
    @Transactional
    public Void addCourse(Integer semId) {
        //TODO（接口已删除）
        return null;
    }

    @Override
    @Transactional
    public Map<String,Map<Integer,Integer>> assignTeacher(Integer semId, AlignTeacherCmd alignTeacherCmd) {
        // 历史路径：收敛到 bc-course 用例，避免基础设施层继续堆业务规则
        try {
            return assignTeacherGatewayEntryUseCase.assignTeacher(semId, alignTeacherCmd);
        } catch (AssignEvaTeachersException e) {
            throw new UpdateException(e.getMessage());
        }
    }

    @Override
    @Transactional
    public Map<String,Map<Integer,Integer>> importCourseFile(Map<String, List<CourseExcelBO>> courseExce, SemesterCO semester, Integer type) {
        // 历史路径：收敛到 bc-course 用例，基础设施层避免继续堆“导入课表”业务流程
        try {
            return importCourseFileUseCase.execute(new ImportCourseFileCommand(courseExce, semester, type));
        } catch (ImportCourseFileException e) {
            throw new UpdateException(e.getMessage());
        }
    }

    @Override
    @Transactional
    public Map<String,Map<Integer,Integer>> updateSelfCourse(String userName, SelfTeachCourseCO selfTeachCourseCO, List<SelfTeachCourseTimeInfoCO> timeList) {
        // 历史路径：收敛到 bc-course 用例，基础设施层避免继续堆“自助改课”业务流程（行为不变）
        return updateSelfCourseUseCase.execute(new UpdateSelfCourseCommand(userName, selfTeachCourseCO, timeList));
    }

    @Override
    @Transactional
    public Void addExistCoursesDetails(Integer courseId, SelfTeachCourseTimeCO timeCO) {
        // 历史路径：收敛到 bc-course 用例，基础设施层避免继续堆“新增课次”业务流程（行为不变）
        addExistCoursesDetailsUseCase.execute(new AddExistCoursesDetailsCommand(courseId, timeCO));
        return null;
    }

    @Override
    @Transactional
    public void addNotExistCoursesDetails(Integer semId, Integer teacherId, UpdateCourseCmd courseInfo, List<SelfTeachCourseTimeCO> dateArr) {
        // 历史路径：收敛到 bc-course 用例，基础设施层避免继续堆“新建课程明细”业务流程（行为不变）
        addNotExistCoursesDetailsUseCase.execute(new AddNotExistCoursesDetailsCommand(semId, teacherId, courseInfo, dateArr));
    }

    @Override
    public Boolean isImported(Integer type, Term term) {
        // 历史路径：收敛到 bc-course Query 用例，旧 gateway 退化为委托壳（保持行为不变）
        return isCourseImportedUseCase.execute(type, term);
    }

    @Override
    @Transactional
    public void updateCoursesType(UpdateCoursesToTypeCmd updateCoursesToTypeCmd) {
        // 历史路径：收敛到 bc-course 用例，基础设施层避免继续堆“批量课程类型修改”业务流程
        updateCoursesTypeEntryUseCase.updateCoursesType(updateCoursesToTypeCmd);
    }
}
