package edu.cuit.app.service.course;

import com.alibaba.cola.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.dev33.satoken.stp.StpUtil;
import edu.cuit.app.convertor.course.CourseBizConvertor;
import edu.cuit.app.service.impl.MsgServiceImpl;
import edu.cuit.app.service.impl.course.IUserCourseServiceImpl;
import edu.cuit.app.service.operate.course.MsgResult;
import edu.cuit.app.service.operate.course.query.UserCourseDetailQueryExec;
import edu.cuit.client.bo.CourseExcelBO;
import edu.cuit.client.dto.clientobject.SemesterCO;
import edu.cuit.domain.gateway.course.CourseDeleteGateway;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.domain.gateway.course.CourseUpdateGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IUserCourseServiceImportCourseTest {

    private static final String SEMESTER_JSON = """
            {"period":0,"startYear":"2025","endYear":"2026"}
            """;

    @Mock
    private CourseQueryGateway courseQueryGateway;
    @Mock
    private CourseUpdateGateway courseUpdateGateway;
    @Mock
    private CourseDeleteGateway courseDeleteGateway;
    @Mock
    private CourseBizConvertor courseBizConvertor;
    @Mock
    private UserCourseDetailQueryExec userCourseDetailQueryExec;
    @Mock
    private MsgServiceImpl msgService;
    @Mock
    private UserQueryGateway userQueryGateway;
    @Mock
    private MsgResult msgResult;

    private IUserCourseServiceImpl userCourseService;

    @BeforeEach
    void setUp() {
        userCourseService = new IUserCourseServiceImpl(
                courseQueryGateway,
                courseUpdateGateway,
                courseDeleteGateway,
                courseBizConvertor,
                userCourseDetailQueryExec,
                msgService,
                userQueryGateway,
                msgResult,
                new ObjectMapper()
        );
    }

    @Test
    void shouldImportTheoryCourseFromCsv() {
        String csv = """
                course_name,teacher_name,prof_title,start_time,end_time,day,weeks,classroom,course_class
                高等数学,王老师,教授,1,2,1,1-16周,一教101,信安2023级2班
                """;
        when(courseUpdateGateway.importCourseFile(anyMap(), any(SemesterCO.class), anyInt()))
                .thenReturn(Collections.emptyMap());
        when(userQueryGateway.findIdByUsername("teacher1")).thenReturn(java.util.Optional.of(1));

        try (MockedStatic<StpUtil> mockedStpUtil = mockStatic(StpUtil.class)) {
            mockedStpUtil.when(StpUtil::getLoginId).thenReturn("teacher1");

            userCourseService.importCourse(
                    new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)),
                    0,
                    SEMESTER_JSON
            );
        }

        ArgumentCaptor<Map<String, List<CourseExcelBO>>> courseCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<SemesterCO> semesterCaptor = ArgumentCaptor.forClass(SemesterCO.class);
        verify(courseUpdateGateway).importCourseFile(courseCaptor.capture(), semesterCaptor.capture(), anyInt());

        Map<String, List<CourseExcelBO>> importedCourses = courseCaptor.getValue();
        assertEquals(1, importedCourses.size());
        assertEquals(1, importedCourses.get("高等数学").size());
        CourseExcelBO importedCourse = importedCourses.get("高等数学").get(0);
        assertEquals("王老师", importedCourse.getTeacherName());
        assertEquals(1, importedCourse.getStartTime());
        assertEquals(2, importedCourse.getEndTime());
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16), importedCourse.getWeeks());
        assertEquals(0, semesterCaptor.getValue().getPeriod());
        assertEquals("2025", semesterCaptor.getValue().getStartYear());
    }

    @Test
    void shouldImportExperimentalCourseFromCsv() {
        String csv = """
                course_name,teacher_name,prof_title,start_time,end_time,day,weeks,classroom,course_class
                数字电路实验,李老师,,5,6,3,1-16周,实验楼201,计科2023级1班
                """;
        when(courseUpdateGateway.importCourseFile(anyMap(), any(SemesterCO.class), anyInt()))
                .thenReturn(Collections.emptyMap());
        when(userQueryGateway.findIdByUsername("teacher1")).thenReturn(java.util.Optional.of(1));

        try (MockedStatic<StpUtil> mockedStpUtil = mockStatic(StpUtil.class)) {
            mockedStpUtil.when(StpUtil::getLoginId).thenReturn("teacher1");

            userCourseService.importCourse(
                    new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)),
                    1,
                    SEMESTER_JSON
            );
        }

        ArgumentCaptor<Map<String, List<CourseExcelBO>>> courseCaptor = ArgumentCaptor.forClass(Map.class);
        verify(courseUpdateGateway).importCourseFile(courseCaptor.capture(), any(SemesterCO.class), anyInt());

        Map<String, List<CourseExcelBO>> importedCourses = courseCaptor.getValue();
        assertEquals(1, importedCourses.size());
        assertEquals(1, importedCourses.get("数字电路实验").size());
        CourseExcelBO importedCourse = importedCourses.get("数字电路实验").get(0);
        assertEquals("李老师", importedCourse.getTeacherName());
        assertEquals(5, importedCourse.getStartTime());
        assertEquals(6, importedCourse.getEndTime());
        assertEquals(3, importedCourse.getDay());
    }

    @Test
    void shouldRejectUnsupportedCourseType() {
        String csv = """
                course_name,teacher_name,prof_title,start_time,end_time,day,weeks,classroom,course_class
                高等数学,王老师,教授,1,2,1,1-16周,一教101,信安2023级2班
                """;

        BizException exception = assertThrows(BizException.class, () -> userCourseService.importCourse(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)),
                2,
                SEMESTER_JSON
        ));

        assertEquals("课表类型转换错误", exception.getMessage());
    }
}
