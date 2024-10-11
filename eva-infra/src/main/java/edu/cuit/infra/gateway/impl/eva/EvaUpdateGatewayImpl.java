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
import edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormTemplateDO;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Update;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class EvaUpdateGatewayImpl implements EvaUpdateGateway {
    private final FormTemplateMapper formTemplateMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final EvaConvertor evaConvertor;
    private final FormRecordMapper formRecordMapper;
    private final CourOneEvaTemplateMapper courOneEvaTemplateMapper;
    @Override
    public Void updateEvaTemplate(EvaTemplateCO evaTemplateCO) {
        //获取对应课程id
        CourOneEvaTemplateDO courOneEvaTemplateDO=courOneEvaTemplateMapper.selectById(evaTemplateCO.getId());
        Integer courseId=courOneEvaTemplateDO.getCourseId();
        //课程id为空则说明没有被占用?TODO
        if(courseId==null) {
            FormTemplateDO formTemplateDO = evaConvertor.ToEvaTemplateDO(evaTemplateCO);
            formTemplateDO.setUpdateTime(LocalDateTime.now());
            formTemplateMapper.update(formTemplateDO, new QueryWrapper<FormTemplateDO>().eq("id", evaTemplateCO.getId()));
        }
        return null;
    }

    @Override
    public Void putEvaTemplate(EvaTaskFormCO evaTaskFormCO) {
        //把评教的具体数据传进去给
        FormTemplateDO formTemplateDO=new FormTemplateDO();
        formTemplateDO.setProps(evaTaskFormCO.getFormPropsValues());
        formTemplateDO.setDescription(evaTaskFormCO.getTextValue());
        formTemplateDO.setUpdateTime(LocalDateTime.now());
        //通过任务id给他找到模板id修改
        //TODO
        formTemplateMapper.update(formTemplateDO,new QueryWrapper<FormTemplateDO>().eq("id",));
        //通过任务id把任务状态改了
        EvaTaskDO evaTaskDO=evaTaskMapper.selectById(evaTaskFormCO.getTaskId());
        evaTaskDO.setStatus(1);
        evaTaskDO.setUpdateTime(LocalDateTime.now());
        evaTaskMapper.update(evaTaskDO,new QueryWrapper<EvaTaskDO>().eq("id",evaTaskFormCO.getTaskId()));
        return null;
    }

    @Override
    public Void postEvaTask(EvaInfoCO evaInfoCO) {
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
    public Void addEvaTemplate(EvaTemplateCO evaTemplateCO) {
        FormTemplateDO formTemplateDO=evaConvertor.ToEvaTemplateDO(evaTemplateCO);
        formTemplateDO.setUpdateTime(LocalDateTime.now());
        formTemplateDO.setCreateTime(LocalDateTime.now());
        formTemplateMapper.insert(formTemplateDO);
        return null;
    }

    @Override
    public Void cancelEvaTaskById(Integer id){
        //取消相应的评教任务
        UpdateWrapper<EvaTaskDO> evaTaskWrapper=new UpdateWrapper<>();
        evaTaskWrapper.eq("id",id);
        evaTaskMapper.delete(evaTaskWrapper);
        //取消相应的评教记录
        UpdateWrapper<FormRecordDO> formRecordWrapper=new UpdateWrapper<>();
        formRecordWrapper.eq("task_id",id);
        return null;
    }
}
