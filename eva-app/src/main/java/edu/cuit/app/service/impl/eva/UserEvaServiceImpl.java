package edu.cuit.app.service.impl.eva;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.cola.exception.SysException;
import edu.cuit.app.aop.CheckSemId;
import edu.cuit.app.convertor.eva.EvaRecordBizConvertor;
import edu.cuit.client.api.eva.IUserEvaService;
import edu.cuit.client.dto.clientobject.eva.EvaRecordCO;
import edu.cuit.domain.entity.eva.EvaRecordEntity;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.domain.gateway.eva.EvaDeleteGateway;
import edu.cuit.domain.gateway.eva.EvaQueryGateway;
import edu.cuit.domain.gateway.eva.EvaUpdateGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserEvaServiceImpl implements IUserEvaService {
    private final EvaQueryGateway evaQueryGateway;
    private final EvaRecordBizConvertor evaRecordBizConvertor;
    private final UserQueryGateway userQueryGateway;

    //去评教
    @Override
    @CheckSemId
    public List<EvaRecordCO> getEvaLogInfo(Integer semId, String keyword) {
        Integer userId=userQueryGateway.findIdByUsername(String.valueOf(StpUtil.getLoginId())).orElseThrow(() -> new SysException("该用户名不存在"));
        List<EvaRecordCO> evaRecordCOS=new ArrayList<>();
        if(userId==null){
            throw new SysException("还没有登录，怎么查到这里的");
        }
        List<EvaRecordEntity> evaRecordEntities=evaQueryGateway.getEvaLogInfo(userId,semId,keyword);
        if(evaRecordEntities.isEmpty()){
            throw new QueryException("并没有找到相关的评教记录");
        }
        for (EvaRecordEntity evaRecordEntity : evaRecordEntities) {
            EvaRecordCO evaRecordCO = evaRecordBizConvertor.evaRecordEntityToCo(evaRecordEntity);
            evaRecordCO.setAverScore(evaQueryGateway.getScoreFromRecord(evaRecordEntity.getFormPropsValues()).orElse(0.0));
            evaRecordCOS.add(evaRecordCO);
        }
        return evaRecordCOS;
    }
    //被评教
    @Override
    @CheckSemId
    public List<EvaRecordCO> getEvaLoggingInfo(Integer courseId, Integer semId) {
        Integer userId=userQueryGateway.findIdByUsername(String.valueOf(StpUtil.getLoginId())).orElseThrow(() -> new SysException("该用户名不存在"));
        if(userId==null){
            throw new SysException("还没有登录，怎么查到这里的");
        }
        List<EvaRecordEntity> evaRecordEntities=evaQueryGateway.getEvaEdLogInfo(userId,semId,courseId);
        List<EvaRecordCO> evaRecordCOS=new ArrayList<>();
        if(evaRecordEntities.isEmpty()){
            throw new QueryException("并没有找到相关的评教记录");
        }
        for (EvaRecordEntity evaRecordEntity : evaRecordEntities) {
            EvaRecordCO evaRecordCO = evaRecordBizConvertor.evaRecordEntityToCo(evaRecordEntity);

            evaRecordCO.setAverScore(evaQueryGateway.getScoreFromRecord(evaRecordEntity.getFormPropsValues()).orElse(0.0));
            evaRecordCOS.add(evaRecordCO);
        }
        return evaRecordCOS;
    }
}
