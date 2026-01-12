package edu.cuit.app.service.impl;

import edu.cuit.bc.iam.application.contract.api.department.IDepartmentService;
import edu.cuit.bc.iam.application.usecase.DepartmentQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements IDepartmentService {

    private final DepartmentQueryUseCase departmentQueryUseCase;

    @Override
    public List<String> all() {
        return departmentQueryUseCase.all();
    }
}
