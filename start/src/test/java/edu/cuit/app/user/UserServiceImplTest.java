package edu.cuit.app.user;

import edu.cuit.app.AvatarManager;
import edu.cuit.app.convertor.PaginationBizConvertor;
import edu.cuit.app.convertor.user.RoleBizConvertor;
import edu.cuit.app.convertor.user.UserBizConvertor;
import edu.cuit.app.service.impl.user.UserServiceImpl;
import edu.cuit.bc.evaluation.application.port.EvaRecordCountQueryPort;
import edu.cuit.client.api.ISemesterService;
import edu.cuit.client.api.course.ICourseDetailService;
import edu.cuit.client.api.course.IUserCourseService;
import edu.cuit.client.api.eva.IEvaTaskService;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.eva.CourseScoreCO;
import edu.cuit.client.dto.clientobject.eva.UserSingleCourseScoreCO;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.domain.gateway.user.LdapPersonGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.domain.gateway.user.UserUpdateGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserQueryGateway userQueryGateway;
    @Mock
    private UserUpdateGateway userUpdateGateway;
    @Mock
    private LdapPersonGateway ldapPersonGateway;
    @Mock
    private CourseQueryGateway courseQueryGateway;
    @Mock
    private EvaRecordCountQueryPort evaRecordCountQueryPort;
    @Mock
    private ISemesterService semesterService;
    @Mock
    private ICourseDetailService courseDetailService;
    @Mock
    private IUserCourseService userCourseService;
    @Mock
    private IEvaTaskService evaTaskService;
    @Mock
    private AvatarManager avatarManager;
    @Mock
    private UserBizConvertor userBizConvertor;
    @Mock
    private RoleBizConvertor roleBizConvertor;
    @Mock
    private PaginationBizConvertor paginationBizConvertor;

    @InjectMocks
    private UserServiceImpl service;

    @Test
    void getOneUserScore_shouldFillEvaNumFromCountPort_andComputeAverageScore() {
        int userId = 100;
        int semId = 1;

        SelfTeachCourseCO course = new SelfTeachCourseCO();
        course.setId(10);
        course.setName("课程A");

        CourseScoreCO score1 = new CourseScoreCO();
        score1.setAverScore(80.0);
        CourseScoreCO score2 = new CourseScoreCO();
        score2.setAverScore(90.0);

        when(userQueryGateway.findUsernameById(userId)).thenReturn(Optional.of("u1"));
        when(courseQueryGateway.getSelfCourseInfo("u1", semId)).thenReturn(List.of(course));
        when(courseQueryGateway.findEvaScore(10)).thenReturn(List.of(score1, score2));
        when(evaRecordCountQueryPort.getEvaNumByCourse(10)).thenReturn(Optional.of(5));

        List<UserSingleCourseScoreCO> result = service.getOneUserScore(userId, semId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10, result.get(0).getCourseId());
        assertEquals("课程A", result.get(0).getCourseName());
        assertEquals(85.0, result.get(0).getScore());
        assertEquals(5, result.get(0).getEvaNum());
    }

    @Test
    void getOneUserScore_whenEvaScoreEmpty_shouldReturnZeroScoreAndZeroEvaNum() {
        int userId = 100;
        int semId = 1;

        SelfTeachCourseCO course = new SelfTeachCourseCO();
        course.setId(10);
        course.setName("课程A");

        when(userQueryGateway.findUsernameById(userId)).thenReturn(Optional.of("u1"));
        when(courseQueryGateway.getSelfCourseInfo("u1", semId)).thenReturn(List.of(course));
        when(courseQueryGateway.findEvaScore(10)).thenReturn(List.of());

        List<UserSingleCourseScoreCO> result = service.getOneUserScore(userId, semId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(0.0, result.get(0).getScore());
        assertEquals(0, result.get(0).getEvaNum());
    }
}

