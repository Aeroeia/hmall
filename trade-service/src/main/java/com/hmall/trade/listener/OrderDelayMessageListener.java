package com.hmall.trade.listener;

import client.PayClient;
import client.TradeClient;
import com.hmall.trade.constants.MQConstans;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.service.IOrderService;
import domain.dto.PayOrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDelayMessageListener {
    private final PayClient payClient;
    private final IOrderService orderService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQConstans.DELAY_QUEUE_NAME, durable = "true"),
            exchange = @Exchange(name = MQConstans.DELAY_EXCHANGE_NAME, type = ExchangeTypes.DIRECT, delayed = "true"),
            key = {MQConstans.DELAY_ORDER_KEY}
    ))
    public void delayListener(Long id) {
        log.info("订单延迟队列监听到消息:{}", id);
        Order order = orderService.getById(id);
        if (order == null || order.getStatus() != 1) {
            return;
        }
        PayOrderDTO payOrderDTO = payClient.queryPayOrderByBizOrderNo(id);
        if (payOrderDTO == null || payOrderDTO.getStatus() == 3) {
            orderService.markOrderPaySuccess(id);
            return;
        }
        orderService.cancelOrder(id);
    }

}
