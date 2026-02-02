package edu.cuit.app.service.impl.eva;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.cola.exception.SysException;
import edu.cuit.app.aop.CheckSemId;
import edu.cuit.bc.iam.application.port.UserBasicQueryPort;
import edu.cuit.bc.evaluation.application.usecase.UserEvaQueryUseCase;
import edu.cuit.client.api.eva.IUserEvaService;
import edu.cuit.client.dto.clientobject.eva.EvaRecordCO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserEvaServiceImpl implements IUserEvaService {
    private final UserEvaQueryUseCase userEvaQueryUseCase;
    private final UserBasicQueryPort userBasicQueryPort;

    //去评教
    @Override
    @CheckSemId
    public List<EvaRecordCO> getEvaLogInfo(Integer semId, String keyword) {
        Integer userId=userBasicQueryPort.findIdByUsername(String.valueOf(StpUtil.getLoginId())).orElseThrow(() -> new SysException("该用户名不存在"));
        if(userId==null){
            throw new SysException("还没有登录，怎么查到这里的");
        }
        return userEvaQueryUseCase.getEvaLogInfo(userId, semId, keyword);
    }
    //被评教
    @Override
    @CheckSemId
    public List<EvaRecordCO> getEvaLoggingInfo(Integer courseId, Integer semId) {
        Integer userId=userBasicQueryPort.findIdByUsername(String.valueOf(StpUtil.getLoginId())).orElseThrow(() -> new SysException("该用户名不存在"));
        if(userId==null){
            throw new SysException("还没有登录，怎么查到这里的");
        }
        return userEvaQueryUseCase.getEvaLoggingInfo(userId, courseId, semId);
    }
}
