package com.hmall.gateway.routers;


import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@Component
@Slf4j
@RequiredArgsConstructor
public class DymicRouteLoader {
    private final NacosConfigManager nacosConfigManager;

    private final String dataId = "gateway-routes.json";

    private final String group = "DEFAULT_GROUP";

    private final RouteDefinitionWriter writer;

    private final Set<String> routeIds = new HashSet<>();
    //Bean初始化后执行方法
    @PostConstruct
    public void init() throws NacosException {
        //拉取配置
        String configInfo = nacosConfigManager.getConfigService()
                .getConfigAndSignListener(dataId, group, 5000, new Listener() {
                    //线程池
                    @Override
                    public Executor getExecutor() {
                        return null;
                    }

                    //监听配置变更
                    @Override
                    public void receiveConfigInfo(String s) {
                        updateConfig(s);
                    }
                });
        //第一次读取也要更新
        updateConfig(configInfo);

    }
    public void updateConfig(String configInfo){
        log.info("监听路由配置信息:{}",configInfo);
        List<RouteDefinition> routeDefinitions = JSONUtil.toList(configInfo, RouteDefinition.class);
        //删除旧路由表
        for(String routeId : routeIds){
            writer.delete(Mono.just(routeId)).subscribe();
        }
        routeIds.clear();
        for (RouteDefinition routeDefinition : routeDefinitions) {
            //存在则覆盖 不存在则新增
            writer.save(Mono.just(routeDefinition)).subscribe();
            //记录路由id，便于下次更新删除
            routeIds.add(routeDefinition.getId());
        }
    }
}
