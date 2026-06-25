package com.sb10.mopl.content.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sb10.mopl.config.JpaAuditingConfig;
import com.sb10.mopl.content.entity.Tag;
import java.util.List;
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
class TagRepositoryTest {

  @Autowired private TagRepository tagRepository;

  @Test
  void findAllByNameIn_returnExistingTags_whenTagNamesExist() {
    // given: 데이터베이스에 미리 "SF"와 "드라마" 태그를 영속화
    tagRepository.save(Tag.create("SF"));
    tagRepository.save(Tag.create("드라마"));

    // when: 검색 키워드로 "SF", "드라마", "존재하지않음"을 전달
    List<Tag> foundTags = tagRepository.findAllByNameIn(List.of("SF", "드라마", "존재하지않음"));

    // then: 존재하는 2개의 태그 엔티티만 성공적으로 반환되는지 확인
    assertThat(foundTags).hasSize(2);
    assertThat(foundTags).extracting(Tag::getName).containsExactlyInAnyOrder("SF", "드라마");
  }

  @Test
  void findByName_returnTag_whenTagNameExists() {
    // given: 데이터베이스에 "액션" 태그 영속화
    Tag tag = tagRepository.save(Tag.create("액션"));

    // when: 이름 "액션"으로 단건 조회
    var foundTagOpt = tagRepository.findByName("액션");

    // then: 조회된 태그의 식별자와 값이 영속화된 태그와 일치하는지 확인
    assertThat(foundTagOpt).isPresent();
    assertThat(foundTagOpt.get().getId()).isEqualTo(tag.getId());
    assertThat(foundTagOpt.get().getName()).isEqualTo("액션");
  }
}
