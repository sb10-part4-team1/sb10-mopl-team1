package com.sb10.mopl.batch.cache;

import com.sb10.mopl.batch.client.TmdbApiClient;
import com.sb10.mopl.batch.dto.TmdbGenreListDto;
import com.sb10.mopl.batch.dto.TmdbGenreListDto.TmdbGenreDto;
import com.sb10.mopl.content.entity.ContentType;
import com.sb10.mopl.content.entity.Tag;
import com.sb10.mopl.content.repository.TagRepository;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
 * TMDB 영화/TV 장르 사전 조립형 캐시 컴포넌트입니다.
 *
 * 배치 스텝 최초 실행 시 콘텐츠 타입에 맞게 initialize(ContentType)를 호출하여
 * 각 타입별 genreTranslationMap(장르 ID -> 한글 장르명)과
 * genreTagMap(한글 장르명 -> Tag 엔티티)을 완성해 둡니다.
 *
 * 이후 수천 번의 청크가 돌아가는 동안 DB 태그 조회 SELECT 쿼리 0회를 보장합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbGenreCache {

  private final TmdbApiClient tmdbApiClient;
  private final TagRepository tagRepository;

  // 영화용 장르 번역기 및 태그 맵
  private final Map<Integer, String> movieTranslationMap = new ConcurrentHashMap<>();
  private final Map<String, Tag> movieTagMap = new ConcurrentHashMap<>();

  // TV용 장르 번역기 및 태그 맵
  private final Map<Integer, String> tvTranslationMap = new ConcurrentHashMap<>();
  private final Map<String, Tag> tvTagMap = new ConcurrentHashMap<>();

  /*
   * 콘텐츠 타입별 TMDB 장르 API 호출 -> 번역기 맵 구성 -> DB IN 쿼리 1회 -> 신규 태그 saveAll -> 태그 캐시 맵 완성.
   * 스텝 시작 시점에 호출되므로 매 호출 시 기존 맵 데이터를 깨끗하게 비우고 새로 로드합니다.
   */
  @Transactional
  public void initialize(ContentType contentType) {
    switch (contentType) {
      case MOVIE -> {
        movieTranslationMap.clear();
        movieTagMap.clear();
        initializeCache("/genre/movie/list", movieTranslationMap, movieTagMap);
        log.info("TMDB 영화 장르 캐시 초기화 완료 - 총 장르 수: {}", movieTagMap.size());
      }
      case TV_SERIES -> {
        tvTranslationMap.clear();
        tvTagMap.clear();
        initializeCache("/genre/tv/list", tvTranslationMap, tvTagMap);
        log.info("TMDB TV 시리즈 장르 캐시 초기화 완료 - 총 장르 수: {}", tvTagMap.size());
      }
      default -> {
        // 영화 및 TV 시리즈 외의 콘텐츠 타입은 장르 캐싱 대상이 아닙니다.
      }
    }
  }

  private void initializeCache(
      String path, Map<Integer, String> translationMap, Map<String, Tag> tagMap) {
    // 1. TMDB 장르 API 호출 후 유효한 데이터만 사전 취합
    TmdbGenreListDto genreResponse = tmdbApiClient.fetch(path, null, TmdbGenreListDto.class);
    List<TmdbGenreDto> validGenres =
        genreResponse.genres().stream().filter(g -> g.id() != null && g.name() != null).toList();

    // 2. 사전에 걸러진 깨끗한 리스트만 활용하여 번역기 맵 및 이름 목록 구성
    validGenres.forEach(g -> translationMap.put(g.id(), g.name()));
    List<String> genreNames = validGenres.stream().map(TmdbGenreDto::name).toList();

    // 2. DB에 기존에 저장된 장르 태그 조회
    Map<String, Tag> existingTagMap =
        tagRepository.findAllByNameIn(genreNames).stream()
            .collect(Collectors.toMap(Tag::getName, tag -> tag));

    // 3. DB에 없는 신규 장르 태그 생성 및 일괄 저장
    List<Tag> newTags =
        genreNames.stream()
            .filter(name -> !existingTagMap.containsKey(name))
            .map(Tag::create)
            .toList();

    if (!newTags.isEmpty()) {
      List<Tag> savedTags = tagRepository.saveAll(newTags);
      savedTags.forEach(tag -> existingTagMap.put(tag.getName(), tag));
      log.info("TMDB 신규 장르 태그 등록 완료 ({}) - 등록 수: {}", path, savedTags.size());
    }

    // 4. tagMap 완성 (한글 장르명 -> Tag 엔티티)
    tagMap.putAll(existingTagMap);
  }

  /*
   * 콘텐츠 타입과 장르 ID를 받아 해당하는 Tag 엔티티를 반환합니다.
   * 번역기 맵에 없는 ID이거나 태그 맵에 없으면 null을 반환합니다.
   */
  public Tag resolveTag(Integer genreId, ContentType contentType) {
    Map<Integer, String> translationMap =
        (contentType == ContentType.MOVIE) ? movieTranslationMap : tvTranslationMap;
    Map<String, Tag> tagMap = (contentType == ContentType.MOVIE) ? movieTagMap : tvTagMap;

    String genreName = translationMap.get(genreId);
    if (genreName == null) {
      log.warn("TMDB 장르 ID 번역 실패 (타입: {}) - 알 수 없는 장르 ID: {}", contentType, genreId);
      return null;
    }
    Tag tag = tagMap.get(genreName);
    if (tag == null) {
      log.warn("TMDB 장르 태그 맵 조회 실패 (타입: {}) - 장르명: {}", contentType, genreName);
    }
    return tag;
  }
}
