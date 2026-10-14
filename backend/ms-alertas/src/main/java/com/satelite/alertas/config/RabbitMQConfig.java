package com.satelite.alertas.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "satelite.exchange";
    public static final String LOTE_QUEUE = "lote.vencimiento.queue";
    public static final String COTIZACION_QUEUE = "cotizacion.procesada.queue";
    public static final String LOTE_ROUTING_KEY_PATTERN = "lote.vencimiento.*";
    public static final String COTIZACION_ROUTING_KEY = "cotizacion.procesada";
    public static final String RESUMEN_QUEUE = "lote.resumen.queue";
    public static final String RESUMEN_ROUTING_KEY = "lote.resumen.diario";

    @Bean
    public TopicExchange sateliteExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue loteVencimientoQueue() {
        return QueueBuilder.durable(LOTE_QUEUE).build();
    }

    @Bean
    public Queue cotizacionProcesadaQueue() {
        return QueueBuilder.durable(COTIZACION_QUEUE).build();
    }

    @Bean
    public Queue loteResumenQueue() {
        return QueueBuilder.durable(RESUMEN_QUEUE).build();
    }

    @Bean
    public Binding resumenBinding(Queue loteResumenQueue, TopicExchange sateliteExchange) {
        return BindingBuilder.bind(loteResumenQueue)
                .to(sateliteExchange)
                .with(RESUMEN_ROUTING_KEY);
    }

    @Bean
    public Binding loteBinding(Queue loteVencimientoQueue, TopicExchange sateliteExchange) {
        return BindingBuilder.bind(loteVencimientoQueue)
                .to(sateliteExchange)
                .with(LOTE_ROUTING_KEY_PATTERN);
    }

    @Bean
    public Binding cotizacionBinding(Queue cotizacionProcesadaQueue, TopicExchange sateliteExchange) {
        return BindingBuilder.bind(cotizacionProcesadaQueue)
                .to(sateliteExchange)
                .with(COTIZACION_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
