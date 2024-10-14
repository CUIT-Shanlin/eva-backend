package edu.cuit.app.service.impl;

import edu.cuit.client.api.ISemesterService;
import edu.cuit.client.dto.clientobject.SemesterCO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SemesterServiceImpl implements ISemesterService {

    @Override
    public List<SemesterCO> all() {
        return List.of();
    }

    @Override
    public SemesterCO now() {
        return null;
    }
}
