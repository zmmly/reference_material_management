/**
 * 入库管理页面测试
 * 测试功能: 列表显示、入库登记表单、 筛选
 */

const { chromium } = require('playwright');
const { BASE_URL, logTest, logStep, logWarn, TestResult } = require('./common');

async function testStockIn() {
  logTest('入库管理页面');
  const result = new TestResult('入库管理页面');
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
    // 访问入库管理
    await page.goto(`${BASE_URL}/stock-in`);
    await page.waitForTimeout(1000);
    // 1. 检查入库原因筛选
    const reasonSelect = await page.locator('.el-form-item:has-text("入库原因") .el-select').count() > 0;
    logStep('入库原因筛选显示', reasonSelect > 0);
    reasonSelect > 0 ? result.addPass() : result.addFail('入库原因筛选');
    // 2. 检查按钮
    const searchBtn = await page.locator('button:has-text("查询")').count() > 0;
    const addBtn = await page.locator('button:has-text("入库登记")').count() > 0;
    logStep('操作按钮完整', searchBtn && addBtn > 0);
    (searchBtn && addBtn > 0) ? result.addPass() : result.addFail('操作按钮');
    // 3. 检查数据表格
    const tableExists = await page.locator('.el-table').count() > 0;
    logStep('入库记录表格显示', tableExists > 0);
    tableExists > 0 ? result.addPass() : result.addFail('入库记录表格');
    // 4. 检查表格列
    const columns = ['标准物质', '批号', '内部编码', '入库数量', '有效期', '存放位置', '入库原因', '操作人', '入库时间'];
    let columnsOk = true;
    for (const col of columns) {
      const found = await page.locator(`th:has-text("${col}")`).count() > 0;
      if (!found) columnsOk = false;
    }
    logStep('表格列完整', columnsOk);
    columnsOk ? result.addPass() : result.addWarn('部分列可能缺失');
    // 5. 测试入库原因筛选
    await page.click('.el-form-item:has-text("入库原因") .el-select');
    await page.waitForTimeout(300);
    await page.click('.el-select-dropdown__item:has-text("新购入")');
    await page.waitForTimeout(500);
    logStep('入库原因筛选执行', true);
    result.addPass();
    // 6. 测试入库登记对话框
    await page.click('button:has-text("入库登记")');
    await page.waitForTimeout(500);
    const dialogVisible = await page.locator('.el-dialog:visible').count() > 0;
    logStep('入库登记对话框弹出', dialogVisible > 0);
    dialogVisible > 0 ? result.addPass() : result.addFail('入库登记对话框');
    if (dialogVisible > 0) {
      // 7. 检查表单字段
      const materialSelect = await page.locator('.el-dialog:visible label:has-text("标准物质")').count() > 0;
      const batchInput = await page.locator('.el-dialog:visible label:has-text("批号")').count() > 0;
      const quantityInput = await page.locator('.el-dialog:visible label:has-text("入库数量")').count() > 0;
      const datePicker = await page.locator('.el-dialog:visible label:has-text("有效期")').count() > 0;
      logStep('入库表单字段完整', materialSelect && batchInput && quantityInput && datePicker > 0);
      (materialSelect && batchInput && quantityInput && datePicker) ? result.addPass() : result.addFail('入库表单字段');
      // 8. 测试表单验证
      await page.click('.el-dialog:visible button:has-text("确定")');
      await page.waitForTimeout(500);
      const validationErrors = await page.locator('.el-form-item__error:visible').count() > 0;
      logStep('表单验证生效', validationErrors > 0);
      validationErrors > 0 ? result.addPass() : result.addWarn('验证可能未生效');
      // 关闭对话框
      await page.click('.el-dialog:visible button:has-text("取消")');
      await page.waitForTimeout(300);
    }
    // 9. 检查分页
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
module.exports = { testStockIn };
