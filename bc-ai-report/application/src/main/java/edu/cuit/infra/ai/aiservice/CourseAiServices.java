package edu.cuit.infra.ai.aiservice;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

public interface CourseAiServices {

    @SystemMessage(PromptConstants.SYSTEM_PROMPT)
    @UserMessage("""
            该老师教学的课程是 {{course}}
            以下为其他老师对该老师所教学的这门课的评价和打分，分析出该老师教学方式的优点：
            {{reviews}}
            """)
    String analyseAdventure(@V("reviews") String reviews,@V("course") String courseName);

    @SystemMessage(PromptConstants.SYSTEM_PROMPT)
    @UserMessage("""
            分析出该老师教学方式的缺点，不要提供改进方式
            """)
    String analyseDrawbacks();

    @SystemMessage(PromptConstants.SYSTEM_PROMPT)
    @UserMessage("""
            根据以上优点和缺点的分析，提供改进方案
            """)
    String suggestion();

    @SystemMessage(PromptConstants.SYSTEM_PROMPT)
    @UserMessage("""
            以下为一位老师所教学的所有课程和已分析出的其优缺点和改进方案，请综合分析这些信息，给该教师一个总体的总结和建议：
            {{suggestions}}
            """)
    String overallSuggestion(@V("suggestions") String suggestions);

}
