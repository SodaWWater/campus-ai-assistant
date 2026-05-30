package com.liminghan.campusai.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
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

    /**
     * 使用 JSON 序列化代替 Java 序列化，避免反序列化安全限制
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        return factory;
    }
}
