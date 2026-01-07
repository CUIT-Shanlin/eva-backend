package edu.cuit.infra.bccourse.adapter;

import edu.cuit.bc.course.application.port.AllocateTeacherPort;
import edu.cuit.client.dto.cmd.course.AlignTeacherCmd;
import edu.cuit.domain.gateway.course.CourseUpdateGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * bc-course：分配听课/评教老师端口适配器（复用既有 CourseUpdateGateway，保持行为不变）。
 */
@Component
@RequiredArgsConstructor
public class AllocateTeacherPortImpl implements AllocateTeacherPort {
    private final CourseUpdateGateway courseUpdateGateway;

    @Override
    public Map<String, Map<Integer, Integer>> allocateTeacher(Integer semId, AlignTeacherCmd alignTeacherCmd) {
        return courseUpdateGateway.assignTeacher(semId, alignTeacherCmd);
    }
}

