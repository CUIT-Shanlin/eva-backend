package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.port.AllocateTeacherPort;
import edu.cuit.client.dto.cmd.course.AlignTeacherCmd;

import java.util.Map;
import java.util.Objects;

/**
 * 课程写侧：分配听课/评教老师用例（保持行为不变：不在用例层新增校验/异常转换）。
 */
public class AllocateTeacherUseCase {
    private final AllocateTeacherPort port;

    public AllocateTeacherUseCase(AllocateTeacherPort port) {
        this.port = Objects.requireNonNull(port, "port");
    }

    public Map<String, Map<Integer, Integer>> allocateTeacher(Integer semId, AlignTeacherCmd alignTeacherCmd) {
        return port.allocateTeacher(semId, alignTeacherCmd);
    }
}

