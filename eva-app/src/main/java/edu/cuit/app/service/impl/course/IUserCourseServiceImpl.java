package edu.cuit.app.service.impl.course;

import edu.cuit.client.api.course.IUserCourseService;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.course.CourseDetailCO;
import edu.cuit.client.dto.clientobject.course.RecommendCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;
import edu.cuit.client.dto.data.Term;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
@Service
@RequiredArgsConstructor
public class IUserCourseServiceImpl implements IUserCourseService {
    @Override
    public List<SimpleResultCO> getUserCourseInfo(Integer id, Integer semId) {
        return null;
    }

    @Override
    public List<CourseDetailCO> getUserCourseDetail(Integer id, Integer semId) {
        return null;
    }

    @Override
    public List<RecommendCourseCO> getSelfCourse(Integer semId) {
        return null;
    }

    @Override
    public void importCourse(InputStream fileStream, Integer type, Term term) {

    }

    @Override
    public List<Void> selfCourseDetail(Integer semId) {
        return null;
    }

    @Override
    public List<SelfTeachCourseTimeCO> selfCourseTime(Integer courseId) {
        return null;
    }

    @Override
    public Void deleteSelfCourse(Integer courseId) {
        return null;
    }

    @Override
    public Void updateSelfCourse(SelfTeachCourseCO selfTeachCourseCO, List<SelfTeachCourseTimeCO> timeList) {
        return null;
    }
}
