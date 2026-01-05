package edu.cuit.app.config;

import edu.cuit.bc.course.application.port.AddCourseTypeRepository;
import edu.cuit.bc.course.application.port.AddCoursePort;
import edu.cuit.bc.course.application.port.AddExistCoursesDetailsRepository;
import edu.cuit.bc.course.application.port.AddExistCoursesDetailsPort;
import edu.cuit.bc.course.application.port.AddNotExistCoursesDetailsPort;
import edu.cuit.bc.course.application.port.AddNotExistCoursesDetailsRepository;
import edu.cuit.bc.course.application.port.ChangeCourseTemplateRepository;
import edu.cuit.bc.course.application.port.AssignEvaTeachersRepository;
import edu.cuit.bc.course.application.port.CourseImportedQueryPort;
import edu.cuit.bc.course.application.port.CourseScheduleQueryPort;
import edu.cuit.bc.course.application.port.CourseTemplateIdQueryPort;
import edu.cuit.bc.course.application.port.CourseDetailQueryPort;
import edu.cuit.bc.course.application.port.DeleteCoursePort;
import edu.cuit.bc.course.application.port.DeleteSelfCoursePort;
import edu.cuit.bc.course.application.port.TimeCourseQueryPort;
import edu.cuit.bc.course.application.port.AllocateTeacherPort;
import edu.cuit.bc.course.application.port.DeleteCoursesPort;
import edu.cuit.bc.course.application.port.DeleteSelfCourseRepository;
import edu.cuit.bc.course.application.port.ImportCourseFilePort;
import edu.cuit.bc.course.application.port.ImportCourseFileRepository;
import edu.cuit.bc.course.application.port.UpdateCoursePort;
import edu.cuit.bc.course.application.port.UpdateCourseInfoRepository;
import edu.cuit.bc.course.application.port.UpdateCourseTypeRepository;
import edu.cuit.bc.course.application.port.UpdateCoursesTypeRepository;
import edu.cuit.bc.course.application.port.DeleteCourseRepository;
import edu.cuit.bc.course.application.port.DeleteCoursesRepository;
import edu.cuit.bc.course.application.port.DeleteCourseTypeRepository;
import edu.cuit.bc.course.application.port.UpdateSingleCoursePort;
import edu.cuit.bc.course.application.usecase.AddCourseTypeUseCase;
import edu.cuit.bc.course.application.usecase.AddCourseEntryUseCase;
import edu.cuit.bc.course.application.usecase.AddExistCoursesDetailsEntryUseCase;
import edu.cuit.bc.course.application.usecase.AddExistCoursesDetailsUseCase;
import edu.cuit.bc.course.application.usecase.AddNotExistCoursesDetailsEntryUseCase;
import edu.cuit.bc.course.application.usecase.AddNotExistCoursesDetailsUseCase;
import edu.cuit.bc.course.application.usecase.AssignTeacherGatewayEntryUseCase;
import edu.cuit.bc.course.application.usecase.AssignEvaTeachersUseCase;
import edu.cuit.bc.course.application.usecase.ChangeCourseTemplateUseCase;
import edu.cuit.bc.course.application.usecase.ChangeSingleCourseTemplateUseCase;
import edu.cuit.bc.course.application.port.UpdateSingleCourseRepository;
import edu.cuit.bc.course.application.usecase.DeleteSelfCourseUseCase;
import edu.cuit.bc.course.application.usecase.DeleteCourseUseCase;
import edu.cuit.bc.course.application.usecase.DeleteCoursesUseCase;
import edu.cuit.bc.course.application.usecase.DeleteCourseTypeUseCase;
import edu.cuit.bc.course.application.usecase.DeleteCourseTypeEntryUseCase;
import edu.cuit.bc.course.application.usecase.DeleteCourseGatewayEntryUseCase;
import edu.cuit.bc.course.application.usecase.DeleteSelfCourseGatewayEntryUseCase;
import edu.cuit.bc.course.application.usecase.DeleteCoursesGatewayEntryUseCase;
import edu.cuit.bc.course.application.usecase.CourseQueryUseCase;
import edu.cuit.bc.course.application.usecase.CourseDetailQueryUseCase;
import edu.cuit.bc.course.application.usecase.TimeCourseQueryUseCase;
import edu.cuit.bc.course.application.usecase.AllocateTeacherUseCase;
import edu.cuit.bc.course.application.usecase.DeleteCoursesEntryUseCase;
import edu.cuit.bc.course.application.usecase.ImportCourseFileUseCase;
import edu.cuit.bc.course.application.usecase.ImportCourseFileGatewayEntryUseCase;
import edu.cuit.bc.course.application.usecase.IsCourseImportedUseCase;
import edu.cuit.bc.course.application.usecase.UpdateCourseInfoUseCase;
import edu.cuit.bc.course.application.usecase.UpdateCourseTypeEntryUseCase;
import edu.cuit.bc.course.application.usecase.UpdateCourseTypeUseCase;
import edu.cuit.bc.course.application.usecase.UpdateCoursesTypeUseCase;
import edu.cuit.bc.course.application.usecase.UpdateCoursesTypeEntryUseCase;
import edu.cuit.bc.course.application.usecase.AddCourseTypeEntryUseCase;
import edu.cuit.bc.course.application.port.UpdateSelfCourseRepository;
import edu.cuit.bc.course.application.usecase.UpdateSelfCourseUseCase;
import edu.cuit.bc.course.application.usecase.UpdateSingleCourseEntryUseCase;
import edu.cuit.bc.course.application.usecase.UpdateSingleCourseUseCase;
import edu.cuit.bc.course.application.usecase.DeleteSelfCourseEntryUseCase;
import edu.cuit.bc.course.application.usecase.DeleteCourseEntryUseCase;
import edu.cuit.bc.course.application.port.UpdateSelfCoursePort;
import edu.cuit.bc.course.application.usecase.ImportCourseFileEntryUseCase;
import edu.cuit.bc.course.application.usecase.UpdateCourseEntryUseCase;
import edu.cuit.bc.course.application.usecase.UpdateCourseGatewayEntryUseCase;
import edu.cuit.bc.course.application.usecase.UpdateCoursesEntryUseCase;
import edu.cuit.bc.course.application.usecase.UpdateSelfCourseEntryUseCase;
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
    public IsCourseImportedUseCase isCourseImportedUseCase(CourseImportedQueryPort queryPort) {
        return new IsCourseImportedUseCase(queryPort);
    }

    @Bean
    public CourseQueryUseCase courseQueryUseCase(CourseScheduleQueryPort queryPort) {
        return new CourseQueryUseCase(queryPort);
    }

    @Bean
    public CourseDetailQueryUseCase courseDetailQueryUseCase(CourseDetailQueryPort queryPort) {
        return new CourseDetailQueryUseCase(queryPort);
    }

    @Bean
    public TimeCourseQueryUseCase timeCourseQueryUseCase(TimeCourseQueryPort queryPort) {
        return new TimeCourseQueryUseCase(queryPort);
    }

    @Bean
    public AllocateTeacherUseCase allocateTeacherUseCase(AllocateTeacherPort port) {
        return new AllocateTeacherUseCase(port);
    }

    @Bean
    public DeleteCoursesEntryUseCase deleteCoursesEntryUseCase(DeleteCoursesPort port) {
        return new DeleteCoursesEntryUseCase(port);
    }

    @Bean
    public UpdateSingleCourseEntryUseCase updateSingleCourseEntryUseCase(UpdateSingleCoursePort port) {
        return new UpdateSingleCourseEntryUseCase(port);
    }

    @Bean
    public AddNotExistCoursesDetailsEntryUseCase addNotExistCoursesDetailsEntryUseCase(AddNotExistCoursesDetailsPort port) {
        return new AddNotExistCoursesDetailsEntryUseCase(port);
    }

    @Bean
    public AddExistCoursesDetailsEntryUseCase addExistCoursesDetailsEntryUseCase(AddExistCoursesDetailsPort port) {
        return new AddExistCoursesDetailsEntryUseCase(port);
    }

    @Bean
    public DeleteSelfCourseEntryUseCase deleteSelfCourseEntryUseCase(DeleteSelfCoursePort port) {
        return new DeleteSelfCourseEntryUseCase(port);
    }

    @Bean
    public UpdateSelfCourseEntryUseCase updateSelfCourseEntryUseCase(UpdateSelfCoursePort port) {
        return new UpdateSelfCourseEntryUseCase(port);
    }

    @Bean
    public ImportCourseFileEntryUseCase importCourseFileEntryUseCase(ImportCourseFilePort port) {
        return new ImportCourseFileEntryUseCase(port);
    }

    @Bean
    public UpdateCourseEntryUseCase updateCourseEntryUseCase(UpdateCoursePort port) {
        return new UpdateCourseEntryUseCase(port);
    }

    @Bean
    public UpdateCoursesEntryUseCase updateCoursesEntryUseCase(ChangeCourseTemplateUseCase changeCourseTemplateUseCase) {
        return new UpdateCoursesEntryUseCase(changeCourseTemplateUseCase);
    }

    @Bean
    public DeleteCourseEntryUseCase deleteCourseEntryUseCase(DeleteCoursePort port) {
        return new DeleteCourseEntryUseCase(port);
    }

    @Bean
    public AddCourseEntryUseCase addCourseEntryUseCase(AddCoursePort port) {
        return new AddCourseEntryUseCase(port);
    }

    @Bean
    public AssignEvaTeachersUseCase assignEvaTeachersUseCase(AssignEvaTeachersRepository repository) {
        return new AssignEvaTeachersUseCase(repository);
    }

    @Bean
    public AssignTeacherGatewayEntryUseCase assignTeacherGatewayEntryUseCase(AssignEvaTeachersUseCase useCase) {
        return new AssignTeacherGatewayEntryUseCase(useCase);
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
    public ImportCourseFileGatewayEntryUseCase importCourseFileGatewayEntryUseCase(ImportCourseFileUseCase useCase) {
        return new ImportCourseFileGatewayEntryUseCase(useCase);
    }

    @Bean
    public UpdateCourseInfoUseCase updateCourseInfoUseCase(UpdateCourseInfoRepository repository) {
        return new UpdateCourseInfoUseCase(repository);
    }

    @Bean
    public UpdateCourseGatewayEntryUseCase updateCourseGatewayEntryUseCase(UpdateCourseInfoUseCase useCase) {
        return new UpdateCourseGatewayEntryUseCase(useCase);
    }

    @Bean
    public UpdateCourseTypeUseCase updateCourseTypeUseCase(UpdateCourseTypeRepository repository) {
        return new UpdateCourseTypeUseCase(repository);
    }

    @Bean
    public UpdateCourseTypeEntryUseCase updateCourseTypeEntryUseCase(UpdateCourseTypeUseCase useCase) {
        return new UpdateCourseTypeEntryUseCase(useCase);
    }

    @Bean
    public UpdateCoursesTypeUseCase updateCoursesTypeUseCase(UpdateCoursesTypeRepository repository) {
        return new UpdateCoursesTypeUseCase(repository);
    }

    @Bean
    public UpdateCoursesTypeEntryUseCase updateCoursesTypeEntryUseCase(UpdateCoursesTypeUseCase useCase) {
        return new UpdateCoursesTypeEntryUseCase(useCase);
    }

    @Bean
    public AddCourseTypeEntryUseCase addCourseTypeEntryUseCase(AddCourseTypeUseCase useCase) {
        return new AddCourseTypeEntryUseCase(useCase);
    }

    @Bean
    public DeleteSelfCourseUseCase deleteSelfCourseUseCase(DeleteSelfCourseRepository repository) {
        return new DeleteSelfCourseUseCase(repository);
    }

    @Bean
    public DeleteSelfCourseGatewayEntryUseCase deleteSelfCourseGatewayEntryUseCase(DeleteSelfCourseUseCase useCase) {
        return new DeleteSelfCourseGatewayEntryUseCase(useCase);
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
    public DeleteCourseGatewayEntryUseCase deleteCourseGatewayEntryUseCase(DeleteCourseUseCase useCase) {
        return new DeleteCourseGatewayEntryUseCase(useCase);
    }

    @Bean
    public DeleteCoursesUseCase deleteCoursesUseCase(DeleteCoursesRepository repository) {
        return new DeleteCoursesUseCase(repository);
    }

    @Bean
    public DeleteCoursesGatewayEntryUseCase deleteCoursesGatewayEntryUseCase(DeleteCoursesUseCase useCase) {
        return new DeleteCoursesGatewayEntryUseCase(useCase);
    }

    @Bean
    public DeleteCourseTypeUseCase deleteCourseTypeUseCase(DeleteCourseTypeRepository repository) {
        return new DeleteCourseTypeUseCase(repository);
    }

    @Bean
    public DeleteCourseTypeEntryUseCase deleteCourseTypeEntryUseCase(DeleteCourseTypeUseCase useCase) {
        return new DeleteCourseTypeEntryUseCase(useCase);
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
