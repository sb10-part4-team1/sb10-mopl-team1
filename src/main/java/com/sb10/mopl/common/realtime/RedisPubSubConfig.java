package com.sb10.mopl.common.realtime;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/*
 * dev/prod(다중 인스턴스 가정) 전용 Redis Pub/Sub 설정입니다. local/test는 Redis 자체가
 * RedisAutoConfiguration에서 제외되어 있으므로 이 설정도 함께 비활성화됩니다(LocalStompFanOutPublisher 참고).
 */
@Configuration
@Profile({"prod", "dev"})
public class RedisPubSubConfig {

  public static final String STOMP_FANOUT_CHANNEL = "mopl:stomp:fanout";

  @Bean
  public ChannelTopic stompFanOutTopic() {
    return new ChannelTopic(STOMP_FANOUT_CHANNEL);
  }

  @Bean(destroyMethod = "shutdown")
  public Executor stompFanOutExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(8);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("stomp-fanout-");
    executor.initialize();
    return executor;
  }

  @Bean
  public RedisMessageListenerContainer redisMessageListenerContainer(
      RedisConnectionFactory connectionFactory,
      StompFanOutSubscriber stompFanOutSubscriber,
      ChannelTopic stompFanOutTopic,
      Executor stompFanOutExecutor) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.setTaskExecutor(stompFanOutExecutor);
    container.addMessageListener(stompFanOutSubscriber, stompFanOutTopic);
    return container;
  }
}
