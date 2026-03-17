/**
 * 登录页面测试
 * 测试功能: 登录表单验证、登录成功/失败
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
    const usernameInput = await page.locator('input[placeholder="用户名"]').count();
    const passwordInput = await page.locator('input[placeholder="密码"]').count();
    const loginButton = await page.locator('button:has-text("登录")').count();
    const formOk = usernameInput && passwordInput && loginButton;
    logStep('登录表单元素完整', formOk);
    formOk ? result.addPass() : result.addFail('登录表单元素');

    // 4. 测试空表单提交验证
    await page.click('button:has-text("登录")');
    await page.waitForTimeout(500);
    const validationError = await page.locator('.el-form-item__error').count() > 0;
    logStep('空表单验证提示', validationError);
    validationError ? result.addPass() : result.addWarn('未显示验证提示');

    // 5. 测试错误密码登录
    await page.fill('input[placeholder="用户名"]', 'admin');
    await page.fill('input[placeholder="密码"]', 'wrongpassword');
    await page.click('button:has-text("登录")');
    await page.waitForTimeout(1000);
    // 检查是否还在登录页或显示错误
    const stillOnLogin = page.url().includes('/login');
    logStep('错误密码拒绝登录', stillOnLogin);
    stillOnLogin ? result.addPass() : result.addFail('错误密码处理');

    // 6. 测试正确登录
    await page.fill('input[placeholder="用户名"]', 'admin');
    await page.fill('input[placeholder="密码"]', 'admin123');
    await page.click('button:has-text("登录")');

    // 等待跳转到dashboard
    await page.waitForURL('**/dashboard', { timeout: 5000 }).catch(() => {});
    const loginSuccess = page.url().includes('/dashboard');
    logStep('正确密码登录成功', loginSuccess, page.url());
    loginSuccess ? result.addPass() : result.addFail('登录失败');

    // 7. 检查登录后token存储
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
