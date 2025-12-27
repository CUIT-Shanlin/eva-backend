package edu.cuit.client.api;

import java.util.List;

/**
 * 教室相关业务接口
 */
public interface IClassroomService {

    /**
     * 获取所有教室
     */
    List<String> getAll();

}
