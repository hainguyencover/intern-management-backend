import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const loginDuration = new Trend('login_duration');
const apiErrorRate = new Rate('api_errors');

export const options = {
    scenarios: {
        // Smoke test: verify system works
        smoke: {
            executor: 'constant-vus',
            vus: 1,
            duration: '10s',
            tags: { test_type: 'smoke' },
        },
        // Load test: normal traffic pattern
        load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 20 },  // ramp up
                { duration: '1m', target: 20 },   // steady
                { duration: '30s', target: 50 },   // ramp up more
                { duration: '1m', target: 50 },   // steady high
                { duration: '30s', target: 0 },    // ramp down
            ],
            startTime: '15s', // start after smoke
            tags: { test_type: 'load' },
        },
        // Stress test: find breaking point
        stress: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 50 },
                { duration: '30s', target: 100 },
                { duration: '30s', target: 150 },
                { duration: '1m', target: 150 },
                { duration: '30s', target: 0 },
            ],
            startTime: '4m', // start after load
            tags: { test_type: 'stress' },
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1500'],
        http_req_failed: ['rate<0.05'],
        api_errors: ['rate<0.1'],
        login_duration: ['p(95)<1000'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || '';

function getHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    if (AUTH_TOKEN) {
        headers['Authorization'] = `Bearer ${AUTH_TOKEN}`;
    }
    return headers;
}

export default function () {
    const headers = getHeaders();

    group('Health & Actuator', () => {
        const res = http.get(`${BASE_URL}/actuator/health`);
        check(res, {
            'health status is 200': (r) => r.status === 200,
            'health response contains UP': (r) => r.body && r.body.includes('UP'),
        });
    });

    group('Authentication', () => {
        const loginPayload = JSON.stringify({
            email: __ENV.TEST_EMAIL || 'admin@example.com',
            password: __ENV.TEST_PASSWORD || 'admin123',
        });
        const res = http.post(`${BASE_URL}/api/v1/auth/login`, loginPayload, { headers });
        loginDuration.add(res.timings.duration);
        const loginOk = check(res, {
            'login returns 200 or 401': (r) => r.status === 200 || r.status === 401,
        });
        if (!loginOk) apiErrorRate.add(1);
        else apiErrorRate.add(0);
    });

    group('API Endpoints', () => {
        // Dashboard
        let res = http.get(`${BASE_URL}/api/v1/dashboard/overview`, { headers });
        check(res, {
            'dashboard returns expected status': (r) => [200, 401, 403].includes(r.status),
        });

        // Intern list (paginated)
        res = http.get(`${BASE_URL}/api/v1/interns/profiles?page=0&size=10`, { headers });
        check(res, {
            'intern list returns expected status': (r) => [200, 401, 403].includes(r.status),
        });

        // Programs list
        res = http.get(`${BASE_URL}/api/v1/programs?page=0&size=10`, { headers });
        check(res, {
            'programs returns expected status': (r) => [200, 401, 403].includes(r.status),
        });

        // Tasks list
        res = http.get(`${BASE_URL}/api/v1/tasks/assigned?page=0&size=10`, { headers });
        check(res, {
            'tasks returns expected status': (r) => [200, 401, 403].includes(r.status),
        });
    });

    sleep(Math.random() * 2 + 0.5); // 0.5-2.5s between iterations
}

export function handleSummary(data) {
    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    };
}

// k6 built-in
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';
