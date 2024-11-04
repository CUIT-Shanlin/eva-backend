package edu.cuit.infra.enums.cache;

import org.springframework.stereotype.Component;

/**
 * 课程缓存键常量
 */
@Component("courseCacheConstants")
public class CourseCacheConstants {

    public final String COURSE_TYPE_LIST = "course.list.type";
    public final String COURSE_LIST_BY_SEM = "course.list.sem.";
    public final String SUBJECT_LIST = "subject.list";
}
