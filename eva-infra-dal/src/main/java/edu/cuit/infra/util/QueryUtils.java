package edu.cuit.infra.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.cuit.client.dto.query.PagingQuery;
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
            queryWrapper.and(queryWrp -> {
               queryWrp.gt(createTimeProp,query.getStartCreateTime())
                       .or().eq(createTimeProp,query.getStartCreateTime());
            });
        }
        if (query.getEndCreateTime() != null) {
            queryWrapper.and(queryWrp -> {
                queryWrp.lt(createTimeProp,query.getEndCreateTime())
                        .or().eq(createTimeProp,query.getEndCreateTime());
            });
        }
        if (query.getStartUpdateTime() != null) {
            queryWrapper.and(queryWrp -> {
                queryWrp.gt(updateTimeProp,query.getStartUpdateTime())
                        .or().eq(updateTimeProp,query.getStartUpdateTime());
            });
        }
        if (query.getEndUpdateTime() != null) {
            queryWrapper.and(queryWrp -> {
                queryWrp.lt(updateTimeProp,query.getEndUpdateTime())
                        .or().eq(updateTimeProp,query.getEndUpdateTime());
            });
        }
    }

    public static <T,R extends GenericConditionalQuery> void fileTimeQuery(QueryWrapper<T> queryWrapper, R query) {
        fileCreateTimeQuery(queryWrapper,query);
        if (query.getStartUpdateTime() != null) {
            queryWrapper.and(queryWrp -> {
                queryWrp.gt("update_time",query.getStartUpdateTime())
                        .or().eq("update_time",query.getStartUpdateTime());
            });

        }
        if (query.getEndUpdateTime() != null) {
            queryWrapper.and(queryWrp -> {
                queryWrp.lt("update_time",query.getEndUpdateTime())
                        .or().eq("update_time",query.getEndUpdateTime());
            });

        }
    }

    public static <T,R extends GenericConditionalQuery> void fileCreateTimeQuery(QueryWrapper<T> queryWrapper,R query) {
        if (query.getStartCreateTime() != null) {
            queryWrapper.and(queryWrp -> {
                queryWrp.gt("create_time",query.getStartCreateTime())
                        .or().eq("create_time",query.getStartCreateTime());
            });
        }
        if (query.getEndCreateTime() != null) {
            queryWrapper.and(queryWrp -> {
                queryWrp.lt("create_time",query.getEndCreateTime())
                        .or().eq("create_time",query.getEndCreateTime());
            });
        }

    }

    public static <T> Page<T> createPage(PagingQuery<?> pageQuery) {
        return Page.of(pageQuery.getPage(),pageQuery.getSize());
    }

}
