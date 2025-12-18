package edu.cuit.infra.course;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.infra.gateway.impl.course.support.CourseTemplateLockChecker;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CourseTemplateLockCheckerTest {

    @Test
    void assertNotLocked_whenSnapshotExists_shouldThrow() {
        CourOneEvaTemplateMapper snapshotMapper = mock(CourOneEvaTemplateMapper.class);
        CourInfMapper courInfMapper = mock(CourInfMapper.class);
        EvaTaskMapper taskMapper = mock(EvaTaskMapper.class);
        FormRecordMapper recordMapper = mock(FormRecordMapper.class);

        when(snapshotMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        CourseTemplateLockChecker checker = new CourseTemplateLockChecker(
                snapshotMapper, courInfMapper, taskMapper, recordMapper
        );

        assertThrows(UpdateException.class, () -> checker.assertNotLocked(1, 1));
        verifyNoInteractions(courInfMapper, taskMapper, recordMapper);
    }

    @Test
    void assertNotLocked_whenNoSnapshotButHasRecord_shouldThrow() {
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

        CourseTemplateLockChecker checker = new CourseTemplateLockChecker(
                snapshotMapper, courInfMapper, taskMapper, recordMapper
        );

        assertThrows(UpdateException.class, () -> checker.assertNotLocked(1, 1));
    }

    @Test
    void assertNotLocked_whenNoSnapshotAndNoRecord_shouldPass() {
        CourOneEvaTemplateMapper snapshotMapper = mock(CourOneEvaTemplateMapper.class);
        CourInfMapper courInfMapper = mock(CourInfMapper.class);
        EvaTaskMapper taskMapper = mock(EvaTaskMapper.class);
        FormRecordMapper recordMapper = mock(FormRecordMapper.class);

        when(snapshotMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(courInfMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of());

        CourseTemplateLockChecker checker = new CourseTemplateLockChecker(
                snapshotMapper, courInfMapper, taskMapper, recordMapper
        );

        assertDoesNotThrow(() -> checker.assertNotLocked(1, 1));
        verifyNoInteractions(taskMapper, recordMapper);
    }
}
