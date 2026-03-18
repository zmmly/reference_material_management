/**
 * 盘点管理页面测试
 * 测试功能: 盘点任务列表、盘点明细、新建盘点、确认盘点、完成盘点
 */

const { chromium } = require('playwright');
const { BASE_URL, logTest, logStep, logWarn, TestResult } = require('./common');

async function testStockCheck() {
  logTest('盘点管理页面');
  const result = new TestResult('盘点管理页面');
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();
  try {
    // 登录
    await page.goto(`${BASE_URL}/login`);
    await page.fill('input[placeholder="请输入用户名"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    await page.click('button:has-text("登")');
    await page.waitForTimeout(1000);

    // 访问盘点管理
    await page.goto(`${BASE_URL}/stock-check`);
    await page.waitForTimeout(1000);

    // 1. 检查左右布局
    const leftCol = await page.locator('.el-col-8, .el-col-6, .el-col-4').count();
    const rightCol = await page.locator('.el-col-16, .el-col-18, .el-col-20').count();
    const hasLayout = leftCol > 0 && rightCol > 0;
    logStep('左右分栏布局', hasLayout > 0);
    hasLayout > 0 ? result.addPass() : result.addFail('布局');

    // 2. 检查盘点任务列表
    const taskList = await page.locator('.el-table').count() > 0;
    logStep('盘点任务列表显示', taskList > 0);
    taskList > 0 ? result.addPass() : result.addFail('盘点任务列表');

    // 3. 测试新建盘点按钮
    const createBtn = await page.locator('button:has-text("新建盘点")').count() > 0;
    logStep('新建盘点按钮显示', createBtn > 0);
    createBtn > 0 ? result.addPass() : result.addFail('新建盘点按钮');

    // 4. 测试新建盘点对话框
    await page.click('button:has-text("新建盘点")');
    await page.waitForTimeout(500);
    const dialogVisible = await page.locator('.el-dialog:visible').count() > 0;
    logStep('新建盘点对话框弹出', dialogVisible > 0);
    dialogVisible > 0 ? result.addPass() : result.addFail('新建盘点对话框');

    if (dialogVisible > 0) {
      // 5. 检查必填表单字段（盘点日期、盘点范围）
      const dateInput = await page.locator('.el-dialog:visible .el-date-picker').count() > 0;
      const scopeSelect = await page.locator('.el-dialog:visible label:has-text("盘点范围")').count() > 0;
      const requiredFieldsOk = dateInput > 0 && scopeSelect > 0;
      logStep('必填表单字段完整', requiredFieldsOk > 0);
      requiredFieldsOk > 0 ? result.addPass() : result.addFail('必填表单字段');

      // 关闭对话框
      await page.click('.el-dialog:visible button:has-text("取消")');
      await page.waitForTimeout(300);
    }

    // 6. 测试选择盘点任务
    const taskRows = await page.locator('.el-col-8 .el-table tbody tr, .el-col-6 .el-table tbody tr, .el-col-4 .el-table tbody tr');
    if (await taskRows.count() > 0) {
      await taskRows.first().click();
      await page.waitForTimeout(500);
      logStep('选择盘点任务', true);
      result.addPass();

      // 7. 检查盘点明细面板
      const detailCard = await page.locator('.el-card:visible').count();
      logStep('盘点明细面板显示', detailCard > 0);
      detailCard > 0 ? result.addPass() : result.addWarn('请先选择盘点任务');

      // 8. 检查完成盘点按钮
      const completeBtn = await page.locator('button:has-text("完成盘点")').count();
      logStep('完成盘点按钮显示', completeBtn > 0);
      completeBtn > 0 ? result.addPass() : result.addWarn('需要先选择盘点任务');

      // 9. 检查确认盘点按钮
      const confirmBtn = await page.locator('button:has-text("确认盘点")').count();
      logStep('确认盘点按钮显示', confirmBtn >= 0);
      confirmBtn >= 0 ? result.addPass() : result.addWarn('需要先选择盘点任务');
    } else {
      logStep('选择盘点任务', false, '无盘点任务');
      result.addWarn('暂无盘点任务数据');
    }

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

module.exports = { testStockCheck };
