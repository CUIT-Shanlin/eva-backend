package edu.cuit.app.eva;

import com.alibaba.cola.exception.SysException;
import edu.cuit.app.service.impl.eva.EvaRecordServiceImpl;
import edu.cuit.bc.evaluation.application.port.EvaRecordPagingQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaRecordScoreQueryPort;
import edu.cuit.bc.evaluation.application.usecase.EvaRecordQueryUseCase;
import edu.cuit.bc.evaluation.application.usecase.DeleteEvaRecordUseCase;
import edu.cuit.bc.evaluation.application.usecase.SubmitEvaluationUseCase;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.eva.EvaRecordCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaLogConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.course.CourseEntity;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.entity.course.SubjectEntity;
import edu.cuit.domain.entity.eva.EvaRecordEntity;
import edu.cuit.domain.entity.eva.EvaTaskEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.domain.gateway.user.MenuQueryGateway;
import edu.cuit.domain.gateway.user.UserUpdateGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaRecordServiceImplTest {

    @Mock
    private EvaRecordPagingQueryPort evaRecordPagingQueryPort;

    @Mock
    private EvaRecordScoreQueryPort evaRecordScoreQueryPort;

    @Mock
    private SubmitEvaluationUseCase submitEvaluationUseCase;

    @Mock
    private DeleteEvaRecordUseCase deleteEvaRecordUseCase;

    private EvaRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        EvaRecordQueryUseCase evaRecordQueryUseCase = new EvaRecordQueryUseCase(evaRecordPagingQueryPort, evaRecordScoreQueryPort);
        service = new EvaRecordServiceImpl(evaRecordQueryUseCase, submitEvaluationUseCase, deleteEvaRecordUseCase);
    }

    @Test
    void pageEvaRecord_shouldFillAverageScoreFromRecord() {
        PagingQuery<EvaLogConditionalQuery> query = new PagingQuery<>();
        query.setPage(1);
        query.setSize(10);

        EvaRecordEntity record1 = newEvaRecordEntity(1, "props-1");
        EvaRecordEntity record2 = newEvaRecordEntity(2, "props-2");

        PaginationResultEntity<EvaRecordEntity> page = new PaginationResultEntity<EvaRecordEntity>()
                .setRecords(List.of(record1, record2))
                .setCurrent(1)
                .setSize(10)
                .setTotal(2);

        when(evaRecordPagingQueryPort.pageEvaRecord(1, query)).thenReturn(page);
        when(evaRecordScoreQueryPort.getScoreFromRecord("props-1")).thenReturn(Optional.of(80.0));
        when(evaRecordScoreQueryPort.getScoreFromRecord("props-2")).thenReturn(Optional.of(90.0));

        PaginationQueryResultCO<EvaRecordCO> result = service.pageEvaRecord(1, query);

        assertNotNull(result);
        assertEquals(2, result.getRecords().size());
        assertEquals(80.0, result.getRecords().get(0).getAverScore());
        assertEquals(90.0, result.getRecords().get(1).getAverScore());
        verify(evaRecordScoreQueryPort).getScoreFromRecord("props-1");
        verify(evaRecordScoreQueryPort).getScoreFromRecord("props-2");
    }

    @Test
    void pageEvaRecord_whenScoreMissing_shouldThrow() {
        PagingQuery<EvaLogConditionalQuery> query = new PagingQuery<>();
        query.setPage(1);
        query.setSize(10);

        EvaRecordEntity record = newEvaRecordEntity(1, "props-1");

        PaginationResultEntity<EvaRecordEntity> page = new PaginationResultEntity<EvaRecordEntity>()
                .setRecords(List.of(record))
                .setCurrent(1)
                .setSize(10)
                .setTotal(1);

        when(evaRecordPagingQueryPort.pageEvaRecord(1, query)).thenReturn(page);
        when(evaRecordScoreQueryPort.getScoreFromRecord("props-1")).thenReturn(Optional.empty());

        SysException ex = assertThrows(SysException.class, () -> service.pageEvaRecord(1, query));
        assertEquals("相关模板不存在", ex.getMessage());
    }

    private static EvaRecordEntity newEvaRecordEntity(Integer id, String formPropsValues) {
        UserEntity teachingTeacher = newUserEntity("教学老师");
        SubjectEntity subject = new SubjectEntity();
        subject.setName("课程名称");

        CourseEntity courseEntity = new CourseEntity();
        courseEntity.setTeacher(() -> teachingTeacher);
        courseEntity.setSubject(() -> subject);

        SingleCourseEntity singleCourseEntity = new SingleCourseEntity();
        singleCourseEntity.setWeek(1);
        singleCourseEntity.setDay(1);
        singleCourseEntity.setStartTime(1);
        singleCourseEntity.setEndTime(2);
        singleCourseEntity.setCourse(() -> courseEntity);

        UserEntity evaTeacher = newUserEntity("评教老师");
        EvaTaskEntity task = new EvaTaskEntity();
        task.setTeacher(() -> evaTeacher);
        task.setCourInf(() -> singleCourseEntity);

        EvaRecordEntity record = new EvaRecordEntity();
        record.setId(id);
        record.setFormPropsValues(formPropsValues);
        record.setTask(() -> task);
        return record;
    }

    private static UserEntity newUserEntity(String name) {
        UserEntity userEntity = new UserEntity(mock(UserUpdateGateway.class), mock(MenuQueryGateway.class));
        userEntity.setName(name);
        return userEntity;
    }
}
