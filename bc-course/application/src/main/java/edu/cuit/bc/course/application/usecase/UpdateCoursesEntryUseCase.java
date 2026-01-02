package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.ChangeCourseTemplateCommand;
import edu.cuit.client.dto.cmd.course.UpdateCoursesCmd;

import java.util.Objects;

/**
 * 课程写侧：批量修改课程模板入口用例（保持行为不变：不在入口用例层新增校验/异常转换）。
 */
public class UpdateCoursesEntryUseCase {
    private final ChangeCourseTemplateUseCase changeCourseTemplateUseCase;

    public UpdateCoursesEntryUseCase(ChangeCourseTemplateUseCase changeCourseTemplateUseCase) {
        this.changeCourseTemplateUseCase = Objects.requireNonNull(changeCourseTemplateUseCase, "changeCourseTemplateUseCase");
    }

    public void updateCourses(Integer semId, UpdateCoursesCmd updateCoursesCmd) {
        changeCourseTemplateUseCase.execute(new ChangeCourseTemplateCommand(
                semId,
                updateCoursesCmd.getTemplateId(),
                updateCoursesCmd.getCourseIdList()
        ));
    }
}
