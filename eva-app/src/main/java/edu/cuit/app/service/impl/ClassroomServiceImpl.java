package edu.cuit.app.service.impl;

import edu.cuit.client.api.IClassroomService;
import edu.cuit.domain.gateway.ClassroomGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassroomServiceImpl implements IClassroomService {

    private final ClassroomGateway classroomGateway;

    @Override
    public List<String> getAll() {
        return classroomGateway.getAll();
    }
}
