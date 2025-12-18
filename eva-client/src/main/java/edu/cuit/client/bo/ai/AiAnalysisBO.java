package edu.cuit.client.bo.ai;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * AI分析报告（代表一位老师的报告）
 */
@Data
@Accessors(chain = true)
public class AiAnalysisBO {

    // 老师姓名
    private String teacherName;

    // AI总体总结
    private String overallReport;

    // 被评教总数
    private Integer totalBeEvaCount;

    // 高分总数（阈值来自评教配置 highScoreThreshold）
    private Integer highScoreEvaCount;

    // 词频统计（由高到低）
    private List<Pair<String,Integer>> wordFrequencyCount;

    // 对每个课程的分析
    private List<AiCourseSuggestionBO> courseSuggestions;
}
