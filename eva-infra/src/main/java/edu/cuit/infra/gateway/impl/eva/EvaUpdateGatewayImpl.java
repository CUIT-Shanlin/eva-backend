package edu.cuit.infra.gateway.impl.eva;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import edu.cuit.client.dto.clientobject.eva.AddTaskCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskFormCO;
import edu.cuit.client.dto.clientobject.eva.EvaTemplateCO;
import edu.cuit.domain.gateway.eva.EvaUpdateGateway;
import edu.cuit.infra.convertor.eva.EvaConvertor;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SemesterDO;
import edu.cuit.infra.dal.database.dataobject.eva.*;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.SemesterMapper;
import edu.cuit.infra.dal.database.mapper.eva.*;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EvaUpdateGatewayImpl implements EvaUpdateGateway {
    private final FormTemplateMapper formTemplateMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final FormRecordMapper formRecordMapper;
    private final CourInfMapper courInfMapper;
    private final CourseMapper courseMapper;
    private final CourOneEvaTemplateMapper courOneEvaTemplateMapper;

    private final SemesterMapper semesterMapper;
    @Override
    @Transactional
    public Void updateEvaTemplate(EvaTemplateCO evaTemplateCO) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        //检验是否那个模板prop有重复
        if(evaTemplateCO.getProps()!=null) {
            List<String> props= Arrays.stream(evaTemplateCO.getProps().split(",")).toList();
            long count = props.stream().distinct().count();
            if (props.size() != count) {
                throw new UpdateException("由于你输入的指标中有重复数据，故不能修改");
            }
        }

        FormTemplateDO formTemplateDO=new FormTemplateDO();
        formTemplateDO.setDescription(evaTemplateCO.getDescription());
        formTemplateDO.setProps(evaTemplateCO.getProps());
        formTemplateDO.setIsDefault(evaTemplateCO.getIsDefault());
        formTemplateDO.setIsDeleted(0);
        formTemplateDO.setName(evaTemplateCO.getName());
        formTemplateDO.setUpdateTime(LocalDateTime.parse(evaTemplateCO.getUpdateTime(),df));
        formTemplateDO.setCreateTime(LocalDateTime.parse(evaTemplateCO.getCreateTime(),df));
        formTemplateMapper.update(formTemplateDO, new QueryWrapper<FormTemplateDO>().eq("id", evaTemplateCO.getId()));
        LogUtils.logContent(formTemplateMapper.selectById(evaTemplateCO.getId()).getName() +" 评教模板");
        return null;
    }
    @Override
    @Transactional
    public Void putEvaTemplate(EvaTaskFormCO evaTaskFormCO) {
        EvaTaskDO evaTaskDO=evaTaskMapper.selectById(evaTaskFormCO.getTaskId());
        CourInfDO courInfDO=courInfMapper.selectById(evaTaskDO.getCourInfId());
        CourseDO courseDO=courseMapper.selectById(courInfDO.getCourseId());
        CourOneEvaTemplateDO courOneEvaTemplateDO=courOneEvaTemplateMapper.selectOne(new QueryWrapper<CourOneEvaTemplateDO>().eq("course_id",courseDO.getId()));
        FormTemplateDO formTemplateDO=formTemplateMapper.selectById(courseDO.getTemplateId());
        //把评教的具体数据传进去给评教记录
        FormRecordDO formRecordDO=new FormRecordDO();
        formRecordDO.setIsDeleted(0);
        formRecordDO.setTaskId(evaTaskFormCO.getTaskId());
        formRecordDO.setTextValue(evaTaskFormCO.getTextValue());
        formRecordDO.setCreateTime(LocalDateTime.now());
        formRecordDO.setIsDeleted(0);

        formRecordDO.setFormPropsValues(JSONUtil.toJsonStr(evaTaskFormCO.getFormPropsValues()));
        formRecordMapper.insert(formRecordDO);

        //通过任务id把任务状态改了
        evaTaskDO.setStatus(1);
        evaTaskDO.setUpdateTime(LocalDateTime.now());
        evaTaskMapper.update(evaTaskDO,new QueryWrapper<EvaTaskDO>().eq("id",evaTaskFormCO.getTaskId()));

        //检验是否有快照模板，没有就建一个
        if(courOneEvaTemplateDO==null){
            CourOneEvaTemplateDO courOneEvaTemplateDO1=new CourOneEvaTemplateDO();
            courOneEvaTemplateDO1.setCourseId(courseDO.getId());
            courOneEvaTemplateDO1.setSemesterId(courseDO.getSemesterId());
            String s="{\"name\":\""+formTemplateDO.getName()+"\",\"description\":\""+formTemplateDO.getDescription()+"\",\"props\":\""+formTemplateDO.getProps()+"\"}";
            courOneEvaTemplateDO1.setFormTemplate(s);
            courOneEvaTemplateMapper.insert(courOneEvaTemplateDO1);
        }
        return null;
    }

    @Override
    @Transactional
    public Integer postEvaTask(AddTaskCO addTaskCO) {
        //同时发送该任务的评教待办消息;
        CourInfDO courInfDO=courInfMapper.selectById(addTaskCO.getCourInfId());
        CourseDO courseDO=courseMapper.selectById(courInfDO.getCourseId());
        //选中的课程是否已经上完
        SemesterDO semesterDO = semesterMapper.selectById(courseDO.getSemesterId());
        LocalDate localDate = semesterDO.getStartDate().plusDays((courInfDO.getWeek()-1) * 7L + courInfDO.getDay() - 1);

        Integer f=2;//判断是不是课程快临近结束或已经结束 1冲0无
        if(localDate.getYear()>=LocalDate.now().getYear()){
            if(localDate.getYear()==LocalDate.now().getYear()){
                if(localDate.getMonthValue()<LocalDate.now().getMonthValue()){
                    f=1;
                }else if(localDate.getMonthValue()>LocalDate.now().getMonthValue()){
                    f=0;
                }else {
                    if(localDate.getDayOfMonth()>LocalDate.now().getDayOfMonth()){
                        f=0;
                    }else {
                        f=1;
                    }
                }
            }else{
                f=0;
            }
        }else {
            f=1;
        }
        if(f==1){
            throw new UpdateException("课程快临近结束或已经结束");
        }
        //看看是否和老师自己的课有冲突
        List<CourseDO> courseDOList=courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id",addTaskCO.getTeacherId()));
        List<Integer> courseIds=courseDOList.stream().map(CourseDO::getId).toList();
        if(CollectionUtil.isNotEmpty(courseIds)) {
            List<CourInfDO> courInfDOList = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id", courseIds));
            for (int i = 0; i < courInfDOList.size(); i++) {
                if (courInfDO.getWeek().equals(courInfDOList.get(i).getWeek())) {
                    if (courInfDO.getDay().equals(courInfDOList.get(i).getDay())) {
                        if (((courInfDO.getStartTime() <= courInfDOList.get(i).getEndTime()) && (courInfDO.getEndTime() >= courInfDOList.get(i).getStartTime()))
                                || ((courInfDOList.get(i).getStartTime() <= courInfDO.getEndTime()) && (courInfDOList.get(i).getEndTime() >= courInfDO.getStartTime()))) {
                            throw new UpdateException("与你其他课程冲突");
                        }
                    }
                }
            }
        }
        //看看是否和老师其他评教任务有冲突
        List<EvaTaskDO> evaTaskDOList=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("teacher_id",addTaskCO.getTeacherId()));
        List<Integer> courInfoIds=evaTaskDOList.stream().map(EvaTaskDO::getCourInfId).toList();
        if(CollectionUtil.isNotEmpty(courInfoIds)) {
            List<CourInfDO> evaCourInfDOList = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("id", courInfoIds));
            for (int i = 0; i < evaCourInfDOList.size(); i++) {
                if (courInfDO.getWeek().equals(evaCourInfDOList.get(i).getWeek())) {
                    if (courInfDO.getDay().equals(evaCourInfDOList.get(i).getDay())) {
                        if ((courInfDO.getStartTime() <= evaCourInfDOList.get(i).getEndTime() && courInfDO.getEndTime() >= evaCourInfDOList.get(i).getStartTime())
                        ||(evaCourInfDOList.get(i).getStartTime()<=courInfDO.getEndTime()&&evaCourInfDOList.get(i).getEndTime()>=courInfDO.getStartTime())){
                            throw new UpdateException("与你其他任务所上课程冲突");
                        }
                    }
                }
            }
        }
        EvaTaskDO evaTaskDO=new EvaTaskDO();
        evaTaskDO.setCreateTime(LocalDateTime.now());
        evaTaskDO.setUpdateTime(LocalDateTime.now());
        evaTaskDO.setStatus(0);
        evaTaskDO.setCourInfId(addTaskCO.getCourInfId());
        evaTaskDO.setTeacherId(addTaskCO.getTeacherId());
        evaTaskDO.setIsDeleted(0);
        evaTaskMapper.insert(evaTaskDO);

        Integer taskId=evaTaskMapper.selectOne(new QueryWrapper<EvaTaskDO>().eq("teacher_id",addTaskCO.getTeacherId()).eq("cour_inf_id",addTaskCO.getCourInfId()).eq("status",0)).getId();

        if(taskId==null){
            throw new QueryException("没有找到你的id");
        }
        return taskId;
    }

    @Override
    @Transactional
    public Void addEvaTemplate(EvaTemplateCO evaTemplateCO) throws ParseException {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        //判断指标重复
        if(evaTemplateCO.getProps()!=null) {
            List<String> props= Arrays.stream(evaTemplateCO.getProps().split(",")).toList();
            long count = props.stream().distinct().count();
            if (props.size() != count) {
                throw new UpdateException("由于你输入的指标中有重复数据，故不能增加");
            }
        }

        FormTemplateDO formTemplateDO=new FormTemplateDO();
        formTemplateDO.setDescription(evaTemplateCO.getDescription());
        formTemplateDO.setProps(evaTemplateCO.getProps());
        formTemplateDO.setIsDefault(evaTemplateCO.getIsDefault());
        formTemplateDO.setId(evaTemplateCO.getId());
        formTemplateDO.setIsDeleted(0);
        formTemplateDO.setName(evaTemplateCO.getName());
        formTemplateDO.setUpdateTime(LocalDateTime.parse(evaTemplateCO.getUpdateTime(),df));
        formTemplateDO.setCreateTime(LocalDateTime.parse(evaTemplateCO.getCreateTime(),df));
        formTemplateMapper.insert(formTemplateDO);
        LogUtils.logContent(formTemplateMapper.selectById(evaTemplateCO.getId()).getName() +" 评教模板");
        return null;
    }

    @Override
    @Transactional
    public Void cancelEvaTaskById(Integer id){
        //取消相应的评教任务
        UpdateWrapper<EvaTaskDO> evaTaskWrapper=new UpdateWrapper<>();
        evaTaskWrapper.eq("id",id);
        EvaTaskDO evaTaskDO=evaTaskMapper.selectById(id);
        evaTaskDO.setStatus(2);
        evaTaskMapper.update(evaTaskDO,evaTaskWrapper);
        return null;
    }
}
