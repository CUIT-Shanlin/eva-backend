package edu.cuit.infra.gateway.impl.eva;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import edu.cuit.client.dto.clientobject.eva.EvaInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskFormCO;
import edu.cuit.client.dto.clientobject.eva.EvaTemplateCO;
import edu.cuit.domain.entity.eva.EvaTaskEntity;
import edu.cuit.domain.entity.eva.EvaTemplateEntity;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.domain.gateway.eva.EvaUpdateGateway;
import edu.cuit.infra.convertor.eva.EvaConvertor;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.eva.*;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.eva.*;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Update;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EvaUpdateGatewayImpl implements EvaUpdateGateway {
    private final FormTemplateMapper formTemplateMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final EvaConvertor evaConvertor;
    private final FormRecordMapper formRecordMapper;
    private final CourInfMapper courInfMapper;
    private final CourseMapper courseMapper;
    private final CourseQueryGateway courseQueryGateway;
    @Override
    @Transactional
    public Void updateEvaTemplate(EvaTemplateCO evaTemplateCO) {
        FormTemplateDO formTemplateDO = evaConvertor.ToFormTemplateDO(evaTemplateCO);
        formTemplateDO.setUpdateTime(LocalDateTime.now());
        formTemplateMapper.update(formTemplateDO, new QueryWrapper<FormTemplateDO>().eq("id", evaTemplateCO.getId()));
        return null;
    }
    @Override
    @Transactional
    //TODO 要删除对应的两种消息 “该任务的待办评教消息” “该任务的系统逾期提醒消息”
    public Void putEvaTemplate(EvaTaskFormCO evaTaskFormCO) {
        //把评教的具体数据传进去给评教记录
        FormRecordDO formRecordDO=evaConvertor.ToFormRecordDO(evaTaskFormCO);
        formRecordDO.setCreateTime(LocalDateTime.now());
        formRecordMapper.insert(formRecordDO);

        //通过任务id把任务状态改了
        EvaTaskDO evaTaskDO=evaTaskMapper.selectById(evaTaskFormCO.getTaskId());
        evaTaskDO.setStatus(1);
        evaTaskDO.setUpdateTime(LocalDateTime.now());
        evaTaskMapper.update(evaTaskDO,new QueryWrapper<EvaTaskDO>().eq("id",evaTaskFormCO.getTaskId()));

        //删除对应的两种消息 “该任务的待办评教消息” “该任务的系统逾期提醒消息” 根据type判断？
        //msgTipMapper.delete(new QueryWrapper<MsgTipDO>().eq("task_id",evaTaskFormCO.getTaskId()).eq("type",0));
        //msgTipMapper.delete(new QueryWrapper<MsgTipDO>().eq("task_id",evaTaskFormCO.getTaskId()).eq("type",2));

        return null;
    }

    @Override
    @Transactional
    //TODO 同时发送该任务的评教待办消息;
    public Void postEvaTask(EvaInfoCO evaInfoCO) {
        //同时发送该任务的评教待办消息;TODO 朱还在写相关接口
        CourInfDO courInfDO=courInfMapper.selectById(evaInfoCO.getCourInfId());
        //看看是否和老师自己的课有冲突
        List<CourseDO> courseDOList=courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id",evaInfoCO.getTeacherId()));
        List<Integer> courseIds=courseDOList.stream().map(CourseDO::getId).toList();

        List<CourInfDO> courInfDOList=courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id",courseIds));
        for(int i=0;i<courInfDOList.size();i++){
            if(courInfDO.getWeek().equals(courInfDOList.get(i).getWeek())){
                if(courInfDO.getDay().equals(courInfDOList.get(i).getDay())){
                    if(courInfDO.getStartTime()<courInfDOList.get(i).getEndTime()||courInfDO.getEndTime()>courInfDOList.get(i).getStartTime()){
                        throw new UpdateException("与你其他课程冲突");
                    }
                }
            }
        }
        //看看是否和老师其他评教任务有冲突
        List<EvaTaskDO> evaTaskDOList=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("teacher_id",evaInfoCO.getTeacherId()));
        List<Integer> courInfoIds=evaTaskDOList.stream().map(EvaTaskDO::getCourInfId).toList();

        List<CourInfDO> evaCourInfDOList=courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("id",courInfoIds));
        for(int i=0;i<evaCourInfDOList.size();i++){
            if(courInfDO.getWeek().equals(evaCourInfDOList.get(i).getWeek())){
                if(courInfDO.getDay().equals(evaCourInfDOList.get(i).getDay())){
                    if(courInfDO.getStartTime()>=evaCourInfDOList.get(i).getEndTime()||courInfDO.getEndTime()<=evaCourInfDOList.get(i).getStartTime()){
                        throw new UpdateException("与你其他任务所上课程冲突");
                    }
                }
            }
        }
        //看看是不是正在没有上完的课程 TODO
        EvaTaskDO evaTaskDO=new EvaTaskDO();
        evaTaskDO.setCreateTime(LocalDateTime.now());
        evaTaskDO.setUpdateTime(LocalDateTime.now());
        evaTaskDO.setStatus(0);
        evaTaskDO.setCourInfId(evaInfoCO.getCourInfId());
        evaTaskDO.setTeacherId(evaInfoCO.getTeacherId());
        evaTaskMapper.insert(evaTaskDO);
        return null;
    }

    @Override
    @Transactional
    public Void addEvaTemplate(EvaTemplateCO evaTemplateCO) {
        FormTemplateDO formTemplateDO=evaConvertor.ToFormTemplateDO(evaTemplateCO);
        formTemplateDO.setUpdateTime(LocalDateTime.now());
        formTemplateDO.setCreateTime(LocalDateTime.now());
        formTemplateMapper.insert(formTemplateDO);
        return null;
    }

    @Override
    @Transactional
    public Void cancelEvaTaskById(Integer id){
        //取消相应的评教任务
        UpdateWrapper<EvaTaskDO> evaTaskWrapper=new UpdateWrapper<>();
        evaTaskWrapper.eq("id",id);
        evaTaskMapper.delete(evaTaskWrapper);
        return null;
    }
}
