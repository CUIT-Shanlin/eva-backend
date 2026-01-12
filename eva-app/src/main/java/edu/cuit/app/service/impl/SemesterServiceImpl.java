package edu.cuit.app.service.impl;

import edu.cuit.client.api.ISemesterService;
import edu.cuit.client.dto.clientobject.SemesterCO;
import edu.cuit.bc.course.application.usecase.SemesterQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SemesterServiceImpl implements ISemesterService {

    private final SemesterQueryUseCase semesterQueryUseCase;

    @Override
    @Transactional
    public List<SemesterCO> all() {
        return semesterQueryUseCase.all();
    }

    @Override
    @Transactional
    public SemesterCO now() {
        return semesterQueryUseCase.now();
    }

    @Override
    @Transactional
    public SemesterCO semesterInfo(Integer id) {
        return semesterQueryUseCase.semesterInfo(id);
    }
}
