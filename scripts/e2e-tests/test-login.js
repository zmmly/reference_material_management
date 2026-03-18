/**
 * 登录页面测试
 * 测试功能: 登录表单验证、验证码、登录成功/失败
 */

const { chromium } = require('playwright');
const { BASE_URL, logTest, logStep, logWarn, TestResult } = require('./common');

async function testLogin() {
  logTest('登录页面');
  const result = new TestResult('登录页面');

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    // 1. 访问登录页面
    await page.goto(`${BASE_URL}/login`);
    logStep('访问登录页面', true);
    result.addPass();

    // 2. 检查页面标题
    const title = await page.locator('h2').textContent();
    const titleOk = title.includes('标准物质管理系统');
    logStep('页面标题显示正确', titleOk, title);
    titleOk ? result.addPass() : result.addFail('页面标题', title);

    // 3. 检查登录表单元素
    const usernameInput = await page.locator('input[placeholder="请输入用户名"]').count();
    const passwordInput = await page.locator('input[placeholder="请输入密码"]').count();
    const captchaInput = await page.locator('input[placeholder="请输入验证码"]').count();
    const captchaImage = await page.locator('.captcha-image img').count();
    const loginButton = await page.locator('button:has-text("登")').count();
    const formOk = usernameInput && passwordInput && captchaInput && captchaImage && loginButton;
    logStep('登录表单元素完整(含验证码)', formOk);
    formOk ? result.addPass() : result.addFail('登录表单元素');

    // 4. 测试空表单提交验证
    await page.click('button:has-text("登")');
    await page.waitForTimeout(500);
    const validationError = await page.locator('.el-form-item__error').count() > 0;
    logStep('空表单验证提示', validationError);
    validationError ? result.addPass() : result.addWarn('未显示验证提示');

    // 5. 测试验证码刷新功能
    const firstCaptchaSrc = await page.locator('.captcha-image img').getAttribute('src');
    await page.click('.captcha-image');
    await page.waitForTimeout(500);
    const secondCaptchaSrc = await page.locator('.captcha-image img').getAttribute('src');
    const captchaRefreshed = firstCaptchaSrc !== secondCaptchaSrc;
    logStep('验证码点击刷新', captchaRefreshed);
    captchaRefreshed ? result.addPass() : result.addWarn('验证码可能未刷新');

    // 6. 测试错误密码登录
    await page.fill('input[placeholder="请输入用户名"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'wrongpassword');
    await page.fill('input[placeholder="请输入验证码"]', 'xxxx');
    await page.click('button:has-text("登")');
    await page.waitForTimeout(1500);
    // 检查是否还在登录页或显示错误
    const stillOnLogin = page.url().includes('/login');
    logStep('错误密码/验证码拒绝登录', stillOnLogin);
    stillOnLogin ? result.addPass() : result.addFail('错误密码处理');

    // 7. 测试正确登录（需要获取验证码答案）
    await page.fill('input[placeholder="请输入用户名"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');

    // 获取验证码答案（开发环境API返回captchaAnswer字段）
    const captchaResponse = await page.evaluate(async () => {
      const res = await fetch('/api/auth/captcha');
      return await res.json();
    });

    if (captchaResponse.data && captchaResponse.data.captchaAnswer) {
      // 开发环境：使用正确的验证码
      await page.fill('input[placeholder="请输入验证码"]', captchaResponse.data.captchaAnswer);
      logStep('使用正确的验证码', true, captchaResponse.data.captchaAnswer);
    } else {
      // 生产环境：无法获取验证码，跳过此测试
      logWarn('生产环境无法获取验证码答案，跳过登录测试');
      result.addWarn('生产环境无法完整测试登录');
      await browser.close();
      result.print();
      return result;
    }

    await page.click('button:has-text("登")');

    // 等待跳转到dashboard或修改密码页面
    await page.waitForURL('**/dashboard', { timeout: 5000 }).catch(() => {});
    await page.waitForURL('**/change-password', { timeout: 1000 }).catch(() => {});

    const loginSuccess = page.url().includes('/dashboard') || page.url().includes('/change-password');
    logStep('正确凭证登录成功', loginSuccess, page.url());
    loginSuccess ? result.addPass() : result.addFail('登录失败');

    // 8. 检查登录后token存储
    const localStorage = await page.evaluate(() => JSON.stringify(window.localStorage));
    const hasToken = localStorage.includes('token');
    logStep('Token已存储', hasToken);
    hasToken ? result.addPass() : result.addFail('Token存储');

  } catch (error) {
    logStep('测试执行异常', false, error.message);
    result.addFail('执行异常', error);
  } finally {
    await browser.close();
  }

  result.print();
  return result;
}

module.exports = { testLogin: testLogin };
