import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 50 },
    { duration: '20s', target: 200 },
    { duration: '30s', target: 200 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<2000'],
  },
};

const SEARCH_KEYWORDS = [
  '어벤져스', '스파이더맨', '해리포터', '아이언맨', '배트맨',
  '크리스토퍼', '디즈니', '마블', '액션', '코미디',
  '로맨스', '스릴러', 'SF', '공포', '드라마',
  '애니메이션', '범죄', '미스터리', '판타지', '모험',
  '사랑', '전쟁', '영웅', '우주', '시간',
  '기억', '비밀', '추적', '왕국', '전설',
  'Love', 'World', 'Man', 'Story', 'City',
  'Star', 'Night', 'Dark', 'Day', 'Game'
];

const CONTENT_TYPES = ['', 'movie', 'tvSeries', 'sport'];
const SORT_BY_OPTIONS = ['watcherCount', 'createdAt', 'rate'];

export default function () {
  const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';

  const randomKeyword = SEARCH_KEYWORDS[Math.floor(Math.random() * SEARCH_KEYWORDS.length)];
  const randomType = CONTENT_TYPES[Math.floor(Math.random() * CONTENT_TYPES.length)];
  const randomSortBy = SORT_BY_OPTIONS[Math.floor(Math.random() * SORT_BY_OPTIONS.length)];

  let url = `${baseUrl}/api/contents?keywordLike=${encodeURIComponent(randomKeyword)}&sortBy=${randomSortBy}`;
  if (randomType) {
    url += `&typeEqual=${randomType}`;
  }

  const params = {
    headers: {
      'Accept': 'application/json',
    },
  };

  const res = http.get(url, params);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response has data array': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body && Array.isArray(body.data);
      } catch (e) {
        return false;
      }
    },
  });

  sleep(Math.random() * 1.0 + 0.5);
}
