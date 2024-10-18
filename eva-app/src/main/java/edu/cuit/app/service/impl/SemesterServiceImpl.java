package edu.cuit.app.service.impl;

import edu.cuit.client.api.ISemesterService;
import edu.cuit.client.dto.clientobject.SemesterCO;
import edu.cuit.domain.gateway.SemesterGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SemesterServiceImpl implements ISemesterService {

    private final SemesterGateway semesterGateway;

    @Override
    public List<SemesterCO> all() {
        return semesterGateway.getAll();
    }

    @Override
    public SemesterCO now() {
        return semesterGateway.getNow();
    }

    @Override
    public SemesterCO semesterInfo(Integer id) {
        return semesterGateway.getSemesterInfo(id);
    }
}
