package edu.cuit.app.service.impl.ai;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.cola.exception.BizException;
import com.alibaba.cola.exception.SysException;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import edu.cuit.app.aop.CheckSemId;
import edu.cuit.app.poi.ai.AiReportExporter;
import edu.cuit.client.api.ai.IAiCourseAnalysisService;
import edu.cuit.client.api.course.IUserCourseService;
import edu.cuit.client.bo.ai.AiAnalysisBO;
import edu.cuit.client.bo.ai.AiCourseSuggestionBO;
import edu.cuit.client.dto.clientobject.course.CourseDetailCO;
import edu.cuit.client.dto.clientobject.eva.EvaRecordCO;
import edu.cuit.domain.entity.eva.EvaRecordEntity;
import edu.cuit.domain.gateway.eva.EvaQueryGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.infra.ai.aiservice.CourseAiServices;
import edu.cuit.infra.ai.util.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCourseAnalysisService implements IAiCourseAnalysisService {

    private final IUserCourseService userCourseService;
    private final EvaQueryGateway evaQueryGateway;
    private final UserQueryGateway userQueryGateway;

    private final QwenChatModel qwenMaxChatModel;
    private final QwenChatModel qwenTurboChatModel;
    private final QwenChatModel deepseekChatModel;

    @Autowired
    @Lazy
    private IAiCourseAnalysisService iAiCourseAnalysisService;

    @Override
    @CheckSemId
    public AiAnalysisBO analysis(Integer semId, Integer teacherId) {
        List<CourseDetailCO> courseDetailsList = userCourseService.getUserCourseDetail(teacherId, semId);

        Map<String, List<CourseDetailCO>> courseCollect = courseDetailsList.stream()
                .collect(Collectors.groupingBy(cl -> cl.getCourseBaseMsg().getName()));

        Map<String,List<EvaRecordEntity>> courseRecordMap = new HashMap<>();
        courseCollect.forEach((name,courses) -> {
            List<EvaRecordEntity> recordList = new ArrayList<>();
            for (CourseDetailCO course : courses) {
                recordList.addAll(evaQueryGateway.getRecordByCourse(course.getCourseBaseMsg().getId()));
            }
            courseRecordMap.put(courses.get(0).getCourseBaseMsg().getName(),recordList);
        });

        List<AiCourseSuggestionBO> courseSuggestionList = new ArrayList<>();
        courseRecordMap.forEach((name,records) -> {
            CourseAiServices aiService = AiServices.builder(CourseAiServices.class)
                    .chatMemory(MessageWindowChatMemory.withMaxMessages(5))
                    .chatLanguageModel(qwenMaxChatModel)
                    .build();
            List<String> reviews = records.parallelStream()
                    .map(record -> {
                        StringBuilder builder = new StringBuilder();
                        builder.append("评价信息：").append(record.getTextValue()).append("评分信息：(");
                        Map<String, Double> map = evaQueryGateway.getScorePropMapByProp(record.getFormPropsValues());
                        map.forEach((prop,score) -> {
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
                            .filter(record -> evaQueryGateway.getScoreFromRecord(record.getFormPropsValues()).get() >= 95)
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
                .filter(record -> evaQueryGateway.getScoreFromRecord(record.getFormPropsValues()).get() >= 95)
                .toList()
                .size();

        String overallSuggestion = aiService.overallSuggestion(MessageUtils.convertToList(courseSuggestionStrings));

        AiAnalysisBO aiAnalysis = new AiAnalysisBO();
        aiAnalysis
                .setCourseSuggestions(courseSuggestionList)
                .setOverallReport(overallSuggestion)
                .setTeacherName(userQueryGateway.findById(teacherId).orElseThrow(() -> {
                    BizException bizException = new BizException("导出报告失败，请联系管理员");
                    log.error("根据用户id获取用户失败",bizException);
                    return bizException;
                }).getName())
                .setTotalBeEvaCount(totalBeEvaCount)
                .setHighScoreEvaCount(highScoreBeEvaCount);

        return aiAnalysis;
    }

    @Override
    public byte[] exportDocData(Integer semId) {

        Integer userId = userQueryGateway.findIdByUsername(((String) StpUtil.getLoginId()))
                .orElseThrow(() -> {
                    SysException e = new SysException("用户数据查找失败，请联系管理员");
                    log.error("系统异常", e);
                    return e;
                });

        AiAnalysisBO analysis = iAiCourseAnalysisService.analysis(semId, userId);
        XWPFDocument document = new AiReportExporter().generateReport(analysis);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            document.write(outputStream);
            byte[] bytes = outputStream.toByteArray();
            outputStream.close();
            return bytes;
        } catch (IOException e) {
            log.error("AI报告导出失败",e);
            throw new SysException("报告导出失败，请联系管理员");
        }

    }
}
