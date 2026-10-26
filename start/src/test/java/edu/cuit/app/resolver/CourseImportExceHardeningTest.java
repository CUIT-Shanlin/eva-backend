package edu.cuit.app.resolver;

import edu.cuit.client.bo.CourseExcelBO;
import edu.cuit.infra.convertor.course.CourseConvertor;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseTypeDO;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormTemplateDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseTypeCourseMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseTypeMapper;
import edu.cuit.infra.dal.database.mapper.course.SubjectMapper;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormTemplateMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.enums.cache.CourseCacheConstants;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.infra.gateway.impl.course.operate.CourseImportExce;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 覆盖课表导入时历史脏数据（重复科目/教师/课程、孤儿科目）导致的 500。
 */
class CourseImportExceHardeningTest {

    private CourseConvertor courseConvertor;
    private CourInfMapper courInfMapper;
    private CourseMapper courseMapper;
    private CourseTypeCourseMapper courseTypeCourseMapper;
    private CourseTypeMapper courseTypeMapper;
    private SubjectMapper subjectMapper;
    private EvaTaskMapper evaTaskMapper;
    private FormRecordMapper recordMapper;
    private CourOneEvaTemplateMapper courOneEvaTemplateMapper;
    private SysUserMapper userMapper;
    private FormTemplateMapper formTemplateMapper;
    private LocalCacheManager cacheManager;
    private CourseCacheConstants courseCacheConstants;
    private EvaCacheConstants evaCacheConstants;
    private CourseImportExce importExce;

    @BeforeEach
    void setUp() {
        courseConvertor = mock(CourseConvertor.class);
        courInfMapper = mock(CourInfMapper.class);
        courseMapper = mock(CourseMapper.class);
        courseTypeCourseMapper = mock(CourseTypeCourseMapper.class);
        courseTypeMapper = mock(CourseTypeMapper.class);
        subjectMapper = mock(SubjectMapper.class);
        evaTaskMapper = mock(EvaTaskMapper.class);
        recordMapper = mock(FormRecordMapper.class);
        courOneEvaTemplateMapper = mock(CourOneEvaTemplateMapper.class);
        userMapper = mock(SysUserMapper.class);
        formTemplateMapper = mock(FormTemplateMapper.class);
        cacheManager = mock(LocalCacheManager.class);
        courseCacheConstants = mock(CourseCacheConstants.class);
        evaCacheConstants = mock(EvaCacheConstants.class);
        importExce = new CourseImportExce(courseConvertor, courInfMapper, courseMapper, courseTypeCourseMapper,
                courseTypeMapper, subjectMapper, evaTaskMapper, recordMapper, courOneEvaTemplateMapper,
                userMapper, formTemplateMapper, cacheManager, courseCacheConstants, evaCacheConstants);
    }

    @Test
    void addAllShouldTolerateDuplicateSubjectTeacherAndCourse() {
        SubjectDO firstSubject = new SubjectDO();
        firstSubject.setId(10);
        firstSubject.setName("测试课程");
        firstSubject.setNature(1);
        SubjectDO duplicateSubject = new SubjectDO();
        duplicateSubject.setId(11);
        duplicateSubject.setName("测试课程");
        duplicateSubject.setNature(1);
        when(subjectMapper.selectList(any())).thenReturn(List.of(firstSubject, duplicateSubject));

        SysUserDO firstUser = new SysUserDO().setId(20).setName("张老师");
        SysUserDO duplicateUser = new SysUserDO().setId(21).setName("张老师");
        when(userMapper.selectList(any())).thenReturn(List.of(firstUser, duplicateUser));

        FormTemplateDO template = new FormTemplateDO();
        template.setId(30);
        template.setIsDefault(1);
        when(formTemplateMapper.selectList(any())).thenReturn(List.of(template));

        CourseTypeDO courseType = new CourseTypeDO();
        courseType.setId(40);
        courseType.setIsDefault(1);
        when(courseTypeMapper.selectList(any())).thenReturn(List.of(courseType));

        when(courseMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(courseMapper.insert(any())).thenReturn(1);
        when(courseTypeCourseMapper.insert(any())).thenReturn(1);
        when(courInfMapper.exists(any())).thenReturn(false);
        when(courInfMapper.insert(any())).thenReturn(1);
        when(courseConvertor.toCourInfDO(nullable(Integer.class), nullable(Integer.class),
                any(CourseExcelBO.class), any(LocalDateTime.class))).thenReturn(new CourInfDO());

        assertDoesNotThrow(() -> importExce.addAll(Map.of("测试课程", List.of(course())), 1, 100));

        ArgumentCaptor<CourseDO> courseCaptor = ArgumentCaptor.forClass(CourseDO.class);
        verify(courseMapper).insert(courseCaptor.capture());
        assertEquals(10, courseCaptor.getValue().getSubjectId());
        assertEquals(20, courseCaptor.getValue().getTeacherId());
        verify(courInfMapper).insert(any());
        verify(courseTypeCourseMapper).insert(any());
    }

    @Test
    void addAllShouldFailWithClearMessageWhenDefaultCourseTypeMissing() {
        SubjectDO subject = new SubjectDO();
        subject.setId(10);
        subject.setName("测试课程");
        subject.setNature(1);
        when(subjectMapper.selectList(any())).thenReturn(List.of(subject));
        when(userMapper.selectList(any())).thenReturn(List.of(new SysUserDO().setId(20).setName("张老师")));

        FormTemplateDO template = new FormTemplateDO();
        template.setId(30);
        template.setIsDefault(1);
        when(formTemplateMapper.selectList(any())).thenReturn(List.of(template));
        when(courseTypeMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(courseMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(courseMapper.insert(any())).thenReturn(1);
        when(courInfMapper.exists(any())).thenReturn(false);
        when(courseConvertor.toCourInfDO(nullable(Integer.class), nullable(Integer.class),
                any(CourseExcelBO.class), any(LocalDateTime.class))).thenReturn(new CourInfDO());

        UpdateException exception = assertThrows(UpdateException.class,
                () -> importExce.addAll(Map.of("测试课程", List.of(course())), 1, 100));

        assertEquals("还没有对应默认课程类型", exception.getMessage());
    }

    @Test
    void deleteCourseShouldSkipOrphanSubjectInsteadOfThrowing() {
        CourseDO orphanCourse = new CourseDO();
        orphanCourse.setId(1);
        orphanCourse.setSubjectId(999);
        CourseDO courseWithoutSubject = new CourseDO();
        courseWithoutSubject.setId(2);
        courseWithoutSubject.setSubjectId(null);
        when(courseMapper.selectList(any())).thenReturn(List.of(orphanCourse, courseWithoutSubject));
        when(subjectMapper.selectById(999)).thenReturn(null);

        assertDoesNotThrow(() -> importExce.deleteCourse(100, 1));
        verify(courseMapper, never()).delete(any());
    }

    private CourseExcelBO course() {
        return new CourseExcelBO()
                .setCourseName("测试课程")
                .setTeacherName("张老师")
                .setStartTime(1)
                .setEndTime(2)
                .setDay(1)
                .setWeeks(List.of(1))
                .setClassroom("b512")
                .setCourseClass("测试班");
    }
}
