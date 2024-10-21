package edu.cuit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 程序主入口
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class,args);
    }

}
