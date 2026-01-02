package edu.cuit.bc.evaluation.application.usecase;

import edu.cuit.bc.evaluation.application.port.EvaRecordScoreQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaRecordUserLogQueryPort;
import edu.cuit.client.dto.clientobject.eva.EvaRecordCO;
import edu.cuit.client.dto.data.course.CourseTime;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.entity.eva.EvaRecordEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 用户评教记录读侧查询用例（QueryUseCase）。
 *
 * <p>当前阶段仅做“用例归位 + 旧入口委托壳化”，不改变任何业务语义；
 * 旧入口仍保留 {@code @CheckSemId} 与当前用户解析等触发点。</p>
 */
public class UserEvaQueryUseCase {
    private final EvaRecordUserLogQueryPort evaRecordUserLogQueryPort;
    private final EvaRecordScoreQueryPort evaRecordScoreQueryPort;

    public UserEvaQueryUseCase(
            EvaRecordUserLogQueryPort evaRecordUserLogQueryPort,
            EvaRecordScoreQueryPort evaRecordScoreQueryPort
    ) {
        this.evaRecordUserLogQueryPort = Objects.requireNonNull(evaRecordUserLogQueryPort, "evaRecordUserLogQueryPort");
        this.evaRecordScoreQueryPort = Objects.requireNonNull(evaRecordScoreQueryPort, "evaRecordScoreQueryPort");
    }

    public List<EvaRecordCO> getEvaLogInfo(Integer userId, Integer semId, String keyword) {
        List<EvaRecordCO> evaRecordCOS = new ArrayList<>();
        List<EvaRecordEntity> evaRecordEntities = evaRecordUserLogQueryPort.getEvaLogInfo(userId, semId, keyword);
        if (evaRecordEntities.isEmpty()) {
            List list = new ArrayList();
            return list;
        }
        for (EvaRecordEntity evaRecordEntity : evaRecordEntities) {
            EvaRecordCO evaRecordCO = toEvaRecordCO(evaRecordEntity);
            evaRecordCO.setAverScore(evaRecordScoreQueryPort.getScoreFromRecord(evaRecordEntity.getFormPropsValues()).orElse(0.0));
            evaRecordCOS.add(evaRecordCO);
        }
        return evaRecordCOS;
    }

    public List<EvaRecordCO> getEvaLoggingInfo(Integer userId, Integer courseId, Integer semId) {
        List<EvaRecordEntity> evaRecordEntities = evaRecordUserLogQueryPort.getEvaEdLogInfo(userId, semId, courseId);
        List<EvaRecordCO> evaRecordCOS = new ArrayList<>();
        if (evaRecordEntities.isEmpty()) {
            List list = new ArrayList();
            return list;
        }
        for (EvaRecordEntity evaRecordEntity : evaRecordEntities) {
            EvaRecordCO evaRecordCO = toEvaRecordCO(evaRecordEntity);
            evaRecordCO.setAverScore(evaRecordScoreQueryPort.getScoreFromRecord(evaRecordEntity.getFormPropsValues()).orElse(0.0));
            evaRecordCOS.add(evaRecordCO);
        }
        return evaRecordCOS;
    }

    private static EvaRecordCO toEvaRecordCO(EvaRecordEntity evaRecordEntity) {
        // 重要：保持与历史 MapStruct 生成实现一致的赋值与求值顺序，避免副作用顺序漂移
        EvaRecordCO evaRecordCO = new EvaRecordCO();
        evaRecordCO.setId(evaRecordEntity.getId());
        evaRecordCO.setTeacherName(evaRecordEntity.getTask().getCourInf().getCourseEntity().getTeacher().getName());
        evaRecordCO.setEvaTeacherName(evaRecordEntity.getTask().getTeacher().getName());
        evaRecordCO.setCourseName(evaRecordEntity.getTask().getCourInf().getCourseEntity().getSubjectEntity().getName());
        evaRecordCO.setTextValue(evaRecordEntity.getTextValue());
        evaRecordCO.setFormPropsValues(evaRecordEntity.getFormPropsValues());
        evaRecordCO.setCreateTime(evaRecordEntity.getCreateTime());
        evaRecordCO.setCourseTime(toCourseTime(evaRecordEntity.getTask().getCourInf()));
        evaRecordCO.setAverScore(null);
        return evaRecordCO;
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
