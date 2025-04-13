package edu.cuit.client.api.ai;

import edu.cuit.client.bo.ai.AiAnalysisBO;

public interface IAiCourseAnalysisService {

    /**
     * 导出分析报告
     * @param semId 学期id
     * @param teacherId 老师id
     * @return AI分析报告
     */
    AiAnalysisBO analysis(Integer semId,Integer teacherId );

    /**
     * 导出自己的ai分析报告
     * @param semId 学期
     * @return word文档二进制数据
     */
    byte[] exportDocData(Integer semId);

}
