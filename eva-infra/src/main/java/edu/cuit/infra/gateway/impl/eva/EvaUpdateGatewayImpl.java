package edu.cuit.infra.gateway.impl.eva;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import edu.cuit.client.dto.clientobject.eva.EvaInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskFormCO;
import edu.cuit.client.dto.clientobject.eva.EvaTemplateCO;
import edu.cuit.domain.entity.eva.EvaTaskEntity;
import edu.cuit.domain.entity.eva.EvaTemplateEntity;
import edu.cuit.domain.gateway.eva.EvaUpdateGateway;
import edu.cuit.infra.convertor.eva.EvaConvertor;
import edu.cuit.infra.dal.database.dataobject.eva.*;
import edu.cuit.infra.dal.database.mapper.eva.*;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Update;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class EvaUpdateGatewayImpl implements EvaUpdateGateway {
    private final FormTemplateMapper formTemplateMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final EvaConvertor evaConvertor;
    private final FormRecordMapper formRecordMapper;
    private final CourOneEvaTemplateMapper courOneEvaTemplateMapper;
    private final MsgTipMapper msgTipMapper;
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
        msgTipMapper.delete(new QueryWrapper<MsgTipDO>().eq("task_id",evaTaskFormCO.getTaskId()).eq("type",0));
        msgTipMapper.delete(new QueryWrapper<MsgTipDO>().eq("task_id",evaTaskFormCO.getTaskId()).eq("type",2));

        return null;
    }

    @Override
    @Transactional
    //TODO 同时发送该任务的评教待办消息;
    public Void postEvaTask(EvaInfoCO evaInfoCO) {
        EvaTaskDO evaTaskDO=new EvaTaskDO();
        evaTaskDO.setCreateTime(LocalDateTime.now());
        evaTaskDO.setUpdateTime(LocalDateTime.now());
        evaTaskDO.setStatus(0);
        evaTaskDO.setCourInfId(evaInfoCO.getCourInfId());
        evaTaskDO.setTeacherId(evaInfoCO.getTeacherId());
        evaTaskMapper.insert(evaTaskDO);
        //同时发送该任务的评教待办消息;TODO朱还在写相关接口

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
