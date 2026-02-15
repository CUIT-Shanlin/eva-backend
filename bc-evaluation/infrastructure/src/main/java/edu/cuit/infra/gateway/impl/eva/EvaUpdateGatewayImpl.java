package edu.cuit.infra.gateway.impl.eva;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import edu.cuit.bc.course.application.port.CourseIdByCourInfIdQueryPort;
import edu.cuit.domain.gateway.eva.EvaUpdateGateway;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.cache.aspect.annotation.local.LocalCacheInvalidate;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.lang.reflect.Method;

@Component
@RequiredArgsConstructor
public class EvaUpdateGatewayImpl implements EvaUpdateGateway {
    private final EvaTaskMapper evaTaskMapper;
    private final CourseIdByCourInfIdQueryPort courseIdByCourInfIdQueryPort;
    private final CourseMapper courseMapper;
    @Autowired
    @Qualifier("sysUserMapper")
    private Object sysUserMapper;
    private final EvaCacheConstants evaCacheConstants;
    private final LocalCacheManager localCacheManager;

    @Override
    @Transactional
    @LocalCacheInvalidate(area="#{@evaCacheConstants.ONE_TASK}", key="#id")
    public Void cancelEvaTaskById(Integer id){
        //取消相应的评教任务
        UpdateWrapper<EvaTaskDO> evaTaskWrapper=new UpdateWrapper<>();
        evaTaskWrapper.eq("id",id);
        EvaTaskDO evaTaskDO=evaTaskMapper.selectById(id);
        evaTaskDO.setStatus(2);
        evaTaskMapper.update(evaTaskDO,evaTaskWrapper);
        Integer courseId = courseIdByCourInfIdQueryPort.findCourseIdByCourInfId(evaTaskDO.getCourInfId()).orElse(null);
        localCacheManager.invalidateCache(
                evaCacheConstants.TASK_LIST_BY_SEM,
                String.valueOf(courseMapper.selectById(courseId).getSemesterId())
        );
        localCacheManager.invalidateCache(
                evaCacheConstants.TASK_LIST_BY_TEACH,
                selectSysUserNameById(evaTaskDO.getTeacherId())
        );
        return null;
    }

    private String selectSysUserNameById(Serializable userId) {
        try {
            Method selectById = sysUserMapper.getClass().getMethod("selectById", Serializable.class);
            Object sysUser = selectById.invoke(sysUserMapper, userId);
            Method getName = sysUser.getClass().getMethod("getName");
            return (String) getName.invoke(sysUser);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
