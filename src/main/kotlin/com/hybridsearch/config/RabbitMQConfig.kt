package com.hybridsearch.config

import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {

    companion object {
        const val EXPORT_EXCHANGE = "export.exchange"
        const val EXPORT_QUEUE = "export.queue"
        const val EXPORT_ROUTING_KEY = "export.task"
    }

    @Bean
    fun exportExchange(): TopicExchange = TopicExchange(EXPORT_EXCHANGE)

    @Bean
    fun exportQueue(): Queue = QueueBuilder
        .durable(EXPORT_QUEUE)
        .withArgument("x-dead-letter-exchange", "$EXPORT_EXCHANGE.dlx")
        .build()

    @Bean
    fun exportBinding(): Binding = BindingBuilder
        .bind(exportQueue())
        .to(exportExchange())
        .with(EXPORT_ROUTING_KEY)

    @Bean
    fun jsonMessageConverter(): Jackson2JsonMessageConverter = Jackson2JsonMessageConverter()

    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory): RabbitTemplate {
        val template = RabbitTemplate(connectionFactory)
        template.messageConverter = jsonMessageConverter()
        return template
    }
}