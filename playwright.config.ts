import { defineConfig, devices } from '@playwright/test';
export default defineConfig({
 testDir: './tests/e2e',
 timeout: 45000,
 expect: { timeout: 12000 },
 retries: 0,
 workers: 1,
 reporter: [['list'], ['html', {open: 'never',outputFolder:'playwright-report'}]],
 use: { baseURL: 'http://127.0.0.1:3000', ...devices['Desktop Chrome'],trace: 'retain-on-failure' },
 webServer: {command:'npm run dev',url:'http://127.0.0.1:3000/api/health',timeout:150000,reuseExistingServer:false,env:{PORT:'3000'}},
});
