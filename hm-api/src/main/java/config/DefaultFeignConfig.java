package config;

import com.hmall.common.utils.UserContext;
import fallback.ItemClientFallbackFactory;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;


public class DefaultFeignConfig {
    @Bean
    public Logger.Level fallbackLogger() {
        return Logger.Level.FULL;
    }

    @Bean
    public RequestInterceptor userInfoRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                requestTemplate.header("user-Info", UserContext.getUser().toString());
            }
        };
    }
    @Bean
    public ItemClientFallbackFactory itemClientFallbackFactory(){
        return new ItemClientFallbackFactory();
    }
}
