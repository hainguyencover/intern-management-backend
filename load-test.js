import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 20 }, // ramp up to 20 users
        { duration: '1m', target: 20 },  // stay at 20 users
        { duration: '20s', target: 0 },  // ramp down to 0 users
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'], // 95% of requests must complete below 500ms
        http_req_failed: ['rate<0.01'],    // less than 1% failure rate
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
    // 1. Health check
    let res = http.get(`${BASE_URL}/actuator/health`);
    check(res, { 'status is 200': (r) => r.status === 200 });

    // 2. Simulated Login (Static check for now or real login if token provided)
    // For a basic load test, we can hit public endpoints or simulate high-volume reads
    res = http.get(`${BASE_URL}/api/v1/applications?size=10`);
    check(res, { 'api status is 200 or 403': (r) => r.status === 200 || r.status === 403 });

    sleep(1);
}
