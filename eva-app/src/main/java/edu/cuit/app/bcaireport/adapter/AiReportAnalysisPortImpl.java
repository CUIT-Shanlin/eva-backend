package edu.cuit.app.bcaireport.adapter;

import com.alibaba.cola.exception.BizException;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import edu.cuit.app.service.impl.ai.AiCourseAnalysisService;
import edu.cuit.bc.aireport.application.port.AiReportAnalysisPort;
import edu.cuit.bc.evaluation.application.port.EvaRecordQueryPort;
import edu.cuit.client.api.course.IUserCourseService;
import edu.cuit.client.bo.ai.AiAnalysisBO;
import edu.cuit.client.bo.ai.AiCourseSuggestionBO;
import edu.cuit.client.dto.clientobject.course.CourseDetailCO;
import edu.cuit.domain.entity.eva.EvaRecordEntity;
import edu.cuit.domain.gateway.eva.EvaConfigGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.infra.ai.aiservice.CourseAiServices;
import edu.cuit.infra.ai.util.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 报告分析端口适配器（过渡期实现）。
 *
 * <p>保持行为不变：原样执行旧 {@link AiCourseAnalysisService#analysis(Integer, Integer)} 的分析流程，
 * 不改变异常文案与日志顺序。</p>
 */
@Component
public class AiReportAnalysisPortImpl implements AiReportAnalysisPort {
    private static final Logger log = LoggerFactory.getLogger(AiCourseAnalysisService.class);

    private final IUserCourseService userCourseService;
    private final EvaRecordQueryPort evaRecordQueryPort;
    private final UserQueryGateway userQueryGateway;
    private final EvaConfigGateway evaConfigGateway;
    private final QwenChatModel qwenMaxChatModel;
    private final QwenChatModel qwenTurboChatModel;
    private final QwenChatModel deepseekChatModel;

    public AiReportAnalysisPortImpl(
            IUserCourseService userCourseService,
            EvaRecordQueryPort evaRecordQueryPort,
            UserQueryGateway userQueryGateway,
            EvaConfigGateway evaConfigGateway,
            QwenChatModel qwenMaxChatModel,
            QwenChatModel qwenTurboChatModel,
            QwenChatModel deepseekChatModel
    ) {
        this.userCourseService = java.util.Objects.requireNonNull(userCourseService, "userCourseService");
        this.evaRecordQueryPort = java.util.Objects.requireNonNull(evaRecordQueryPort, "evaRecordQueryPort");
        this.userQueryGateway = java.util.Objects.requireNonNull(userQueryGateway, "userQueryGateway");
        this.evaConfigGateway = java.util.Objects.requireNonNull(evaConfigGateway, "evaConfigGateway");
        this.qwenMaxChatModel = java.util.Objects.requireNonNull(qwenMaxChatModel, "qwenMaxChatModel");
        this.qwenTurboChatModel = java.util.Objects.requireNonNull(qwenTurboChatModel, "qwenTurboChatModel");
        this.deepseekChatModel = java.util.Objects.requireNonNull(deepseekChatModel, "deepseekChatModel");
    }

    @Override
    public AiAnalysisBO analysis(Integer semId, Integer teacherId) {
        int highScoreThreshold = evaConfigGateway.getEvaConfig().getHighScoreThreshold();

        List<CourseDetailCO> courseDetailsList = userCourseService.getUserCourseDetail(teacherId, semId)
                .stream().filter(course -> course.getTypeList() != null && course.getDateList() != null)
                .toList();

        Map<String, List<CourseDetailCO>> courseCollect = courseDetailsList.stream()
                .collect(Collectors.groupingBy(cl -> cl.getCourseBaseMsg().getName()));

        Map<String, List<EvaRecordEntity>> courseRecordMap = new HashMap<>();
        courseCollect.forEach((name, courses) -> {
            List<EvaRecordEntity> recordList = new ArrayList<>();
            for (CourseDetailCO course : courses) {
                recordList.addAll(evaRecordQueryPort.getRecordByCourse(course.getCourseBaseMsg().getId()));
            }
            courseRecordMap.put(courses.get(0).getCourseBaseMsg().getName(), recordList);
        });

        List<AiCourseSuggestionBO> courseSuggestionList = new ArrayList<>();
        courseRecordMap.forEach((name, records) -> {
            CourseAiServices aiService = AiServices.builder(CourseAiServices.class)
                    .chatMemory(MessageWindowChatMemory.withMaxMessages(5))
                    .chatLanguageModel(qwenMaxChatModel)
                    .build();
            List<String> reviews = records.parallelStream()
                    .map(record -> {
                        StringBuilder builder = new StringBuilder();
                        builder.append("评价信息：").append(record.getTextValue()).append("评分信息：(");
                        Map<String, Double> map = evaRecordQueryPort.getScorePropMapByProp(record.getFormPropsValues());
                        map.forEach((prop, score) -> {
                            builder.append(" ").append(prop).append(": ").append(score).append(" , ");
                        });
                        builder.append(" )");
                        return builder.toString();
                    }).toList();
            AiCourseSuggestionBO suggestionBO = new AiCourseSuggestionBO();
            String adventure = aiService.analyseAdventure(MessageUtils.convertToList(reviews), name);
            String drawbacks = aiService.analyseDrawbacks();
            String suggestion = aiService.suggestion();
            suggestionBO
                    .setCourseName(name)
                    .setAdventures(adventure)
                    .setDrawbacks(drawbacks)
                    .setSuggestion(suggestion)
                    .setBeEvaNumCount(records.size())
                    .setHighScoreBeEvaCount((int) records.parallelStream()
                            .filter(record -> evaRecordQueryPort.getScoreFromRecord(record.getFormPropsValues()).get() >= highScoreThreshold)
                            .count());
            courseSuggestionList.add(suggestionBO);
        });

        CourseAiServices aiService = AiServices.builder(CourseAiServices.class)
                .chatLanguageModel(deepseekChatModel)
                .build();

        List<String> courseSuggestionStrings = new ArrayList<>();
        for (AiCourseSuggestionBO suggestion : courseSuggestionList) {
            String builder = "课程名称：" + suggestion.getCourseName() +
                    " , 优点：" + suggestion.getAdventures() +
                    " , 缺点：" + suggestion.getDrawbacks() +
                    " , 改进方案：" + suggestion.getSuggestion();
            courseSuggestionStrings.add(builder);
        }

        int totalBeEvaCount = courseRecordMap.values().stream()
                .mapToInt(List::size)
                .sum();

        int highScoreBeEvaCount = courseRecordMap.values().stream()
                .flatMap(List::stream)
                .filter(record -> evaRecordQueryPort.getScoreFromRecord(record.getFormPropsValues()).get() >= highScoreThreshold)
                .toList()
                .size();

        String overallSuggestion = aiService.overallSuggestion(MessageUtils.convertToList(courseSuggestionStrings));

        AiAnalysisBO aiAnalysis = new AiAnalysisBO();
        aiAnalysis
                .setCourseSuggestions(courseSuggestionList)
                .setOverallReport(overallSuggestion)
                .setTeacherName(userQueryGateway.findById(teacherId).orElseThrow(() -> {
                    BizException bizException = new BizException("导出报告失败，请联系管理员");
                    log.error("根据用户id获取用户失败", bizException);
                    return bizException;
                }).getName())
                .setTotalBeEvaCount(totalBeEvaCount)
                .setHighScoreEvaCount(highScoreBeEvaCount);

        return aiAnalysis;
    }
}
