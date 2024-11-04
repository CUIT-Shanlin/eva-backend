package edu.cuit.client.dto.cmd.course;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 批量修改课程对应类型的模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class UpdateCoursesToTypeCmd extends ClientObject {

    /**
     * 课程id数组
     */
    private List<Integer> courseIdList;

    /**
     * 待改成的课程类型id数组
     */
    private List<Integer> typeIdList;
}
