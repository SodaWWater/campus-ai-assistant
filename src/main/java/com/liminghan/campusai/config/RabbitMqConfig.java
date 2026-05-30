package com.liminghan.campusai.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RabbitMqConfig {

    @Value("${app.mq.document-exchange}")
    private String documentExchange;

    @Value("${app.mq.document-queue}")
    private String documentQueue;

    @Value("${app.mq.document-routing-key}")
    private String documentRoutingKey;

    @Value("${app.mq.dlx-exchange}")
    private String dlxExchange;

    @Value("${app.mq.dlx-routing-key}")
    private String dlxRoutingKey;

    @Value("${app.mq.dlq-queue}")
    private String dlqQueue;

    @Value("${app.mq.max-retry-count:3}")
    private int maxRetryCount;

    // ═══════════════════ 主交换机 & 队列 ═══════════════════

    @Bean
    public DirectExchange documentDirectExchange() {
        return new DirectExchange(documentExchange, true, false);
    }

    /**
     * 主队列绑定死信交换机，消息被 reject/nack 或 TTL 超时后自动路由到 DLQ
     */
    @Bean
    public Queue documentProcessQueue() {
        return QueueBuilder.durable(documentQueue)
                .deadLetterExchange(dlxExchange)
                .deadLetterRoutingKey(dlxRoutingKey)
                .build();
    }

    @Bean
    public Binding documentProcessBinding(Queue documentProcessQueue, DirectExchange documentDirectExchange) {
        return BindingBuilder.bind(documentProcessQueue).to(documentDirectExchange).with(documentRoutingKey);
    }

    // ═══════════════════ 死信交换机 & 死信队列 ═══════════════════

    @Bean
    public DirectExchange documentDlxExchange() {
        return new DirectExchange(dlxExchange, true, false);
    }

    @Bean
    public Queue documentDlqQueue() {
        return new Queue(dlqQueue, true);
    }

    @Bean
    public Binding documentDlqBinding(Queue documentDlqQueue, DirectExchange documentDlxExchange) {
        return BindingBuilder.bind(documentDlqQueue).to(documentDlxExchange).with(dlxRoutingKey);
    }

    // ═══════════════════ RetryTemplate：3 次重试 + 指数退避 ═══════════════════

    @Bean
    public RetryTemplate documentProcessRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(maxRetryCount);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);   // 首次重试等待 1s
        backOffPolicy.setMultiplier(2.0);         // 指数倍增：1s → 2s → 4s
        backOffPolicy.setMaxInterval(10000);      // 最大间隔 10s

        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }

    // ═══════════════════ ListenerContainerFactory ═══════════════════

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter,
            RabbitTemplate rabbitTemplate) {

        // 重试耗尽后自动发布到 DLQ
        RepublishMessageRecoverer recoverer = new RepublishMessageRecoverer(
                rabbitTemplate, dlxExchange, dlxRoutingKey);

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setPrefetchCount(1);
        // RetryInterceptor: 3 次重试后 RepublishMessageRecoverer 投递到 DLQ
        factory.setAdviceChain(RetryInterceptorBuilder
                .stateless()
                .retryOperations(documentProcessRetryTemplate())
                .recoverer(recoverer)
                .build());

        return factory;
    }

    // ═══════════════════ 消息序列化 ═══════════════════

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
}
