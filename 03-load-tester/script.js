import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  // 1. 부하 설정: 10명의 가상 유저(VU)가 동시에 접속
  vus: 10,
  // 2. 테스트 기간: 30초 동안 지속
  duration: '30s',
};

export default function () {
  // 환경변수나 인자로 받은 URL 사용 (기본값 설정)
  const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080';
  
  // 요청 보내기
  const res = http.get(`${BASE_URL}/api/fast`);

  // 응답이 200인지 확인 (성공 체크)
  check(res, {
    'is status 200': (r) => r.status === 200,
  });

  // 다음 요청 전 약간의 휴식 (현실적인 사용자 시뮬레이션)
  sleep(0.1);
}
