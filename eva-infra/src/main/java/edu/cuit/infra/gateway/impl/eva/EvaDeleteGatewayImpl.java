package edu.cuit.infra.gateway.impl.eva;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import edu.cuit.domain.gateway.eva.EvaDeleteGateway;
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
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EvaDeleteGatewayImpl implements EvaDeleteGateway {
    private final FormRecordMapper formRecordMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final FormTemplateMapper formTemplateMapper;
    private final CourOneEvaTemplateMapper courOneEvaTemplateMapper;
    @Override
    public Void deleteEvaRecord(List<Integer> ids) {
        for (Integer id : ids) {
            UpdateWrapper<FormRecordDO> formRecordWrapper = new UpdateWrapper<>();
            formRecordWrapper.eq("id", id);
            formRecordMapper.delete(formRecordWrapper);
            //获取评教记录对应的id
            FormRecordDO formRecordDO=formRecordMapper.selectById(id);
            Integer taskId=formRecordDO.getTaskId();
            //删除对应的任务
            UpdateWrapper<EvaTaskDO> evaTaskWrapper =new UpdateWrapper<>();
            formRecordWrapper.eq("task_id",taskId);
        }
        return null;
    }

    @Override
    public Void deleteEvaTemplate(List<Integer> ids) {
        //TODO 怎么知道评教表单有没有用过

        for(Integer id : ids){
            //模板和快照模板id一样？(假如一样)
            QueryWrapper<CourOneEvaTemplateDO> courOneEvaTemplateWrapper =new QueryWrapper<>();
            courOneEvaTemplateWrapper.eq("id",id);
            //获取对应课程id
            CourOneEvaTemplateDO courOneEvaTemplateDO=courOneEvaTemplateMapper.selectById(id);
            Integer courseId=courOneEvaTemplateDO.getCourseId();
            //判断是不是在课程里面有
            if(courseId==null){
                UpdateWrapper<FormTemplateDO> formTemplateWrapper =new UpdateWrapper<>();
                formTemplateWrapper.eq("id",id);
                //删除模板
                formTemplateMapper.delete(formTemplateWrapper);
            }//TODO 没有的记得报错
        }
        return null;
    }
}
