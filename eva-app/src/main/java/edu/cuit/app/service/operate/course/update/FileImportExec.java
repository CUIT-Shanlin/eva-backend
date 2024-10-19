package edu.cuit.app.service.operate.course.update;

import edu.cuit.client.bo.CourseExcelBO;
import edu.cuit.client.dto.clientobject.SemesterCO;
import edu.cuit.client.dto.clientobject.course.SubjectCO;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FileImportExec {
    //科目集合

      public static final Map<String, List<CourseExcelBO>> courseExce = new HashMap<>();

    public static void importCourse(List<CourseExcelBO> list){
        //根据CourseExcelBO中的课程名称进行分类
        for (CourseExcelBO entity : list) {
            String courseName = entity.getCourseName();
            courseExce.computeIfAbsent(courseName, k -> new ArrayList<>()).add(entity);

        }
    }


}
