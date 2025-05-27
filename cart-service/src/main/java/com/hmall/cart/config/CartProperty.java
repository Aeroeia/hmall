package com.hmall.cart.config;

import io.lettuce.core.dynamic.annotation.CommandNaming;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "hm.cart")
@Data
public class CartProperty {
    private Integer maxItems;
}
