package edu.cuit.app.service.impl;

import edu.cuit.client.api.IDepartmentService;
import edu.cuit.domain.gateway.DepartmentGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements IDepartmentService {

    private final DepartmentGateway departmentGateway;

    @Override
    public List<String> all() {
        return departmentGateway.getAll();
    }
}
