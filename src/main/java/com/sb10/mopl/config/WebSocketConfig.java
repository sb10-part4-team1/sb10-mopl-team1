package com.sb10.mopl.config;

import com.sb10.mopl.common.exception.GlobalStompChannelErrorHandler;
import com.sb10.mopl.common.interceptor.StompChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker // STOMP 사용 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final StompChannelInterceptor stompChannelInterceptor;
  private final GlobalStompChannelErrorHandler globalStompChannelErrorHandler;

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // 엔드포인트 정의
    registry
        .addEndpoint("/ws")
        .setAllowedOriginPatterns("*") // fixme: 운영 단계에서 수정 필요
        .withSockJS(); // SockJS fallback 지원

    // 채널 인터셉터(preSend) 단계에서 발생한 예외를 STOMP ERROR 프레임으로 응답
    registry.setErrorHandler(globalStompChannelErrorHandler);
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    // publish prefix
    registry.setApplicationDestinationPrefixes("/pub");

    // Subscribe prefix
    // todo: redis 기반 fan-out으로 전환
    registry.enableSimpleBroker("/sub");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registry) {
    registry.interceptors(stompChannelInterceptor);
  }
}
