package edu.cuit.bc.evaluation.application.usecase;

import com.alibaba.cola.exception.SysException;
import edu.cuit.bc.evaluation.application.port.EvaTaskInfoQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaTaskPagingQueryPort;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskBaseInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskDetailInfoCO;
import edu.cuit.client.dto.data.course.CourseTime;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaTaskConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.entity.eva.EvaTaskEntity;

import java.util.List;
import java.util.Objects;

/**
 * 评教任务读侧查询用例（QueryUseCase）。
 *
 * <p>当前阶段仅做“用例归位 + 委托壳”，不改变任何业务语义；
 * 旧入口仍保留 {@code @CheckSemId} 等触发点。</p>
 */
public class EvaTaskQueryUseCase {
    private final EvaTaskPagingQueryPort evaTaskPagingQueryPort;
    private final EvaTaskInfoQueryPort evaTaskInfoQueryPort;

    public EvaTaskQueryUseCase(
            EvaTaskPagingQueryPort evaTaskPagingQueryPort,
            EvaTaskInfoQueryPort evaTaskInfoQueryPort
    ) {
        this.evaTaskPagingQueryPort = Objects.requireNonNull(evaTaskPagingQueryPort, "evaTaskPagingQueryPort");
        this.evaTaskInfoQueryPort = Objects.requireNonNull(evaTaskInfoQueryPort, "evaTaskInfoQueryPort");
    }

    public PaginationQueryResultCO<EvaTaskBaseInfoCO> pageEvaUnfinishedTaskAsPaginationQueryResult(
            Integer semId,
            PagingQuery<EvaTaskConditionalQuery> query
    ) {
        PaginationResultEntity<EvaTaskEntity> page = evaTaskPagingQueryPort.pageEvaUnfinishedTask(semId, query);
        List<EvaTaskBaseInfoCO> results = page.getRecords().stream()
                .map(EvaTaskQueryUseCase::evaTaskEntityToEvaBaseCO)
                .toList();

        PaginationQueryResultCO<EvaTaskBaseInfoCO> pageCO = new PaginationQueryResultCO<>();
        pageCO.setCurrent(page.getCurrent())
                .setSize(page.getSize())
                .setTotal(page.getTotal())
                .setRecords(results);
        return pageCO;
    }

    public EvaTaskDetailInfoCO oneEvaTaskInfo(Integer id) {
        EvaTaskEntity evaTaskEntity = evaTaskInfoQueryPort.oneEvaTaskInfo(id)
                .orElseThrow(() -> new SysException("并没有找到相关任务信息"));

        // 重要：保持与旧实现一致的懒加载触发顺序（先 courInf，再 teacher）
        SingleCourseEntity singleCourseEntity = evaTaskEntity.getCourInf();
        return evaTaskEntityToTaskDetailCO(evaTaskEntity, singleCourseEntity);
    }

    private static EvaTaskBaseInfoCO evaTaskEntityToEvaBaseCO(EvaTaskEntity evaTaskEntity) {
        if (evaTaskEntity == null) {
            return null;
        }

        EvaTaskBaseInfoCO evaTaskBaseInfoCO = new EvaTaskBaseInfoCO();

        if (evaTaskEntity.getId() != null) {
            evaTaskBaseInfoCO.setId(evaTaskEntity.getId().longValue());
        }
        if (evaTaskEntity.getStatus() != null) {
            evaTaskBaseInfoCO.setStatus(evaTaskEntity.getStatus().longValue());
        }
        evaTaskBaseInfoCO.setCreateTime(evaTaskEntity.getCreateTime());
        evaTaskBaseInfoCO.setUpdateTime(evaTaskEntity.getUpdateTime());

        evaTaskBaseInfoCO.setEvaTeacherName(evaTaskEntity.getTeacher().getName());
        evaTaskBaseInfoCO.setTeacherName(evaTaskEntity.getCourInf().getCourseEntity().getTeacher().getName());
        evaTaskBaseInfoCO.setCourseName(evaTaskEntity.getCourInf().getCourseEntity().getSubjectEntity().getName());

        return evaTaskBaseInfoCO;
    }

    private static EvaTaskDetailInfoCO evaTaskEntityToTaskDetailCO(EvaTaskEntity evaTaskEntity, SingleCourseEntity singleCourseEntity) {
        if (evaTaskEntity == null && singleCourseEntity == null) {
            return null;
        }

        // 重要：保持与历史 MapStruct 生成实现一致的赋值与求值顺序，避免副作用顺序漂移
        EvaTaskDetailInfoCO evaTaskDetailInfoCO = new EvaTaskDetailInfoCO();
        evaTaskDetailInfoCO.setId(evaTaskEntity.getId());
        evaTaskDetailInfoCO.setStatus(evaTaskEntity.getStatus());
        evaTaskDetailInfoCO.setEvaTeacherName(evaTaskEntity.getTeacher().getName());
        evaTaskDetailInfoCO.setTeacherName(evaTaskEntity.getCourInf().getCourseEntity().getTeacher().getName());
        evaTaskDetailInfoCO.setCourseName(evaTaskEntity.getCourInf().getCourseEntity().getSubjectEntity().getName());
        evaTaskDetailInfoCO.setCreateTime(evaTaskEntity.getCreateTime());
        evaTaskDetailInfoCO.setUpdateTime(evaTaskEntity.getUpdateTime());
        evaTaskDetailInfoCO.setCourseTime(toCourseTime(evaTaskEntity.getCourInf()));
        evaTaskDetailInfoCO.setLocation(evaTaskEntity.getCourInf().getLocation());
        return evaTaskDetailInfoCO;
    }

    private static CourseTime toCourseTime(SingleCourseEntity singleCourseEntity) {
        if (singleCourseEntity == null) {
            return null;
        }

        CourseTime courseTime = new CourseTime();
        courseTime.setWeek(singleCourseEntity.getWeek());
        courseTime.setDay(singleCourseEntity.getDay());
        courseTime.setStartTime(singleCourseEntity.getStartTime());
        courseTime.setEndTime(singleCourseEntity.getEndTime());
        return courseTime;
    }
}
