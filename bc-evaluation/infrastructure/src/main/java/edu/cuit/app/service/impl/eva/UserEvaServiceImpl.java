package edu.cuit.app.service.impl.eva;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.cola.exception.SysException;
import edu.cuit.app.aop.CheckSemId;
import edu.cuit.bc.evaluation.application.usecase.UserEvaQueryUseCase;
import edu.cuit.client.api.eva.IUserEvaService;
import edu.cuit.client.dto.clientobject.eva.EvaRecordCO;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.domain.gateway.eva.EvaDeleteGateway;
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
    private final UserEvaQueryUseCase userEvaQueryUseCase;
    private final UserQueryGateway userQueryGateway;

    //去评教
    @Override
    @CheckSemId
    public List<EvaRecordCO> getEvaLogInfo(Integer semId, String keyword) {
        Integer userId=userQueryGateway.findIdByUsername(String.valueOf(StpUtil.getLoginId())).orElseThrow(() -> new SysException("该用户名不存在"));
        if(userId==null){
            throw new SysException("还没有登录，怎么查到这里的");
        }
        return userEvaQueryUseCase.getEvaLogInfo(userId, semId, keyword);
    }
    //被评教
    @Override
    @CheckSemId
    public List<EvaRecordCO> getEvaLoggingInfo(Integer courseId, Integer semId) {
        Integer userId=userQueryGateway.findIdByUsername(String.valueOf(StpUtil.getLoginId())).orElseThrow(() -> new SysException("该用户名不存在"));
        if(userId==null){
            throw new SysException("还没有登录，怎么查到这里的");
        }
        return userEvaQueryUseCase.getEvaLoggingInfo(userId, courseId, semId);
    }
}
