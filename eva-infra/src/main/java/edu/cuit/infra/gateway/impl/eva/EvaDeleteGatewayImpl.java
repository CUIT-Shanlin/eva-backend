package edu.cuit.infra.gateway.impl.eva;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import edu.cuit.domain.gateway.eva.EvaDeleteGateway;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormTemplateDO;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormTemplateMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EvaDeleteGatewayImpl implements EvaDeleteGateway {
    private final FormRecordMapper formRecordMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final FormTemplateMapper formTemplateMapper;
    private final CourOneEvaTemplateMapper courOneEvaTemplateMapper;
    private final CourseMapper courseMapper;
    private final SysUserMapper sysUserMapper;
    @Override
    @Transactional
    public Void deleteEvaRecord(List<Integer> ids) {
        for (Integer id : ids) {
            QueryWrapper<FormRecordDO> formRecordWrapper = new QueryWrapper<>();
            formRecordWrapper.eq("id", id);
            if(formRecordMapper.selectOne(formRecordWrapper)==null){
                throw new QueryException("并未找到找到相应评教记录");
            }else {
                FormRecordDO formRecordDO=formRecordMapper.selectById(id);
                LogUtils.logContent(sysUserMapper.selectById(evaTaskMapper.selectById(formRecordDO.getTaskId()).getTeacherId()).getName()
                        +" 用户评教任务ID为"+formRecordDO.getTaskId()+"的评教记录");
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
            //是否是默认数据
            FormTemplateDO formTemplateDO=formTemplateMapper.selectById(id);
            if(formTemplateDO.getIsDefault()==1||formTemplateDO.getIsDefault()==0){
                throw new UpdateException("默认数据不允许删除");
            }
            //没有分配在课程中
            QueryWrapper<CourseDO> courWrapper =new QueryWrapper<>();
            courWrapper.eq("templateId",id);
            CourseDO courseDO=courseMapper.selectOne(courWrapper);
            //获取对应课程id
            if(courseDO==null){
                QueryWrapper<FormTemplateDO> formTemplateWrapper = new QueryWrapper<>();
                formTemplateWrapper.eq("id", id);
                if(formTemplateMapper.selectOne(formTemplateWrapper)==null){
                    throw new QueryException("并未找到找到相应模板");
                }else{
                    //删除模板
                    LogUtils.logContent(formTemplateMapper.selectById(id).getName() +" 评教模板");
                    formTemplateMapper.delete(formTemplateWrapper);
                }
            }else{
                throw new UpdateException("该模板已经被课程分配，无法再进行删除");
            }
        }
        return null;
    }
}
