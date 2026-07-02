package com.sb10.mopl.batch.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.batch.BatchDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

/**
 * 실제 서비스 환경(!test)에서 데이터베이스 커넥션 풀을 API용과 배치용으로 물리 격리하는 설정 클래스입니다.
 *
 * <p>[풀 격리 이유]: 대용량 배치 작업이 DB 커넥션을 대량 점유하여 실시간 API 웹 서비스(로그인, 조회 등)가 커넥션을 얻지 못해
 *
 * <p>시스템이 먹통이 되는 장애를방지하기 위해 HikariCP 커넥션 풀을 물리적으로 2개 분리합니다.
 *
 * <p>[Lazy 프록시 역할]: 외부 API 통신(네트워크 대기) 동안 DB 커넥션을 불필요하게 물고 있는 병목 현상을 방어하기 위해,
 *
 * <p>실제 SQL 쿼리가 실행되는 시점에만 물리 커넥션을 획득하도록 배치용 풀에 지연 프록시(LazyConnectionDataSourceProxy)를 씌웁니다.
 *
 * <p>[빈 역할 분담]:
 *
 * <p>1. dataSource: 일반 API 서비스 및 JPA 리포지토리가 사용하는 기본(@Primary) 커넥션 풀
 *
 * <p>2. batchDataSource:스프링 배치 프레임워크가 전용으로 탈취(@BatchDataSource)하여 사용하는 배치 메타데이터 및 작업용 풀
 */
@Profile("!test")
@Configuration
public class DataSourceConfig {

  private final DataSourceProperties properties;

  @Value("${mopl.datasource.api-pool-name:HikariPool-API}")
  private String apiPoolName;

  @Value("${mopl.datasource.api-pool-size:10}")
  private int apiPoolSize;

  @Value("${mopl.datasource.batch-pool-name:HikariPool-Batch}")
  private String batchPoolName;

  @Value("${mopl.datasource.batch-pool-size:5}")
  private int batchPoolSize;

  public DataSourceConfig(DataSourceProperties properties) {
    this.properties = properties;
  }

  /**
   * API 서비스용 기본 데이터소스(Primary) 빈입니다.
   *
   * @return HikariDataSource
   */
  @Primary
  @Bean("dataSource")
  public HikariDataSource dataSource() {
    HikariDataSource ds =
        properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    ds.setPoolName(apiPoolName);
    ds.setMaximumPoolSize(apiPoolSize);
    return ds;
  }

  /**
   * 배치 전용 실제 물리 데이터소스 빈입니다.
   *
   * @return HikariDataSource
   */
  @Bean("actualBatchDataSource")
  public HikariDataSource actualBatchDataSource() {
    HikariDataSource ds =
        properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    ds.setPoolName(batchPoolName);
    ds.setMaximumPoolSize(batchPoolSize);
    return ds;
  }

  /**
   * 실제 배치 물리 데이터소스를 지연 획득 프록시로 감싸서 스프링 배치 전용 데이터소스로 등록합니다.
   *
   * @param actualBatchDataSource 실제 배치 물리 Hikari 풀
   * @return LazyConnectionDataSourceProxy
   */
  @BatchDataSource
  @Bean("batchDataSource")
  public DataSource batchDataSource(
      @Qualifier("actualBatchDataSource") DataSource actualBatchDataSource) {
    return new LazyConnectionDataSourceProxy(actualBatchDataSource);
  }
}
