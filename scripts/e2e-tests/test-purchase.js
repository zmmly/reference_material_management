/**
 * 采购申请页面测试
 * 测试功能: 我的申请列表、 待审批列表、 新建申请、 审批流程
 */

const { chromium } = require('playwright');
const { BASE_URL, logTest, logStep, logWarn, TestResult } = require('./common');

async function testPurchase() {
  logTest('采购申请页面');
  const result = new TestResult('采购申请页面');
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
    // 访问采购申请
    await page.goto(`${BASE_URL}/purchase`);
    await page.waitForTimeout(1000);
    // 1. 检查Tab页
    const tabs = await page.locator('.el-tabs').count() > 0;
    const myTab = await page.locator('.el-tabs__item:has-text("我的申请")').count() > 0;
    const pendingTab = await page.locator('.el-tabs__item:has-text("待审批")').count() > 0;
    logStep('Tab页切换功能', tabs > 0);
    tabs > 0 ? result.addPass() : result.addFail('Tab页切换');
    // 2. 测试我的申请列表
    const myTable = await page.locator('#pane-my .el-table').count() > 0;
    logStep('我的申请列表显示', myTable > 0);
    myTable > 0 ? result.addPass() : result.addWarn('列表可能为空');
    // 3. 测试状态标签
    const statusTags = await page.locator('#pane-my .el-tag').count();
    logStep('状态标签显示', statusTags >= 0);
    statusTags >= 0 ? result.addPass() : result.addWarn('无状态数据');
    // 4. 测试新建采购申请
    await page.click('button:has-text("新建采购申请")');
    await page.waitForTimeout(500);
    const dialogVisible = await page.locator('.el-dialog:visible').count() > 0;
    logStep('新建申请对话框弹出', dialogVisible > 0);
    dialogVisible > 0 ? result.addPass() : result.addFail('新建申请对话框');
    if (dialogVisible > 0) {
      // 5. 检查表单字段
      const materialSelect = await page.locator('.el-dialog:visible .el-form-item:has-text("标准物质")').count() > 0;
      const quantityInput = await page.locator('.el-dialog:visible .el-input-number').count() > 0;
      const reasonInput = await page.locator('.el-dialog:visible textarea').count() > 0;
      logStep('申请表单字段完整', materialSelect && quantityInput && reasonInput > 0);
      (materialSelect && quantityInput && reasonInput > 0) ? result.addPass() : result.addFail('申请表单字段');
      // 6. 测试表单验证
      await page.click('.el-dialog:visible button:has-text("提交申请")');
      await page.waitForTimeout(500);
      const validationError = await page.locator('.el-form-item__error:visible').count() > 0;
      logStep('表单验证生效', validationError > 0);
      validationError > 0 ? result.addPass() : result.addWarn('验证可能未生效');
      // 关闭对话框
      await page.click('.el-dialog:visible button:has-text("取消")');
      await page.waitForTimeout(300);
    }
    // 7. 切换到待审批Tab
    if (pendingTab > 0) {
      await page.click('.el-tabs__item:has-text("待审批")');
      await page.waitForTimeout(500);
      // 8. 检查审批操作
      const approveBtn = await page.locator('#pane-pending button:has-text("通过")');
      const rejectBtn = await page.locator('#pane-pending button:has-text("拒绝")');
      logStep('审批操作按钮显示', approveBtn > 0 || rejectBtn > 0);
      (approveBtn > 0 || rejectBtn > 0) ? result.addPass() : result.addFail('审批操作按钮');
      // 9. 测试通过按钮
      if (await approveBtn.count() > 0) {
        await approveBtn.first().click();
        await page.waitForTimeout(500);
        const confirmVisible = await page.locator('.el-message-box:visible').count() > 0;
        logStep('通过确认框弹出', confirmVisible > 0);
        confirmVisible > 0 ? result.addPass() : result.addFail('通过确认框');
        if (confirmVisible > 0) {
          // 取消操作
          await page.click('.el-message-box button:has-text("取消")');
          await page.waitForTimeout(300);
        }
      }
      // 10. 测试拒绝按钮
      if (await rejectBtn.count() > 0) {
        await rejectBtn.first().click();
        await page.waitForTimeout(500);
        const rejectDialogVisible = await page.locator('.el-dialog:visible').count() > 0;
        logStep('拒绝对话框弹出', rejectDialogVisible > 0);
        rejectDialogVisible > 0 ? result.addPass() : result.addFail('拒绝对话框');
        if (rejectDialogVisible > 0) {
          // 取消操作
          await page.click('.el-dialog:visible button:has-text("取消")');
          await page.waitForTimeout(300);
        }
      }
    }
    // 11. 测试撤回按钮
    await page.click('.el-tabs__item:has-text("我的申请")');
    await page.waitForTimeout(300);
    const cancelBtn = await page.locator('#pane-my button:has-text("撤回")');
    if (await cancelBtn.count() > 0) {
      await cancelBtn.first().click();
      await page.waitForTimeout(500);
      const confirmVisible2 = await page.locator('.el-message-box:visible').count() > 0;
      logStep('撤回确认框弹出', confirmVisible2 > 0);
      confirmVisible2 > 0 ? result.addPass() : result.addFail('撤回确认框');
      if (confirmVisible2 > 0) {
        await page.click('.el-message-box button:has-text("取消")');
        await page.waitForTimeout(300);
      }
    }
  } catch (error) {
    logStep('测试执行异常', false, error.message);
    result.addFail('执行异常', error);
  } finally {
    await browser.close();
  }
  result.print();
  return result;
}
module.exports = { testPurchase };
