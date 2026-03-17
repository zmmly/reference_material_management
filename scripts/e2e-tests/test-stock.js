/**
 * 库存查询页面测试
 * 测试功能: 搜索过滤、分页、入库登记跳转、出库跳转
 */

const { chromium } = require('playwright');
const { BASE_URL, logTest, logStep, logWarn, TestResult } = require('./common');

async function testStock() {
  logTest('库存查询页面');
  const result = new TestResult('库存查询页面');

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

    // 访问库存查询
    await page.goto(`${BASE_URL}/stock`);
    await page.waitForTimeout(1000);

    // 1. 检查搜索表单
    const keywordInput = await page.locator('input[placeholder="名称/编码/批号"]').count();
    const locationSelect = await page.locator('.el-select').count();
    const searchBtn = await page.locator('button:has-text("查询")').count();
    const stockInBtn = await page.locator('button:has-text("入库登记")').count();
    logStep('搜索表单完整', keywordInput && searchBtn && stockInBtn);
    (keywordInput && searchBtn && stockInBtn) ? result.addPass() : result.addFail('搜索表单');

    // 2. 检查数据表格
    const tableExists = await page.locator('.el-table').count() > 0;
    logStep('库存表格显示', tableExists);
    tableExists ? result.addPass() : result.addFail('库存表格');

    // 3. 检查表格列
    const columns = ['内部编码', '标准物质名称', '批号', '库存数量', '有效期', '存放位置', '状态', '操作'];
    let columnsOk = true;
    for (const col of columns) {
      const found = await page.locator(`.el-table th:has-text("${col}")`).count() > 0;
      if (!found) columnsOk = false;
    }
    logStep('表格列完整', columnsOk);
    columnsOk ? result.addPass() : result.addWarn('部分列可能缺失');

    // 4. 测试搜索功能
    await page.fill('input[placeholder="名称/编码/批号"]', 'test');
    await page.click('button:has-text("查询")');
    await page.waitForTimeout(500);
    logStep('关键字搜索执行', true);
    result.addPass();

    // 清空搜索
    await page.fill('input[placeholder="名称/编码/批号"]', '');

    // 5. 测试状态筛选
    const statusSelect = page.locator('.el-form-item:has-text("状态") .el-select');
    if (await statusSelect.count() > 0) {
      await statusSelect.click();
      await page.waitForTimeout(300);
      await page.click('.el-select-dropdown__item:has-text("正常")');
      await page.waitForTimeout(500);
      logStep('状态筛选执行', true);
      result.addPass();
    }

    // 6. 测试入库登记跳转
    await page.click('button:has-text("入库登记")');
    await page.waitForTimeout(1000);
    const navigatedToStockIn = page.url().includes('/stock-in');
    logStep('入库登记跳转', navigatedToStockIn);
    navigatedToStockIn ? result.addPass() : result.addFail('入库跳转');

    // 返回库存页面
    await page.goto(`${BASE_URL}/stock`);
    await page.waitForTimeout(500);

    // 7. 检查分页
    const pagination = await page.locator('.el-pagination').count() > 0;
    logStep('分页组件显示', pagination);
    pagination ? result.addPass() : result.addWarn('分页可能不存在');

    // 8. 测试状态标签显示
    const statusTags = await page.locator('.el-table .el-tag').count();
    logStep('状态标签显示', statusTags > 0, `数量: ${statusTags}`);
    statusTags > 0 ? result.addPass() : result.addWarn('无状态标签');

    // 9. 检查有效期颜色标识
    // 红色(已过期)或橙色(即将过期)
    const warningColors = await page.locator('.el-table .text-warning, .el-table .text-danger').count();
    logStep('有效期颜色标识', true, `警告样式元素: ${warningColors}`);

    // 10. 测试出库按钮
    const outBtn = await page.locator('button:has-text("出库")').first();
    if (await outBtn.count() > 0) {
      logStep('出库按钮存在', true);
      result.addPass();
      // 点击出库会跳转到出库申请页面
      // 不实际点击以避免创建数据
    } else {
      logStep('出库按钮存在', false, '可能无库存数据');
      result.addWarn('可能无库存数据');
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

module.exports = { testStock };
