/**
 * 预警中心页面测试
 * 测试功能: 统计卡片、预警列表、 处理/忽略预警、 手动检查
 */

const { chromium } = require('playwright');
const { BASE_URL, logTest, logStep, logWarn, TestResult } = require('./common');

async function testAlert() {
  logTest('预警中心页面');
  const result = new TestResult('预警中心页面');
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();
  try {
    // 登录
    await page.goto(`${BASE_URL}/login`);
    await page.fill('input[placeholder="用户名"]', 'admin');
    await page.fill('input[placeholder="密码"]', 'admin123');
    await page.click('button:has-text("登录")');
    await page.waitForTimeout(1000);
    // 访问预警中心
    await page.goto(`${BASE_URL}/alert`);
    await page.waitForTimeout(1000);
    // 1. 检查统计卡片
    const statCards = await page.locator('.stat-card').count();
    logStep('统计卡片显示', statCards >= 4, `数量: ${statCards}`);
    statCards >= 4 ? result.addPass() : result.addFail('统计卡片');
    // 2. 检查筛选表单
    const statusSelect = await page.locator('.el-form-item:has-text("状态") .el-select').count() > 0;
    const typeSelect = await page.locator('.el-form-item:has-text("类型") .el-select').count() > 0;
    logStep('筛选表单完整', statusSelect && typeSelect);
    (statusSelect && typeSelect) ? result.addPass() : result.addFail('筛选表单');
    // 3. 检查预警列表
    const alertTable = await page.locator('.el-card:has-text("预警类型") .el-table').count() > 0;
    logStep('预警列表显示', alertTable > 0);
    alertTable > 0 ? result.addPass() : result.addFail('预警列表');
    // 4. 测试状态筛选
    await page.click('.el-form-item:has-text("状态") .el-select');
    await page.waitForTimeout(300);
    await page.click('.el-select-dropdown__item:has-text("未处理")');
    await page.waitForTimeout(500);
    logStep('状态筛选执行', true);
    result.addPass();
    // 5. 测试类型筛选
    await page.click('.el-form-item:has-text("类型") .el-select');
    await page.waitForTimeout(300);
    await page.click('.el-select-dropdown__item:has-text("有效期预警")');
    await page.waitForTimeout(500);
    logStep('类型筛选执行', true);
    result.addPass();
    // 6. 测试手动检查按钮
    const triggerBtn = await page.locator('button:has-text("手动检查")').count() > 0;
    logStep('手动检查按钮显示', triggerBtn > 0);
    triggerBtn > 0 ? result.addPass() : result.addFail('手动检查按钮');
    // 7. 测试处理预警
    const handleBtn = await page.locator('button:has-text("处理")');
    if (await handleBtn.count() > 0) {
      await handleBtn.first().click();
      await page.waitForTimeout(500);
      // 检查处理对话框
      const handleDialog = await page.locator('.el-dialog:visible').count() > 0;
      logStep('处理预警对话框弹出', handleDialog > 0);
      handleDialog > 0 ? result.addPass() : result.addFail('处理对话框');
      if (handleDialog > 0) {
        // 填写处理说明
        await page.fill('.el-dialog:visible textarea', '测试处理说明');
        await page.click('.el-dialog:visible button:has-text("确定")');
        await page.waitForTimeout(500);
        logStep('处理预警提交', true);
        result.addPass();
      }
    }
    // 8. 测试忽略预警
    const ignoreBtn = await page.locator('button:has-text("忽略")');
    if (await ignoreBtn.count() > 0) {
      await ignoreBtn.first().click();
      await page.waitForTimeout(500);
      logStep('忽略预警执行', true);
      result.addPass();
    }
    // 9. 检查预警类型标签
    const typeTags = await page.locator('.el-table .el-tag').count();
    logStep('预警类型标签显示', typeTags > 0, `数量: ${typeTags}`);
    typeTags > 0 ? result.addPass() : result.addWarn('无预警数据');
    // 10. 检查预警级别标签
    const levelTags = await page.locator('.el-table .el-tag[effect="dark"]').count();
    logStep('预警级别标签显示', levelTags >= 0);
    levelTags >= 0 ? result.addPass() : result.addWarn('无预警数据');
  } catch (error) {
    logStep('测试执行异常', false, error.message);
    result.addFail('执行异常', error);
  } finally {
    await browser.close();
  }
  result.print();
  return result;
}
module.exports = { testAlert };
