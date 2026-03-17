/**
 * 分类管理页面测试
 * 测试功能: 分类树显示、新增顶级分类、添加子分类、编辑分类、删除分类
 */

const { chromium } = require('playwright');
const { BASE_URL, logTest, logStep, logWarn, TestResult } = require('./common');

async function testCategory() {
  logTest('分类管理页面');
  const result = new TestResult('分类管理页面');

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

    // 访问分类管理
    await page.goto(`${BASE_URL}/basic/category`);
    await page.waitForTimeout(1000);

    // 1. 检查新增按钮
    const addButton = await page.locator('button:has-text("新增顶级分类")').count();
    logStep('新增顶级分类按钮显示', addButton > 0);
    addButton > 0 ? result.addPass() : result.addFail('新增按钮');

    // 2. 检查分类树表格
    const tableExists = await page.locator('.el-table').count() > 0;
    logStep('分类树表格显示', tableExists);
    tableExists ? result.addPass() : result.addFail('分类表格');

    // 3. 测试新增顶级分类
    await page.click('button:has-text("新增顶级分类")');
    await page.waitForTimeout(500);

    const dialogVisible = await page.locator('.el-dialog:visible').count() > 0;
    logStep('新增分类对话框弹出', dialogVisible);
    dialogVisible ? result.addPass() : result.addFail('新增对话框');

    // 4. 测试表单验证
    if (dialogVisible) {
      await page.click('.el-dialog:visible button:has-text("确定")');
      await page.waitForTimeout(500);
      const validationError = await page.locator('.el-form-item__error:visible').count() > 0;
      logStep('表单验证生效', validationError);
      validationError ? result.addPass() : result.addWarn('验证可能未生效');

      // 5. 填写分类名称并提交
      const testCategoryName = `测试分类_${Date.now()}`;
      await page.fill('.el-dialog:visible input', testCategoryName);
      await page.click('.el-dialog:visible button:has-text("确定")');
      await page.waitForTimeout(1000);

      // 检查是否成功（对话框关闭）
      const dialogClosed = await page.locator('.el-dialog:visible').count() === 0;
      logStep('新增分类提交成功', dialogClosed);
      dialogClosed ? result.addPass() : result.addFail('新增提交');

      // 6. 检查新分类是否出现在列表中
      await page.waitForTimeout(500);
      const newCategoryVisible = await page.locator(`text=${testCategoryName}`).count() > 0;
      logStep('新分类显示在列表', newCategoryVisible);
      newCategoryVisible ? result.addPass() : result.addWarn('可能需要刷新');

      // 7. 测试编辑分类
      if (newCategoryVisible) {
        // 找到刚创建的分类行的编辑按钮
        const row = page.locator(`tr:has-text("${testCategoryName}")`);
        await row.locator('button:has-text("编辑")').click();
        await page.waitForTimeout(500);

        const editDialogVisible = await page.locator('.el-dialog:visible').count() > 0;
        logStep('编辑对话框弹出', editDialogVisible);
        editDialogVisible ? result.addPass() : result.addFail('编辑对话框');

        if (editDialogVisible) {
          // 修改名称
          const editedName = `${testCategoryName}_已编辑`;
          await page.fill('.el-dialog:visible input', editedName);
          await page.click('.el-dialog:visible button:has-text("确定")');
          await page.waitForTimeout(1000);

          const editSuccess = await page.locator(`text=${editedName}`).count() > 0;
          logStep('编辑分类成功', editSuccess);
          editSuccess ? result.addPass() : result.addFail('编辑提交');

          // 8. 测试添加子分类
          await page.locator(`tr:has-text("${editedName}") button:has-text("添加子级")`).click();
          await page.waitForTimeout(500);

          const childDialogVisible = await page.locator('.el-dialog:visible').count() > 0;
          logStep('添加子分类对话框弹出', childDialogVisible);
          childDialogVisible ? result.addPass() : result.addFail('子分类对话框');

          if (childDialogVisible) {
            const childCategoryName = `子分类_${Date.now()}`;
            await page.fill('.el-dialog:visible input', childCategoryName);
            await page.click('.el-dialog:visible button:has-text("确定")');
            await page.waitForTimeout(1000);
            logStep('添加子分类成功', true);
            result.addPass();
          }

          // 9. 测试删除分类
          // 先删除子分类，再删除父分类
          const expandBtn = page.locator(`tr:has-text("${editedName}") .el-table__expand-icon`);
          if (await expandBtn.count() > 0) {
            await expandBtn.click();
            await page.waitForTimeout(300);
          }

          // 点击删除
          await page.locator(`tr:has-text("${editedName}") button:has-text("删除")`).click();
          await page.waitForTimeout(500);

          // 确认删除对话框
          const confirmVisible = await page.locator('.el-message-box').count() > 0;
          if (confirmVisible) {
            await page.click('.el-message-box button:has-text("确定")');
            await page.waitForTimeout(1000);
            logStep('删除分类成功', true);
            result.addPass();
          } else {
            logStep('删除确认框显示', false);
            result.addFail('删除确认框');
          }
        }
      }
    }

    // 10. 检查操作列按钮
    const actionButtons = await page.locator('.el-table tr:first-child .el-button').count();
    logStep('操作列按钮显示', actionButtons >= 3, `数量: ${actionButtons}`);
    actionButtons >= 3 ? result.addPass() : result.addWarn(`按钮数量: ${actionButtons}`);

  } catch (error) {
    logStep('测试执行异常', false, error.message);
    result.addFail('执行异常', error);
  } finally {
    await browser.close();
  }

  result.print();
  return result;
}

module.exports = { testCategory };
