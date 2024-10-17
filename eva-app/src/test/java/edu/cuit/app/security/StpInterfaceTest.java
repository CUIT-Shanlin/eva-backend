package edu.cuit.app.security;

import cn.dev33.satoken.stp.StpInterface;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class StpInterfaceTest {

    @Autowired
    private StpInterface stpInterface;

    @Test
    public void testPerm() {
        long start = System.currentTimeMillis();
        System.out.println(stpInterface.getPermissionList("admin",""));
        System.out.println("take " + (System.currentTimeMillis() - start) + " ms");
    }

}
