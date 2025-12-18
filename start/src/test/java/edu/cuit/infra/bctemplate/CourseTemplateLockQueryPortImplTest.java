package edu.cuit.infra.bctemplate;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.infra.bctemplate.adapter.CourseTemplateLockQueryPortImpl;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CourseTemplateLockQueryPortImplTest {

    @Test
    void isLocked_whenCourseIdNull_shouldReturnFalseAndNoInteractions() {
        CourOneEvaTemplateMapper snapshotMapper = mock(CourOneEvaTemplateMapper.class);
        CourInfMapper courInfMapper = mock(CourInfMapper.class);
        EvaTaskMapper taskMapper = mock(EvaTaskMapper.class);
        FormRecordMapper recordMapper = mock(FormRecordMapper.class);

        CourseTemplateLockQueryPortImpl port = new CourseTemplateLockQueryPortImpl(
                snapshotMapper, courInfMapper, taskMapper, recordMapper
        );

        assertFalse(port.isLocked(null, 1));
        verifyNoInteractions(snapshotMapper, courInfMapper, taskMapper, recordMapper);
    }

    @Test
    void isLocked_whenSnapshotExists_shouldReturnTrueAndSkipFallbackQuery() {
        CourOneEvaTemplateMapper snapshotMapper = mock(CourOneEvaTemplateMapper.class);
        CourInfMapper courInfMapper = mock(CourInfMapper.class);
        EvaTaskMapper taskMapper = mock(EvaTaskMapper.class);
        FormRecordMapper recordMapper = mock(FormRecordMapper.class);

        when(snapshotMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        CourseTemplateLockQueryPortImpl port = new CourseTemplateLockQueryPortImpl(
                snapshotMapper, courInfMapper, taskMapper, recordMapper
        );

        assertTrue(port.isLocked(1, 1));
        verifyNoInteractions(courInfMapper, taskMapper, recordMapper);
    }

    @Test
    void isLocked_whenNoSnapshotButHasRecord_shouldReturnTrue() {
        CourOneEvaTemplateMapper snapshotMapper = mock(CourOneEvaTemplateMapper.class);
        CourInfMapper courInfMapper = mock(CourInfMapper.class);
        EvaTaskMapper taskMapper = mock(EvaTaskMapper.class);
        FormRecordMapper recordMapper = mock(FormRecordMapper.class);

        when(snapshotMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        CourInfDO courInf = new CourInfDO();
        courInf.setId(10);
        when(courInfMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(courInf));

        EvaTaskDO task = new EvaTaskDO();
        task.setId(20);
        when(taskMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(task));
        when(recordMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        CourseTemplateLockQueryPortImpl port = new CourseTemplateLockQueryPortImpl(
                snapshotMapper, courInfMapper, taskMapper, recordMapper
        );

        assertTrue(port.isLocked(1, 1));
    }

    @Test
    void isLocked_whenNoSnapshotAndNoRecord_shouldReturnFalse() {
        CourOneEvaTemplateMapper snapshotMapper = mock(CourOneEvaTemplateMapper.class);
        CourInfMapper courInfMapper = mock(CourInfMapper.class);
        EvaTaskMapper taskMapper = mock(EvaTaskMapper.class);
        FormRecordMapper recordMapper = mock(FormRecordMapper.class);

        when(snapshotMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(courInfMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of());

        CourseTemplateLockQueryPortImpl port = new CourseTemplateLockQueryPortImpl(
                snapshotMapper, courInfMapper, taskMapper, recordMapper
        );

        assertFalse(port.isLocked(1, 1));
        verifyNoInteractions(taskMapper, recordMapper);
    }
}

