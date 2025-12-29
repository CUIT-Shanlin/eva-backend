package edu.cuit.app.poi.course.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import edu.cuit.client.bo.CourseExcelBO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * excel课程工具类
 */
public class ExcelCourseUtils {

    /**
     * 合并多门课程
     * @param _courses 课程列表
     * @return 合并后的课程列表
     */
    public static List<CourseExcelBO> mergeMultipleCourses(List<CourseExcelBO> _courses) {
        if (_courses == null || _courses.size() <= 1) {
            return _courses == null ? null : new ArrayList<>(_courses);
        }
        List<CourseExcelBO> course = new ArrayList<>(new ArrayList<>(_courses).stream()
                .sorted(Comparator.comparingInt(CourseExcelBO::getStartTime))
                .toList());

        for (int i = 0;i < course.size();i ++) {
            for (int j = 0;j < course.size();j++) {
                if (i == j) continue;
                List<CourseExcelBO> mergedCourse = mergeTwoCourse(course.get(i), course.get(j));
                if (!mergedCourse.isEmpty()) {
                    course.remove(i);
                    course.remove(i > j ? j : j-1);
                    course.addAll(mergedCourse);
                    i = 0;
                    break;
                }
            }
        }

        return course;

    }

    /**
     * 合并两节课的结束，需要先自行判断相邻，合并到course1
     * @param course1 课程1
     * @param course2 课程2
     */
    public static List<CourseExcelBO> mergeTwoCourse(CourseExcelBO course1,CourseExcelBO course2) {

        if (!course1.isAdjoin(course2)) return List.of();

        // 获取交集
        List<Integer> intersectionWeeks = (List<Integer>) CollectionUtil.intersection(course1.getWeeks(), course2.getWeeks());
        if (intersectionWeeks.isEmpty()) return List.of();

        List<CourseExcelBO> results = new ArrayList<>();

        List<Integer> course1Weeks = course1.getWeeks();
        List<Integer> course2Weeks = course2.getWeeks();

        // 计算补集
        course1Weeks.removeAll(intersectionWeeks);
        course2Weeks.removeAll(intersectionWeeks);

        CourseExcelBO intersectedCourse = ObjectUtil.clone(course1);
        intersectedCourse.setWeeks(intersectionWeeks);
        if (course1.getStartTime() > course2.getEndTime()) {
            intersectedCourse
                    .setStartTime(course2.getStartTime())
                    .setEndTime(course1.getEndTime());
        } else {
            intersectedCourse
                    .setStartTime(course1.getStartTime())
                    .setEndTime(course2.getEndTime());
        }

        if (!course1Weeks.isEmpty()) results.add(ObjectUtil.clone(course1).setWeeks(course1Weeks));
        results.add(intersectedCourse);
        if (!course2Weeks.isEmpty()) results.add(ObjectUtil.clone(course2).setWeeks(course2Weeks));

        return results;

    }
}
