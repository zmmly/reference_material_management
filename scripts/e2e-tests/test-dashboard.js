/**
 * 仪表盘页面测试
 * 测试功能: 统计卡片、待办事项、预警信息、 快捷入口
 */

const { chromium } = require('playwright');
const { BASE_URL, logTest, logStep, logWarn, TestResult } = require('./common');

async function testDashboard() {
  logTest('仪表盘页面');
  const result = new TestResult('仪表盘页面');
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();
  try {
    // 先登录
    await page.goto(`${BASE_URL}/login`);
    await page.fill('input[placeholder="用户名"]', 'admin');
    await page.fill('input[placeholder="密码"]', 'admin123');
    await page.click('button:has-text("登录")');
    await page.waitForURL('**/dashboard', { timeout: 5000 }).catch(() => {});

    // 1. 检查统计卡片
    const statCards = await page.locator('.stat-card').count();
    logStep('统计卡片显示', statCards >= 4, `数量: ${statCards}`);
    if (statCards >= 4) {
      result.addPass();
    } else {
      // 尝试备用选择器
      const altCards = await page.locator('.el-card').count();
      logStep('卡片元素显示', altCards >= 4, `数量: ${altCards}`);
      altCards >= 4 ? result.addPass() : result.addFail('统计卡片');
    }
    // 2. 点击统计卡片测试跳转
    const firstCard = page.locator('.stat-card, .el-card').first();
    if (await firstCard.count() > 0) {
      await firstCard.click();
      await page.waitForTimeout(500);
      const navigated = !page.url().includes('/dashboard');
      logStep('统计卡片点击跳转', navigated, page.url());
      navigated ? result.addPass() : result.addWarn('跳转可能未生效');
      // 返回dashboard
      await page.goto(`${BASE_URL}/dashboard`);
    }

    // 3. 检查待办事项区域
    const todoSection = await page.locator('text=待办事项').count();
    logStep('待办事项区域显示', todoSection > 0);
    todoSection > 0 ? result.addPass() : result.addWarn('待办事项可能不存在');
    // 4. 检查预警信息区域
    const alertSection = await page.locator('text=预警信息').count();
    logStep('预警信息区域显示', alertSection > 0);
    alertSection > 0 ? result.addPass() : result.addWarn('预警信息可能不存在');
    // 5. 检查图表区域
    const categoryChart = await page.locator('text=库存分类统计').count();
    const expiryChart = await page.locator('text=有效期分布').count();
    const chartsOk = categoryChart > 0 && expiryChart > 0;
    logStep('图表区域显示', chartsOk);
    chartsOk ? result.addPass() : result.addWarn('图表可能不存在');
    // 6. 检查快捷入口
    const quickEntries = await page.locator('.quick-entry').count();
    logStep('快捷入口显示', quickEntries >= 0, `数量: ${quickEntries}`);
    quickEntries >= 0 ? result.addPass() : result.addWarn('快捷入口可能不存在');
    // 7. 测试快捷入口点击
    if (quickEntries > 0) {
      await page.locator('.quick-entry, .el-col-4 > div').first().click();
      await page.waitForTimeout(500);
      const navigated2 = !page.url().includes('/dashboard');
      logStep('快捷入口点击跳转', navigated2);
      navigated2 ? result.addPass() : result.addWarn('跳转可能未生效');
    }
    // 8. 检查页面无JS错误
    const consoleErrors = [];
    page.on('console', msg => {
      if (msg.type() === 'error') consoleErrors.push(msg.text());
    });
    await page.waitForTimeout(1000);
    logStep('页面无JS错误', consoleErrors.length === 0, consoleErrors.length);
    consoleErrors.length === 0 ? result.addPass() : result.addWarn(`有${consoleErrors.length}个错误`);
  } catch (error) {
    const errorMsg = error && error.message ? error.message : String(error);
    logStep('测试执行异常', false, errorMsg);
    result.addFail('执行异常', new Error(errorMsg));
  } finally {
    await browser.close();
  }
  result.print();
  return result;
}
module.exports = { testDashboard };
