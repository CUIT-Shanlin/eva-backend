package edu.cuit.infra.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;

/**
 * 查询相关工具类
 */
public class QueryUtils {

    public static <T,R extends GenericConditionalQuery> void fileTimeQuery(LambdaQueryWrapper<T> queryWrapper,
                                                                           R query,
                                                                           SFunction<T,?> createTimeProp,
                                                                           SFunction<T,?> updateTimeProp) {
        if (query.getStartCreateTime() != null) {
            queryWrapper.or().gt(createTimeProp,query.getStartCreateTime());
        }
        if (query.getEndCreateTime() != null) {
            queryWrapper.or().lt(createTimeProp,query.getEndCreateTime());
        }
        if (query.getStartUpdateTime() != null) {
            queryWrapper.or().gt(updateTimeProp,query.getStartUpdateTime());
        }
        if (query.getEndUpdateTime() != null) {
            queryWrapper.or().lt(updateTimeProp,query.getEndUpdateTime());
        }
    }

    public static <T,R extends GenericConditionalQuery> void fileTimeQuery(QueryWrapper<T> queryWrapper, R query) {
        if (query.getStartCreateTime() != null) {
            queryWrapper.or().gt("createTime",query.getStartCreateTime());
        }
        if (query.getEndCreateTime() != null) {
            queryWrapper.or().lt("createTime",query.getEndCreateTime());
        }
        if (query.getStartUpdateTime() != null) {
            queryWrapper.or().gt("updateTime",query.getStartUpdateTime());
        }
        if (query.getEndUpdateTime() != null) {
            queryWrapper.or().lt("updateTime",query.getEndUpdateTime());
        }
    }

}
