package edu.cuit.client.dto.cmd.course;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 一门课程的可修改信息(一门课程的可修改信息)
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class UpdateCoursesCmd extends ClientObject {
    /**
     * 课程id数组
      */
    private List<Integer> courseIdList;

    /**
     *待改成的模版id
     */
    private Integer templateId;
}
