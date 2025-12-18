package edu.cuit.app.config;

import edu.cuit.bc.course.application.port.AddCourseTypeRepository;
import edu.cuit.bc.course.application.port.AddExistCoursesDetailsRepository;
import edu.cuit.bc.course.application.port.AddNotExistCoursesDetailsRepository;
import edu.cuit.bc.course.application.port.ChangeCourseTemplateRepository;
import edu.cuit.bc.course.application.port.AssignEvaTeachersRepository;
import edu.cuit.bc.course.application.port.CourseTemplateIdQueryPort;
import edu.cuit.bc.course.application.port.DeleteSelfCourseRepository;
import edu.cuit.bc.course.application.port.ImportCourseFileRepository;
import edu.cuit.bc.course.application.port.UpdateCourseInfoRepository;
import edu.cuit.bc.course.application.port.UpdateCourseTypeRepository;
import edu.cuit.bc.course.application.port.UpdateCoursesTypeRepository;
import edu.cuit.bc.course.application.port.DeleteCourseRepository;
import edu.cuit.bc.course.application.port.DeleteCoursesRepository;
import edu.cuit.bc.course.application.port.DeleteCourseTypeRepository;
import edu.cuit.bc.course.application.usecase.AddCourseTypeUseCase;
import edu.cuit.bc.course.application.usecase.AddExistCoursesDetailsUseCase;
import edu.cuit.bc.course.application.usecase.AddNotExistCoursesDetailsUseCase;
import edu.cuit.bc.course.application.usecase.AssignEvaTeachersUseCase;
import edu.cuit.bc.course.application.usecase.ChangeCourseTemplateUseCase;
import edu.cuit.bc.course.application.usecase.ChangeSingleCourseTemplateUseCase;
import edu.cuit.bc.course.application.port.UpdateSingleCourseRepository;
import edu.cuit.bc.course.application.usecase.DeleteSelfCourseUseCase;
import edu.cuit.bc.course.application.usecase.DeleteCourseUseCase;
import edu.cuit.bc.course.application.usecase.DeleteCoursesUseCase;
import edu.cuit.bc.course.application.usecase.DeleteCourseTypeUseCase;
import edu.cuit.bc.course.application.usecase.ImportCourseFileUseCase;
import edu.cuit.bc.course.application.usecase.UpdateCourseInfoUseCase;
import edu.cuit.bc.course.application.usecase.UpdateCourseTypeUseCase;
import edu.cuit.bc.course.application.usecase.UpdateCoursesTypeUseCase;
import edu.cuit.bc.course.application.port.UpdateSelfCourseRepository;
import edu.cuit.bc.course.application.usecase.UpdateSelfCourseUseCase;
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

    @Bean
    public UpdateCourseTypeUseCase updateCourseTypeUseCase(UpdateCourseTypeRepository repository) {
        return new UpdateCourseTypeUseCase(repository);
    }

    @Bean
    public UpdateCoursesTypeUseCase updateCoursesTypeUseCase(UpdateCoursesTypeRepository repository) {
        return new UpdateCoursesTypeUseCase(repository);
    }

    @Bean
    public DeleteSelfCourseUseCase deleteSelfCourseUseCase(DeleteSelfCourseRepository repository) {
        return new DeleteSelfCourseUseCase(repository);
    }

    @Bean
    public UpdateSelfCourseUseCase updateSelfCourseUseCase(UpdateSelfCourseRepository repository) {
        return new UpdateSelfCourseUseCase(repository);
    }

    @Bean
    public DeleteCourseUseCase deleteCourseUseCase(DeleteCourseRepository repository) {
        return new DeleteCourseUseCase(repository);
    }

    @Bean
    public DeleteCoursesUseCase deleteCoursesUseCase(DeleteCoursesRepository repository) {
        return new DeleteCoursesUseCase(repository);
    }

    @Bean
    public DeleteCourseTypeUseCase deleteCourseTypeUseCase(DeleteCourseTypeRepository repository) {
        return new DeleteCourseTypeUseCase(repository);
    }

    @Bean
    public AddCourseTypeUseCase addCourseTypeUseCase(AddCourseTypeRepository repository) {
        return new AddCourseTypeUseCase(repository);
    }

    @Bean
    public AddNotExistCoursesDetailsUseCase addNotExistCoursesDetailsUseCase(AddNotExistCoursesDetailsRepository repository) {
        return new AddNotExistCoursesDetailsUseCase(repository);
    }

    @Bean
    public AddExistCoursesDetailsUseCase addExistCoursesDetailsUseCase(AddExistCoursesDetailsRepository repository) {
        return new AddExistCoursesDetailsUseCase(repository);
    }
}
