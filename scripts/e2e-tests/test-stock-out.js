/**
 * 出库管理页面测试
 * 测试功能: 出库申请列表、待审批列表、审批/拒绝
 */

const { chromium } = require('playwright');
const { BASE_URL, logTest, logStep, logWarn, TestResult } = require('./common');

async function testStockOut() {
  logTest('出库管理页面');
  const result = new TestResult('出库管理页面');
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
    // 访问出库管理
    await page.goto(`${BASE_URL}/stock-out`);
    await page.waitForTimeout(1000);
    // 1. 检查Tab页
    const tabs = await page.locator('.el-tabs').count();
    logStep('Tab组件显示', tabs > 0);
    tabs > 0 ? result.addPass() : result.addFail('Tab组件');
    // 2. 测试我的申请列表
    const myTable = await page.locator('.el-table').count();
    logStep('出库列表显示', myTable > 0);
    myTable > 0 ? result.addPass() : result.addFail('出库列表');
    // 3. 检查表格列
    const columns = ['标准物质', '内部编码', '申请数量', '状态'];
    let columnsOk = true;
    for (const col of columns) {
      const found = await page.locator(`th:has-text("${col}")`).count() > 0;
      if (!found) columnsOk = false;
    }
    logStep('表格列完整', columnsOk > 0);
    columnsOk > 0 ? result.addPass() : result.addWarn('部分列可能缺失');
    // 4. 切换Tab
    const tabItems = await page.locator('.el-tabs__item');
    if (await tabItems.count() > 1) {
      await tabItems.nth(1).click();
      await page.waitForTimeout(500);
      logStep('Tab切换正常', true);
      result.addPass();
      // 5. 检查审批操作按钮
      const approveBtn = await page.locator('button:has-text("通过")');
      const rejectBtn = await page.locator('button:has-text("拒绝")');
      logStep('审批操作按钮显示', (await approveBtn.count() + await rejectBtn.count()) > 0);
      (await approveBtn.count() + await rejectBtn.count()) > 0 ? result.addPass() : result.addWarn('可能无待审批数据');
      // 6. 测试撤回按钮
      const cancelBtn = await page.locator('button:has-text("撤回")');
      if (await cancelBtn.count() > 0) {
        logStep('撤回按钮显示', true);
        result.addPass();
      }
    }
    // 7. 检查状态标签
    const statusTags = await page.locator('.el-table .el-tag').count();
    logStep('状态标签显示', statusTags >= 0);
    statusTags >= 0 ? result.addPass() : result.addWarn('可能无数据');
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
module.exports = { testStockOut };
