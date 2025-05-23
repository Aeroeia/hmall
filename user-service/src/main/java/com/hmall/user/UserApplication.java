package com.hmall.user;

import client.ItemClient;
import config.DefaultFeignConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@MapperScan("com.hmall.user.mapper")
@SpringBootApplication
@EnableFeignClients(clients = {ItemClient.class},defaultConfiguration = DefaultFeignConfig.class)
public class UserApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }


    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}