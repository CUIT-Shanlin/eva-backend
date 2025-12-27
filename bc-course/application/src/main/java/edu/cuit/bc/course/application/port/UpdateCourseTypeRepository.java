package edu.cuit.bc.course.application.port;

import edu.cuit.bc.course.application.model.UpdateCourseTypeCommand;

/**
 * 修改一个课程类型的持久化端口。
 */
public interface UpdateCourseTypeRepository {
    void update(UpdateCourseTypeCommand command);
}

