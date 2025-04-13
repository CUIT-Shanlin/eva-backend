package edu.cuit.client.bo.ai;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * AI课程报告业务对象（代表某位老师的一门课）
 */
@Data
@Accessors(chain = true)
public class AiCourseSuggestionBO {

    // 课程名称
    private String courseName;

    // 改进建议
    private String suggestion;

    // 被评教次数
    private Integer beEvaNumCount;

    // 高分（95分以上）次数
    private Integer highScoreBeEvaCount;

    // 优点
    private String adventures;

    // 缺点
    private String drawbacks;

    // 词频统计
//    private List<Pair<String,Integer>> wordFrequencyCount;

}
