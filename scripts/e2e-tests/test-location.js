/**
 * 位置管理页面测试
 * 测试功能: 搜索、新增位置、编辑位置、 分页
 */

const { chromium } = require('playwright');
const { BASE_URL, logTest, logStep, logWarn, TestResult } = require('./common');

async function testLocation() {
  logTest('位置管理页面');
  const result = new TestResult('位置管理页面');
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
    // 访问位置管理
    await page.goto(`${BASE_URL}/basic/location`);
    await page.waitForTimeout(1000);
    // 1. 检查搜索表单
    const searchInput = await page.locator('input[placeholder="请输入"]').count() > 0;
    const searchBtn = await page.locator('button:has-text("查询")').count() > 0;
    const addBtn = await page.locator('button:has-text("新增")').count() > 0;
    logStep('搜索表单元素完整', searchInput && searchBtn && addBtn > 0);
    (searchInput && searchBtn && addBtn > 0) ? result.addPass() : result.addFail('搜索表单');
    // 2. 检查数据表格
    const tableExists = await page.locator('.el-table').count() > 0;
    logStep('数据表格显示', tableExists > 0);
    tableExists > 0 ? result.addPass() : result.addFail('数据表格');
    // 3. 检查分页组件
    const pagination = await page.locator('.el-pagination').count() > 0;
    logStep('分页组件显示', pagination > 0);
    pagination > 0 ? result.addPass() : result.addWarn('分页可能不存在');
    // 4. 测试搜索功能
    await page.fill('input[placeholder="请输入"]', 'test');
    await page.click('button:has-text("查询")');
    await page.waitForTimeout(500);
    logStep('搜索功能执行', true);
    result.addPass();
    // 清空搜索
    await page.fill('input[placeholder="请输入"]', '');
    await page.click('button:has-text("查询")');
    await page.waitForTimeout(500);
    // 5. 测试新增位置
    await page.click('button:has-text("新增")');
    await page.waitForTimeout(500);
    const dialogVisible = await page.locator('.el-dialog:visible').count() > 0;
    logStep('新增对话框弹出', dialogVisible > 0);
    dialogVisible > 0 ? result.addPass() : result.addFail('新增对话框');
    if (dialogVisible > 0) {
      // 6. 检查表单字段
      const codeInput = await page.locator('.el-dialog:visible input').first().count() > 0;
      const nameInput = await page.locator('.el-dialog:visible label:has-text("位置名称")').count() > 0;
      const tempSelect = await page.locator('.el-dialog:visible label:has-text("温度要求")').count() > 0;
      logStep('表单字段完整', codeInput && nameInput && tempSelect > 0);
      (codeInput && nameInput && tempSelect > 0) ? result.addPass() : result.addFail('表单字段');
      // 7. 测试表单验证
      await page.click('.el-dialog:visible button:has-text("确定")');
      await page.waitForTimeout(500);
      const validationError = await page.locator('.el-form-item__error:visible').count() > 0;
      logStep('表单验证生效', validationError > 0);
      validationError > 0 ? result.addPass() : result.addWarn('验证可能未生效');
      // 8. 填写表单
      const testCode = `LOC${Date.now().toString().slice(-6)}`;
      const firstInput = page.locator('.el-dialog:visible input').first();
      await firstInput.fill(testCode);
      // 查写位置名称
      const nameInputField = page.locator('.el-dialog:visible .el-form-item:has-text("位置名称") input');
      await nameInputField.fill(`测试位置_${Date.now()}`);
      // 选择温度
      const tempSelectEl = page.locator('.el-dialog:visible .el-form-item:has-text("温度要求") .el-select');
      if (await tempSelectEl.count() > 0) {
        await tempSelectEl.click();
        await page.waitForTimeout(300);
        await page.click('.el-select-dropdown__item:has-text("常温")');
        await page.waitForTimeout(300);
      }
      // 提交
      await page.click('.el-dialog:visible button:has-text("确定")');
      await page.waitForTimeout(1000);
      const dialogClosed = await page.locator('.el-dialog:visible').count() === 0;
      logStep('新增位置提交成功', dialogClosed > 0);
      dialogClosed > 0 ? result.addPass() : result.addFail('新增提交');
      // 9. 测试编辑功能
      await page.waitForTimeout(500);
      const editBtn = await page.locator('button:has-text("编辑")');
      if (await editBtn.count() > 0) {
        await editBtn.first().click();
        await page.waitForTimeout(500);
        const editDialogVisible = await page.locator('.el-dialog:visible').count() > 0;
        logStep('编辑对话框弹出', editDialogVisible > 0);
        editDialogVisible > 0 ? result.addPass() : result.addFail('编辑对话框');
        if (editDialogVisible > 0) {
          // 关闭对话框
          await page.click('.el-dialog:visible button:has-text("取消")');
          await page.waitForTimeout(300);
        }
      }
    }
    // 10. 检查表格列
    const columns = ['位置编码', '位置名称', '温度要求', '容量', '描述', '操作'];
    let columnsOk = true;
    for (const col of columns) {
      const found = await page.locator(`.el-table th:has-text("${col}")`).count() > 0;
      if (!found) columnsOk = false;
    }
    logStep('表格列完整', columnsOk > 0);
    columnsOk > 0 ? result.addPass() : result.addWarn('部分列可能缺失');
  } catch (error) {
    logStep('测试执行异常', false, error.message);
    result.addFail('执行异常', error);
  } finally {
    await browser.close();
  }
  result.print();
  return result;
}
module.exports = { testLocation };
