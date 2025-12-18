package edu.cuit.infra.gateway.impl.course;

import edu.cuit.bc.course.application.model.DeleteCourseCommand;
import edu.cuit.bc.course.application.model.DeleteCourseTypeCommand;
import edu.cuit.bc.course.application.model.DeleteCoursesCommand;
import edu.cuit.bc.course.application.model.DeleteSelfCourseCommand;
import edu.cuit.bc.course.application.usecase.DeleteCourseTypeUseCase;
import edu.cuit.bc.course.application.usecase.DeleteCourseUseCase;
import edu.cuit.bc.course.application.usecase.DeleteCoursesUseCase;
import edu.cuit.bc.course.application.usecase.DeleteSelfCourseUseCase;
import edu.cuit.client.dto.data.course.CoursePeriod;
import edu.cuit.domain.gateway.course.CourseDeleteGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CourseDeleteGatewayImpl implements CourseDeleteGateway {
    private final DeleteSelfCourseUseCase deleteSelfCourseUseCase;
    private final DeleteCoursesUseCase deleteCoursesUseCase;
    private final DeleteCourseUseCase deleteCourseUseCase;
    private final DeleteCourseTypeUseCase deleteCourseTypeUseCase;


    /**
     * 批量删除某节课
     *
     * @param semId        学期id
     * @param id           对应课程编号
     * @param coursePeriod 课程的一段时间模型
     */
    @Override
    @Transactional
    public Map<String, Map<Integer, Integer>> deleteCourses(Integer semId, Integer id, CoursePeriod coursePeriod) {
        // 历史路径：收敛到 bc-course 用例，基础设施层避免继续堆“批量删课”业务流程（行为不变）
        return deleteCoursesUseCase.execute(new DeleteCoursesCommand(semId, id, coursePeriod));
    }


    /**
     * 连带删除一门课程
     *
     * @param semId 学期id
     * @param id    对应课程编号
     */
    @Override
    @Transactional
    public Map<String, Map<Integer, Integer>> deleteCourse(Integer semId, Integer id) {
        // 历史路径：收敛到 bc-course 用例，基础设施层避免继续堆“删课”业务流程（行为不变）
        return deleteCourseUseCase.execute(new DeleteCourseCommand(semId, id));
    }


    /**
     * 删除一个课程类型/批量删除课程类型
     *
     * @param ids 课程类型数组
     */
    @Override
    @Transactional
    public Void deleteCourseType(List<Integer> ids) {
        // 历史路径：收敛到 bc-course 用例，基础设施层避免继续堆“删课程类型”业务流程（行为不变）
        deleteCourseTypeUseCase.execute(new DeleteCourseTypeCommand(ids));
        return null;
    }

    @Override
    @Transactional
    public Map<String, Map<Integer, Integer>> deleteSelfCourse(String userName, Integer courseId) {
        // 历史路径：收敛到 bc-course 用例，基础设施层避免继续堆“自助删课”业务流程（行为不变）
        return deleteSelfCourseUseCase.execute(new DeleteSelfCourseCommand(userName, courseId));
    }

}
