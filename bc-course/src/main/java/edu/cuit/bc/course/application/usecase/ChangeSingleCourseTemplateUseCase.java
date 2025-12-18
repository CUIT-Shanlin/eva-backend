package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.ChangeSingleCourseTemplateCommand;
import edu.cuit.bc.course.application.port.ChangeCourseTemplateRepository;
import edu.cuit.bc.course.application.port.CourseTemplateIdQueryPort;
import edu.cuit.bc.course.domain.ChangeCourseTemplateException;
import edu.cuit.bc.course.domain.CourseNotFoundException;
import edu.cuit.bc.template.application.CourseTemplateLockService;
import edu.cuit.bc.template.domain.TemplateLockedException;

import java.util.List;
import java.util.Objects;

/**
 * 单课程模板切换用例。
 *
 * <p>注意：前端/调用方可能会“总是带上 templateId”，因此必须先判断“是否真的发生切换”。</p>
 */
public class ChangeSingleCourseTemplateUseCase {
    private final CourseTemplateIdQueryPort courseTemplateIdQueryPort;
    private final ChangeCourseTemplateRepository repository;
    private final CourseTemplateLockService courseTemplateLockService;

    public ChangeSingleCourseTemplateUseCase(
            CourseTemplateIdQueryPort courseTemplateIdQueryPort,
            ChangeCourseTemplateRepository repository,
            CourseTemplateLockService courseTemplateLockService
    ) {
        this.courseTemplateIdQueryPort = Objects.requireNonNull(courseTemplateIdQueryPort, "courseTemplateIdQueryPort");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.courseTemplateLockService = Objects.requireNonNull(courseTemplateLockService, "courseTemplateLockService");
    }

    public void execute(ChangeSingleCourseTemplateCommand command) {
        Objects.requireNonNull(command, "command");

        Integer semesterId = command.semesterId();
        Integer courseId = command.courseId();
        Integer templateId = command.templateId();

        // 未传 templateId：视为“不修改模板”
        if (templateId == null) {
            return;
        }
        if (semesterId == null) {
            throw new ChangeCourseTemplateException("学期ID不能为空");
        }
        if (courseId == null) {
            throw new ChangeCourseTemplateException("课程ID不能为空");
        }

        Integer currentTemplateId = courseTemplateIdQueryPort.findTemplateId(semesterId, courseId)
                .orElseThrow(() -> new CourseNotFoundException("没有该课程"));

        // templateId 未变化：允许继续修改课程其它字段，不触发“锁定不可切换”规则
        if (Objects.equals(currentTemplateId, templateId)) {
            return;
        }

        try {
            courseTemplateLockService.assertCanChangeTemplate(courseId, semesterId);
        } catch (TemplateLockedException e) {
            throw ChangeCourseTemplateException.templateLocked(courseId);
        }

        repository.changeTemplate(semesterId, templateId, List.of(courseId));
    }
}
