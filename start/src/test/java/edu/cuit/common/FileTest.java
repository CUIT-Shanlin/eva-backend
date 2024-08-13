package edu.cuit.common;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ClassUtil;
import edu.cuit.Application;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = Application.class)
public class FileTest {

    @Test
    public void testGetRootUrl() {
        System.out.println(ResourceUtil.getResource("").getFile());
        System.out.println(ClassUtil.getClassLoader().getResource("").getPath());
    }

}
