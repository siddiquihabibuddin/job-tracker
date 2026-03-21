import { chromium } from 'playwright';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const BASE = 'http://localhost:5173';

async function getAuthData() {
  const res = await fetch('http://localhost:8080/v1/auth/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'demo@example.com', password: 'demo' }),
  });
  return res.json();
}

async function login(page) {
  const auth = await getAuthData();
  const user = { id: auth.userId, email: auth.email, name: auth.displayName ?? auth.email.split('@')[0] };
  // Seed both token and user into sessionStorage so AuthContext picks them up on load
  await page.goto(BASE + '/login');
  await page.waitForLoadState('networkidle');
  await page.evaluate(({ t, u }) => {
    sessionStorage.setItem('jt_token', t);
    sessionStorage.setItem('jt_user', JSON.stringify(u));
  }, { t: auth.token, u: user });
  await page.goto(BASE + '/dashboard');
  await page.waitForURL('**/dashboard', { timeout: 15000 });
}

async function run() {
  const browser = await chromium.launch({ headless: true });

  // --- Login page (unauthenticated) ---
  const anonCtx = await browser.newContext({ viewport: { width: 1400, height: 860 } });
  const anonPage = await anonCtx.newPage();
  await anonPage.goto(BASE + '/login');
  await anonPage.waitForLoadState('networkidle');
  await anonPage.waitForTimeout(500);
  await anonPage.screenshot({ path: resolve(__dirname, 'docs/screenshots/login.png') });
  console.log('✓ login.png');
  await anonCtx.close();

  // --- Authenticated pages ---
  const ctx = await browser.newContext({ viewport: { width: 1400, height: 860 } });
  const page = await ctx.newPage();
  await login(page);

  // Dashboard — wait for charts
  await page.waitForTimeout(4000);
  await page.screenshot({ path: resolve(__dirname, 'docs/screenshots/dashboard.png') });
  console.log('✓ dashboard.png');

  // Applications list
  await page.goto(BASE + '/applications');
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(2000);
  await page.screenshot({ path: resolve(__dirname, 'docs/screenshots/applications.png') });
  console.log('✓ applications.png');

  // New application form
  await page.goto(BASE + '/applications/new');
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(1000);
  await page.screenshot({ path: resolve(__dirname, 'docs/screenshots/new-application.png') });
  console.log('✓ new-application.png');

  // View a single application (grab first ID from Node context to avoid CORS)
  const { token } = await getAuthData();
  const appsRes = await fetch('http://localhost:8080/v1/applications?limit=1', {
    headers: { Authorization: `Bearer ${token}` }
  }).then(r => r.json());
  const firstId = appsRes?.items?.[0]?.id;
  if (firstId) {
    await page.goto(BASE + `/applications/${firstId}`);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1500);
    await page.screenshot({ path: resolve(__dirname, 'docs/screenshots/view-application.png') });
    console.log('✓ view-application.png');
  }

  await ctx.close();
  await browser.close();
  console.log('All screenshots saved to docs/screenshots/');
}

run().catch(err => { console.error(err); process.exit(1); });
