package edu.cuit.app.service.impl;

import edu.cuit.client.api.IClassroomService;
import edu.cuit.bc.course.application.usecase.ClassroomQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassroomServiceImpl implements IClassroomService {

    private final ClassroomQueryUseCase classroomQueryUseCase;

    @Override
    public List<String> getAll() {
        return classroomQueryUseCase.getAll();
    }
}
