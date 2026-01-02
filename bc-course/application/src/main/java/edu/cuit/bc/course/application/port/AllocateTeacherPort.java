package edu.cuit.bc.course.application.port;

import edu.cuit.client.dto.cmd.course.AlignTeacherCmd;

import java.util.Map;

/**
 * 课程写侧：分配听课/评教老师端口（渐进式重构：委托既有 legacy gateway，保持行为不变）。
 */
public interface AllocateTeacherPort {
    Map<String, Map<Integer, Integer>> allocateTeacher(Integer semId, AlignTeacherCmd alignTeacherCmd);
}

