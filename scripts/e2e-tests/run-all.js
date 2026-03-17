/**
 * 标准物质管理系统 - E2E测试主运行脚本
 * 依次运行所有页面测试
 */

const { chromium } = require('playwright');
const { log } = require('./common');
const loginModule = require('./test-login');
const dashboardModule = require('./test-dashboard');
const categoryModule = require('./test-category');
const locationModule = require('./test-location');
const stockModule = require('./test-stock');
const stockInModule = require('./test-stock-in');
const stockOutModule = require('./test-stock-out');
const purchaseModule = require('./test-purchase');
const stockCheckModule = require('./test-stock-check');
const alertModule = require('./test-alert');
const userModule = require('./test-user');
async function main() {
  log('\n======================================');
  log('  标准物质管理系统 - E2E测试');
  log('======================================\n');
  const results = [];
  // 依次运行所有测试
  const testModules = [
    { name: '登录页面', fn: loginModule.testLogin },
    { name: '仪表盘页面', fn: dashboardModule.testDashboard },
    { name: '分类管理页面', fn: categoryModule.testCategory },
    { name: '位置管理页面', fn: locationModule.testLocation },
    { name: '库存查询页面', fn: stockModule.testStock },
    { name: '入库管理页面', fn: stockInModule.testStockIn },
    { name: '出库管理页面', fn: stockOutModule.testStockOut },
    { name: '采购申请页面', fn: purchaseModule.testPurchase },
    { name: '盘点管理页面', fn: stockCheckModule.testStockCheck },
    { name: '预警中心页面', fn: alertModule.testAlert },
    { name: '用户管理页面', fn: userModule.testUser }
  ];
  let totalPassed = 0;
  let totalFailed = 0;
  let totalWarnings = 0;
  for (const module of testModules) {
    try {
      log(`\n正在运行: ${module.name}...`);
      const result = await module.fn();
      results.push({ name: module.name, result });
      if (result && result.passed !== undefined) {
        totalPassed += result.passed || 0;
        totalFailed += result.failed || 0;
        totalWarnings += result.warnings || 0;
      }
    } catch (error) {
      log(`测试模块 ${module.name} 执行失败: ${error.message}`, 'fail');
      results.push({ name: module.name, error: error.message });
      totalFailed++;
    }
  }
  // 输出总结
  log('\n======================================');
  log('  测试总结');
  log('======================================');
  for (const r of results) {
    const status = r.error ? '✗' : '✓';
    log(`${status} ${r.name}${r.error ? `: ${r.error}` : ''}`);
  }
  log(`\n总通过: ${totalPassed}`, 'pass');
  log(`总失败: ${totalFailed}`, totalFailed > 0 ? 'fail' : 'pass');
  if (totalWarnings > 0) {
    log(`总警告: ${totalWarnings}`, 'warn');
  }
  if (totalFailed === 0) {
    log('\n✓ 所有测试通过!', 'pass');
    process.exit(0);
  } else {
    log(`\n✗ 有 ${totalFailed} 个测试失败`, 'fail');
    process.exit(1);
  }
}
// 执行测试
main().catch(error => {
  log('\n测试执行异常', 'fail');
  console.error(error);
  process.exit(1);
});
