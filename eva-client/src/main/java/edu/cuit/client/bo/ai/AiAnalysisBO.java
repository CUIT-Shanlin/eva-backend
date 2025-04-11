package edu.cuit.client.bo.ai;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

@Data
@Accessors(chain = true)
public class AiAnalysisBO {

    // 老师姓名
    private String teacherName;

    // AI总体总结
    private String overallReport;

    // 被评教总数
    private Integer totalBeEvaCount;

    // 高分（95分以上）总数
    private Integer highScoreEvaCount;

    // 词频统计（由高到低）
    private List<Pair<String,Integer>> wordFrequencyCount;

    // 对每个课程的分析
    private List<AiCourseSuggestionBO> courseSuggestions;
}
