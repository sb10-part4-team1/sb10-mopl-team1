import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 50 },  // 10초 동안 50명까지 증가
    { duration: '20s', target: 200 }, // 20초 동안 200명까지 증가 (최고 부하)
    { duration: '30s', target: 200 }, // 200명으로 30초간 유지
    { duration: '10s', target: 0 },   // 10초 동안 종료
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],   // 에러율 5% 미만 유지
    http_req_duration: ['p(95)<2000'], // 95%의 요청은 2초 이내 응답
  },
};

export default function () {
  const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';

  // 백엔드 ContentSearchRequest의 필수 파라미터(@NotNull sortBy) 추가
  const url = `${baseUrl}/api/contents?sortBy=createdAt`;

  const params = {
    headers: {
      'Accept': 'application/json',
    },
  };

  const res = http.get(url, params);

  if (res.status !== 200) {
    console.log(`[ERROR LOG] Status: ${res.status} | Body: ${res.body}`);
  }

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
