package com.sb10.mopl.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sb10.mopl.sse.kafka.SseEventPayload;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * SSE 이벤트(SseEventPayload)를 앱에서 사용하는 것과 동일한 ObjectMapper로 직렬화/역직렬화하기 위한 설정입니다. 클래스명
 * 프로퍼티(spring.kafka.producer.value-serializer 등)로만 설정하면 JsonSerializer/JsonDeserializer가 Spring이
 * 관리하는 ObjectMapper가 아닌 자체 기본 ObjectMapper를 사용해, Instant 등이 앱의 다른 JSON 응답과 다른 포맷(epoch 타임스탬프)으로
 * 직렬화되는 문제가 있었습니다.
 *
 * <p>producer가 붙이는 타입 헤더(__TypeId__)를 consumer가 그대로 읽어 SseEventPayload로 역직렬화하므로, consumer 쪽에 타입을
 * 별도로 하드코딩할 필요가 없습니다.
 *
 * <p>{@code kafkaListenerContainerFactory} 빈을 직접 이름으로 정의해야 합니다 — 이름 없이 {@code
 * ConsumerFactory<String, Object>}만 등록하면, Spring Boot가 자동구성하는 리스너 컨테이너 팩토리는 {@code
 * ConsumerFactory<Object, Object>}를 기대하므로 제네릭 타입 불일치로 이 빈을 못 찾고 자체 기본값(StringDeserializer)으로 만든
 * 컨테이너 팩토리를 그대로 쓰게 되어, {@code @KafkaListener}가 여전히 문자열을 수신해 메시지 변환 실패가 발생합니다.
 */
@Configuration
@Profile({"prod", "dev"})
public class KafkaConfig {

  @Bean
  public ProducerFactory<String, Object> producerFactory(
      KafkaProperties kafkaProperties, ObjectMapper objectMapper) {
    return new DefaultKafkaProducerFactory<>(
        kafkaProperties.buildProducerProperties(),
        new StringSerializer(),
        new JsonSerializer<>(objectMapper));
  }

  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate(
      ProducerFactory<String, Object> producerFactory) {
    return new KafkaTemplate<>(producerFactory);
  }

  @Bean
  public ConsumerFactory<String, Object> consumerFactory(
      KafkaProperties kafkaProperties, ObjectMapper objectMapper) {
    JsonDeserializer<Object> valueDeserializer = new JsonDeserializer<>(objectMapper);
    valueDeserializer.addTrustedPackages(SseEventPayload.class.getPackageName());

    return new DefaultKafkaConsumerFactory<>(
        kafkaProperties.buildConsumerProperties(), new StringDeserializer(), valueDeserializer);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
      ConsumerFactory<String, Object> consumerFactory) {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    return factory;
  }
}
