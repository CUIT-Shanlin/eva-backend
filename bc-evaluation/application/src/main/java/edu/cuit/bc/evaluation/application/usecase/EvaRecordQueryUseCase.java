package edu.cuit.bc.evaluation.application.usecase;

import com.alibaba.cola.exception.SysException;
import edu.cuit.bc.evaluation.application.port.EvaRecordPagingQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaRecordScoreQueryPort;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.eva.EvaRecordCO;
import edu.cuit.client.dto.data.course.CourseTime;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaLogConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.entity.eva.EvaRecordEntity;

import java.util.List;
import java.util.Objects;

/**
 * 评教记录读侧查询用例（QueryUseCase）。
 *
 * <p>当前阶段仅做“编排逻辑归位 + 旧入口委托壳化”，不改变任何业务语义；
 * 关键约束：异常文案/副作用顺序/循环顺序保持完全一致。</p>
 */
public class EvaRecordQueryUseCase {
    private final EvaRecordPagingQueryPort evaRecordPagingQueryPort;
    private final EvaRecordScoreQueryPort evaRecordScoreQueryPort;

    public EvaRecordQueryUseCase(
            EvaRecordPagingQueryPort evaRecordPagingQueryPort,
            EvaRecordScoreQueryPort evaRecordScoreQueryPort
    ) {
        this.evaRecordPagingQueryPort = Objects.requireNonNull(evaRecordPagingQueryPort, "evaRecordPagingQueryPort");
        this.evaRecordScoreQueryPort = Objects.requireNonNull(evaRecordScoreQueryPort, "evaRecordScoreQueryPort");
    }

    public PaginationQueryResultCO<EvaRecordCO> pageEvaRecordAsPaginationQueryResult(
            Integer semId,
            PagingQuery<EvaLogConditionalQuery> query
    ) {
        PaginationResultEntity<EvaRecordEntity> page = evaRecordPagingQueryPort.pageEvaRecord(semId, query);

        List<EvaRecordCO> results = page.getRecords().stream()
                .map(this::toEvaRecordCO)
                .toList();

        for (int i = 0; i < results.size(); i++) {
            results.get(i).setAverScore(
                    evaRecordScoreQueryPort.getScoreFromRecord(page.getRecords().get(i).getFormPropsValues())
                            .orElseThrow(() -> new SysException("相关模板不存在"))
            );
        }

        PaginationQueryResultCO<EvaRecordCO> pageCO = new PaginationQueryResultCO<>();
        pageCO.setCurrent(page.getCurrent())
                .setSize(page.getSize())
                .setTotal(page.getTotal())
                .setRecords(results);
        return pageCO;
    }

    private EvaRecordCO toEvaRecordCO(EvaRecordEntity evaRecordEntity) {
        SingleCourseEntity courseInfo = evaRecordEntity.getTask().getCourInf();

        CourseTime courseTime = new CourseTime()
                .setWeek(courseInfo.getWeek())
                .setDay(courseInfo.getDay())
                .setStartTime(courseInfo.getStartTime())
                .setEndTime(courseInfo.getEndTime());

        return new EvaRecordCO()
                .setId(evaRecordEntity.getId())
                .setTeacherName(courseInfo.getCourseEntity().getTeacher().getName())
                .setEvaTeacherName(evaRecordEntity.getTask().getTeacher().getName())
                .setCourseName(courseInfo.getCourseEntity().getSubjectEntity().getName())
                .setTextValue(evaRecordEntity.getTextValue())
                .setFormPropsValues(evaRecordEntity.getFormPropsValues())
                .setCreateTime(evaRecordEntity.getCreateTime())
                .setCourseTime(courseTime)
                .setAverScore(null);
    }
}
