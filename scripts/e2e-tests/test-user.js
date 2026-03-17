/**
 * 用户管理页面测试
 * 测试功能: 用户列表、新增用户、编辑用户、 状态切换、 重置密码
 */

const { chromium } = require('playwright');
const { BASE_URL, logTest, logStep, logWarn, TestResult } = require('./common');
async function testUser() {
  logTest('用户管理页面');
  const result = new TestResult('用户管理页面');
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
    // 访问用户管理
    await page.goto(`${BASE_URL}/system/user`);
    await page.waitForTimeout(1000);
    // 1. 检查搜索表单
    const usernameInput = await page.locator('input[placeholder="请输入"]').count() > 0;
    const statusSelect = await page.locator('.el-select').count() > 0;
    logStep('搜索表单完整', usernameInput && statusSelect > 0);
    (usernameInput && statusSelect > 0) ? result.addPass() : result.addFail('搜索表单');
    // 2. 检查用户列表
    const tableExists = await page.locator('.el-table').count() > 0;
    logStep('用户列表显示', tableExists > 0);
    tableExists > 0 ? result.addPass() : result.addFail('用户列表');
    // 3. 检查表格列
    const columns = ['用户名', '姓名', '手机号', '角色', '状态', '操作'];
    let columnsOk = true;
    for (const col of columns) {
      const found = await page.locator(`th:has-text("${col}")`).count() > 0;
      if (!found) columnsOk = false;
    }
    logStep('表格列完整', columnsOk);
    columnsOk ? result.addPass() : result.addFail('表格列');
    // 4. 测试新增用户
    await page.click('button:has-text("新增")');
    await page.waitForTimeout(500);
    const dialogVisible = await page.locator('.el-dialog:visible').count() > 0;
    logStep('新增用户对话框弹出', dialogVisible);
    dialogVisible ? result.addPass() : result.addFail('新增用户对话框');
    if (dialogVisible) {
      // 5. 检查表单字段
      const formFields = ['用户名', '姓名', '手机号', '邮箱', '角色'];
      let fieldsOk = true;
      for (const field of formFields) {
        const found = await page.locator(`.el-dialog:visible label:has-text("${field}")`).count() > 0;
        if (!found) fieldsOk = false;
      }
      logStep('表单字段完整', fieldsOk);
      fieldsOk ? result.addPass() : result.addFail('表单字段');
      // 6. 测试表单验证
      await page.click('.el-dialog:visible button:has-text("确定")');
      await page.waitForTimeout(500);
      const validationError = await page.locator('.el-form-item__error:visible').count() > 0;
      logStep('表单验证生效', validationError > 0);
      validationError > 0 ? result.addPass() : result.addWarn('验证可能未生效');
      // 关闭对话框
      await page.click('.el-dialog:visible button:has-text("取消")');
      await page.waitForTimeout(300);
    }
    // 7. 测试编辑用户
    const editBtn = await page.locator('button:has-text("编辑")');
    if (await editBtn.count() > 0) {
      await editBtn.first().click();
      await page.waitForTimeout(500);
      const editDialogVisible = await page.locator('.el-dialog:visible').count() > 0;
      logStep('编辑用户对话框弹出', editDialogVisible);
      editDialogVisible ? result.addPass() : result.addFail('编辑对话框');
      if (editDialogVisible) {
        // 用户名应被禁用
        const usernameDisabled = await page.locator('.el-dialog:visible input').first().isDisabled();
        logStep('编辑时用户名禁用', usernameDisabled);
        usernameDisabled ? result.addPass() : result.addFail('用户名禁用状态');
        // 关闭对话框
        await page.click('.el-dialog:visible button:has-text("取消")');
        await page.waitForTimeout(300);
      }
    }
    // 8. 测试状态切换
    const toggleBtn = await page.locator('button:has-text("禁用"), button:has-text("启用")');
    if (await toggleBtn.count() > 0) {
      await toggleBtn.first().click();
      await page.waitForTimeout(500);
      // 检查确认对话框
      const confirmVisible = await page.locator('.el-message-box:visible').count() > 0;
      logStep('状态切换确认框弹出', confirmVisible > 0);
      confirmVisible > 0 ? result.addPass() : result.addFail('状态切换确认框');
      if (confirmVisible > 0) {
        // 取消操作
        await page.click('.el-message-box button:has-text("取消")');
        await page.waitForTimeout(300);
      }
    }
    // 9. 测试重置密码
    const resetBtn = await page.locator('button:has-text("重置密码")');
    if (await resetBtn.count() > 0) {
      await resetBtn.first().click();
      await page.waitForTimeout(500);
      const resetConfirmVisible = await page.locator('.el-message-box:visible').count() > 0;
      logStep('重置密码确认框弹出', resetConfirmVisible > 0);
      resetConfirmVisible > 0 ? result.addPass() : result.addFail('重置密码确认框');
      if (resetConfirmVisible > 0) {
        // 取消操作
        await page.click('.el-message-box button:has-text("取消")');
        await page.waitForTimeout(300);
      }
    }
    // 10. 检查分页
    const pagination = await page.locator('.el-pagination').count() > 0;
    logStep('分页组件显示', pagination > 0);
    pagination > 0 ? result.addPass() : result.addWarn('分页可能不存在');
  } catch (error) {
    logStep('测试执行异常', false, error.message);
    result.addFail('执行异常', error);
  } finally {
    await browser.close();
  }
  result.print();
  return result;
}
module.exports = { testUser };
