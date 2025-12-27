package edu.cuit.bc.course.application.port;

import edu.cuit.bc.course.application.model.UpdateCoursesTypeCommand;

/**
 * 批量修改课程对应类型的持久化端口。
 */
public interface UpdateCoursesTypeRepository {
    void update(UpdateCoursesTypeCommand command);
}

