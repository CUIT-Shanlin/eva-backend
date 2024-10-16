package edu.cuit.infra.gateway.impl.eva;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import edu.cuit.domain.gateway.eva.EvaDeleteGateway;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormTemplateDO;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormTemplateMapper;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final CourseMapper courseMapper;
    @Override
    @Transactional
    public Void deleteEvaRecord(List<Integer> ids) {
        for (Integer id : ids) {
            UpdateWrapper<FormRecordDO> formRecordWrapper = new UpdateWrapper<>();
            formRecordWrapper.eq("id", id);
            if(formRecordWrapper==null){
                throw new QueryException("可怜的人类，并未找到找到相应课程");
            }else {
                formRecordMapper.delete(formRecordWrapper);
            }
        }
        return null;
    }
    //ok
    @Override
    @Transactional
    public Void deleteEvaTemplate(List<Integer> ids) {
        for(Integer id : ids){
            //没有分配在课程中
            QueryWrapper<CourseDO> courWrapper =new QueryWrapper<>();
            courWrapper.eq("templateId",id);
            CourseDO courseDO=courseMapper.selectOne(courWrapper);
            //获取对应课程id
            if(courseDO==null){
                UpdateWrapper<FormTemplateDO> formTemplateWrapper = new UpdateWrapper<>();
                formTemplateWrapper.eq("id", id);
                if(formTemplateWrapper==null){
                    throw new QueryException("可怜的人类，并未找到找到相应课程");
                }else{
                    //删除模板
                    formTemplateMapper.delete(formTemplateWrapper);
                }
            }else{
                throw new UpdateException("可怜的人类，该模板已经被课程分配，无法再进行删除");
            }
        }
        return null;
    }
}
