package edu.cuit.app.config;

import edu.cuit.bc.course.application.port.ChangeCourseTemplateRepository;
import edu.cuit.bc.course.application.port.AssignEvaTeachersRepository;
import edu.cuit.bc.course.application.port.CourseTemplateIdQueryPort;
import edu.cuit.bc.course.application.port.ImportCourseFileRepository;
import edu.cuit.bc.course.application.port.UpdateCourseInfoRepository;
import edu.cuit.bc.course.application.usecase.AssignEvaTeachersUseCase;
import edu.cuit.bc.course.application.usecase.ChangeCourseTemplateUseCase;
import edu.cuit.bc.course.application.usecase.ChangeSingleCourseTemplateUseCase;
import edu.cuit.bc.course.application.port.UpdateSingleCourseRepository;
import edu.cuit.bc.course.application.usecase.ImportCourseFileUseCase;
import edu.cuit.bc.course.application.usecase.UpdateCourseInfoUseCase;
import edu.cuit.bc.course.application.usecase.UpdateSingleCourseUseCase;
import edu.cuit.bc.template.application.CourseTemplateLockService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * bc-course 的组合根（单体阶段由 eva-app 负责装配）。
 */
@Configuration
public class BcCourseConfiguration {
    @Bean
    public ChangeCourseTemplateUseCase changeCourseTemplateUseCase(
            ChangeCourseTemplateRepository repository,
            CourseTemplateLockService courseTemplateLockService
    ) {
        return new ChangeCourseTemplateUseCase(repository, courseTemplateLockService);
    }

    @Bean
    public ChangeSingleCourseTemplateUseCase changeSingleCourseTemplateUseCase(
            CourseTemplateIdQueryPort courseTemplateIdQueryPort,
            ChangeCourseTemplateRepository repository,
            CourseTemplateLockService courseTemplateLockService
    ) {
        return new ChangeSingleCourseTemplateUseCase(courseTemplateIdQueryPort, repository, courseTemplateLockService);
    }

    @Bean
    public AssignEvaTeachersUseCase assignEvaTeachersUseCase(AssignEvaTeachersRepository repository) {
        return new AssignEvaTeachersUseCase(repository);
    }

    @Bean
    public UpdateSingleCourseUseCase updateSingleCourseUseCase(UpdateSingleCourseRepository repository) {
        return new UpdateSingleCourseUseCase(repository);
    }

    @Bean
    public ImportCourseFileUseCase importCourseFileUseCase(ImportCourseFileRepository repository) {
        return new ImportCourseFileUseCase(repository);
    }

    @Bean
    public UpdateCourseInfoUseCase updateCourseInfoUseCase(UpdateCourseInfoRepository repository) {
        return new UpdateCourseInfoUseCase(repository);
    }
}
