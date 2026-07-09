package com.sb10.mopl.common.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MaskingPatternLayoutEncoder extends LayoutWrappingEncoder<ILoggingEvent> {

  private String pattern;

  // 로그백 설정이 로드될 때 실행되는 인코더 초기화 및 시작 라이프사이클 메서드
  @Override
  public void start() {
    // 1. 커스텀 마스킹 레이아웃(MaskingPatternLayout) 객체를 생성
    MaskingPatternLayout maskingPatternLayout = new MaskingPatternLayout();

    // 2. 로그백의 시스템 컨텍스트(환경 및 설정 정보)를 레이아웃에 주입
    maskingPatternLayout.setContext(context);

    // 3. logback-spring.xml 에 기입된 패턴 문자열(%d %logger 등)을 레이아웃에 전달
    maskingPatternLayout.setPattern(pattern);

    // 4. 생성한 마스킹 레이아웃 엔진 가동
    maskingPatternLayout.start();

    // 5. 부모 인코더가 들고 있는 기본 Layout 인스턴스를 커스텀 마스킹 레이아웃으로 대체 (덮어쓰기 오염 방지)
    this.layout = maskingPatternLayout;

    // 6. 부모 인코더 가동(최종 기동 완료)
    super.start();
  }
}
