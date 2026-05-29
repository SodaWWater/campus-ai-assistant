package com.liminghan.campusai.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Value("${app.mq.document-exchange}")
    private String documentExchange;

    @Value("${app.mq.document-queue}")
    private String documentQueue;

    @Value("${app.mq.document-routing-key}")
    private String documentRoutingKey;

    @Bean
    public DirectExchange documentDirectExchange() {
        return new DirectExchange(documentExchange, true, false);
    }

    @Bean
    public Queue documentProcessQueue() {
        return new Queue(documentQueue, true);
    }

    @Bean
    public Binding documentProcessBinding(Queue documentProcessQueue, DirectExchange documentDirectExchange) {
        return BindingBuilder.bind(documentProcessQueue).to(documentDirectExchange).with(documentRoutingKey);
    }
}
