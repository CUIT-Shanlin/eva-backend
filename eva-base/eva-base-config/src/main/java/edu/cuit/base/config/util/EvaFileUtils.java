package edu.cuit.base.config.util;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ClassUtil;

import java.io.File;

/**
 * 文件相关工具类
 */
public class EvaFileUtils {

    /**
     * 获取项目根目录
     */
    public static File getRootDirectory() {
        ClassUtil.getClassLoader().getResource("");
        //TODO  获取根路径
        return null;
    }

}
