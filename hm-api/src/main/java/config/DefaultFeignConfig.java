package config;

import feign.Logger;
import org.springframework.context.annotation.Bean;


public class DefaultFeignConfig {
    @Bean
    public Logger.Level fallbackLogger() {
        return Logger.Level.FULL;
    }
}
