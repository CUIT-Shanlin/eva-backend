package edu.cuit;

import cn.dev33.satoken.dao.SaTokenDaoRedissonJackson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 程序主入口
 */
@SpringBootApplication(exclude = {SaTokenDaoRedissonJackson.class})
@ConfigurationPropertiesScan
@EnableAsync
@EnableTransactionManagement
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class,args);
    }

}
