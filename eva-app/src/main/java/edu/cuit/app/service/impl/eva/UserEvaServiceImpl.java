package edu.cuit.app.service.impl.eva;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.cola.exception.BizException;
import com.alibaba.cola.exception.SysException;
import edu.cuit.app.convertor.user.MenuBizConvertor;
import edu.cuit.client.api.eva.IUserEvaService;
import edu.cuit.client.api.user.IMenuService;
import edu.cuit.client.dto.clientobject.eva.EvaRecordCO;
import edu.cuit.client.dto.clientobject.user.GenericMenuSectionCO;
import edu.cuit.client.dto.clientobject.user.MenuCO;
import edu.cuit.client.dto.cmd.user.NewMenuCmd;
import edu.cuit.client.dto.cmd.user.UpdateMenuCmd;
import edu.cuit.client.dto.data.course.CourseTime;
import edu.cuit.client.dto.query.condition.MenuConditionalQuery;
import edu.cuit.domain.entity.eva.EvaRecordEntity;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.domain.gateway.eva.EvaDeleteGateway;
import edu.cuit.domain.gateway.eva.EvaQueryGateway;
import edu.cuit.domain.gateway.eva.EvaUpdateGateway;
import edu.cuit.domain.gateway.user.MenuQueryGateway;
import edu.cuit.domain.gateway.user.MenuUpdateGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserEvaServiceImpl implements IUserEvaService {private final EvaDeleteGateway evaDeleteGateway;
    private final EvaUpdateGateway evaUpdateGateway;
    private final EvaQueryGateway evaQueryGateway;
    private final UserQueryGateway userQueryGateway;
    private final CourseQueryGateway courseQueryGateway;

    //怎么获取自己的 TODO
    @Override
    public List<EvaRecordCO> getEvaLogInfo(Integer semId, String keyword) {
        Integer userId= (Integer) StpUtil.getLoginId();
        if(userId==null){
            throw new SysException("还没有登录，怎么查到这里的");
        }
        List<EvaRecordEntity> evaRecordEntities=evaQueryGateway.getEvaLogInfo(userId,semId,keyword);
        for(int i=0;i<evaRecordEntities.size();i++){
            EvaRecordCO evaRecordCO=new EvaRecordCO();
            evaRecordCO.setId(evaRecordEntities.get(i).getId());
            evaRecordCO.setEvaTeacherName(userQueryGateway.findUsernameById(userId).get());


            evaRecordCO.setCourseName(courseQueryGateway.getCourseByInfo
                            (evaRecordEntities.get(i).getTask().getCourInf().getId())
                    .get().getSubjectEntity().getName());
            evaRecordCO.setAverScore(evaQueryGateway.getScoreFromRecord(evaRecordEntities.get(i).getFormPropsValues()).get());

            evaRecordCO.getCourseTime();
        }


        return null;
    }

    @Override
    public List<EvaRecordCO> getEvaLoggingInfo(Integer courseId, Integer semId) {
        return null;
    }
}
