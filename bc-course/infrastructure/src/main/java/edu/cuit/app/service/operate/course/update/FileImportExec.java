package edu.cuit.app.service.operate.course.update;

import edu.cuit.client.bo.CourseExcelBO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileImportExec {
    //科目集合

    public static Map<String, List<CourseExcelBO>> importCourse(List<CourseExcelBO> list){
        Map<String, List<CourseExcelBO>> courseExce = new HashMap<>();
        //根据CourseExcelBO中的课程名称进行分类
        for (CourseExcelBO entity : list) {
            String courseName = entity.getCourseName();
            courseExce.computeIfAbsent(courseName, k -> new ArrayList<>()).add(entity);
        }
        return courseExce;
    }


}
