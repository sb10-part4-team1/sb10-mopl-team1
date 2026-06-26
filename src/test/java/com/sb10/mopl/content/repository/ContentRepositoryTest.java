package com.sb10.mopl.content.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sb10.mopl.config.JpaAuditingConfig;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class ContentRepositoryTest {

  @Autowired private ContentRepository contentRepository;

  @Test
  void save_success_whenContentDataIsValid() {
    // given: 올바른 콘텐츠 엔티티를 빌드
    Content content =
        Content.create("인셉션", ContentType.MOVIE, "SF 스릴러 영화", "/uploads/inception.jpg");

    // when: 콘텐츠 영속화 수행
    Content savedContent = contentRepository.save(content);

    // then: 식별자 UUID가 자동 할당되었고 올바르게 저장되었는지 확인
    assertThat(savedContent.getId()).isNotNull();
    assertThat(savedContent.getTitle()).isEqualTo("인셉션");
    assertThat(savedContent.getType()).isEqualTo(ContentType.MOVIE);
  }
}
