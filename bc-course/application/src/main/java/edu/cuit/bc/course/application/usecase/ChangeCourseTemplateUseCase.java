package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.ChangeCourseTemplateCommand;
import edu.cuit.bc.course.application.port.ChangeCourseTemplateRepository;
import edu.cuit.bc.course.domain.ChangeCourseTemplateException;
import edu.cuit.bc.template.application.CourseTemplateLockService;
import edu.cuit.bc.template.domain.TemplateLockedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 批量切换课程模板用例。
 *
 * <p>业务规则：只要课程在该学期产生过评教记录（模板被锁定），则不允许切换模板。</p>
 */
public class ChangeCourseTemplateUseCase {
    private final ChangeCourseTemplateRepository repository;
    private final CourseTemplateLockService courseTemplateLockService;

    public ChangeCourseTemplateUseCase(ChangeCourseTemplateRepository repository, CourseTemplateLockService courseTemplateLockService) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.courseTemplateLockService = Objects.requireNonNull(courseTemplateLockService, "courseTemplateLockService");
    }

    public void execute(ChangeCourseTemplateCommand command) {
        Objects.requireNonNull(command, "command");

        Integer semesterId = command.semesterId();
        Integer templateId = command.templateId();
        List<Integer> courseIdList = command.courseIdList();

        if (semesterId == null) {
            throw new ChangeCourseTemplateException("学期ID不能为空");
        }
        if (templateId == null) {
            throw new ChangeCourseTemplateException("模板ID不能为空");
        }
        if (courseIdList == null || courseIdList.isEmpty()) {
            throw new ChangeCourseTemplateException("课程ID列表不能为空");
        }
        if (courseIdList.stream().anyMatch(Objects::isNull)) {
            throw new ChangeCourseTemplateException("课程ID列表不能包含空值");
        }

        // 批量切换模板前，先整体校验锁定（避免部分成功部分失败）
        List<Integer> lockedCourseIds = new ArrayList<>();
        for (Integer courseId : courseIdList) {
            try {
                courseTemplateLockService.assertCanChangeTemplate(courseId, semesterId);
            } catch (TemplateLockedException e) {
                lockedCourseIds.add(courseId);
            }
        }
        if (!lockedCourseIds.isEmpty()) {
            throw ChangeCourseTemplateException.templateLocked(lockedCourseIds);
        }

        repository.changeTemplate(semesterId, templateId, courseIdList);
    }
}
