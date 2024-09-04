package edu.cuit.client.dto.cmd.course;

import com.alibaba.cola.dto.ClientObject;
import edu.cuit.client.dto.clientobject.course.CourseMsgCO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 一门课程的可修改信息(一门课程的可修改信息)
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class UpdateCourseCmd extends ClientObject {
        /**
         * 课程id
         */
        private Integer id;

        /**
         * 科目信息
         */
        private CourseMsgCO subjectMsg;

        /**
         * 评教模板id
         */
        private Integer templateId;

        /**
         * 创建时间
         */
        private LocalDateTime createTime;

        /**
         * 更新时间
         */
        private LocalDateTime updateTime;

        /**
         * 类型id数组
         */
        private List<Integer> typeIdList;
}
