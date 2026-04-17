import fs from 'node:fs'
import { defineConfig } from '@playwright/test'

const browserExecutablePath = [
  process.env.PLAYWRIGHT_BROWSER_PATH,
  'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
  'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
].find((candidate) => candidate && fs.existsSync(candidate))

const launchOptions = browserExecutablePath
  ? { executablePath: browserExecutablePath, args: ['--disable-dev-shm-usage'] }
  : { args: ['--disable-dev-shm-usage'] }

export default defineConfig({
  testDir: './tests',
  timeout: 180_000,
  expect: {
    timeout: 20_000,
  },
  reporter: [
    ['list'],
    ['html', { open: 'never', outputFolder: 'playwright-report' }],
  ],
  use: {
    baseURL: process.env.PLAYWRIGHT_FRONTEND_URL || 'https://synapscore-frontend-3.onrender.com',
    headless: process.env.PLAYWRIGHT_HEADED !== 'true',
    viewport: { width: 1440, height: 960 },
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    launchOptions,
  },
})
