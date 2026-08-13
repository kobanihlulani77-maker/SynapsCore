import prodConfig from './playwright.prod.config.mjs'

export default {
  ...prodConfig,
  testMatch: ['controls-execution.spec.mjs'],
  timeout: 420_000,
  expect: {
    timeout: 20_000,
  },
  reporter: [
    ['list'],
    ['html', { open: 'never', outputFolder: 'playwright-report-controls' }],
  ],
}
