/**
 * 标准物质管理系统 - E2E测试公共函数
 */

const BASE_URL = process.env.FRONTEND_URL || 'http://localhost:3002';

// 颜色定义
const colors = {
  red: '\x1b[31m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  reset: '\x1b[0m'
};

function log(message, type = 'info') {
  const color = type === 'pass' ? colors.green : type === 'fail' ? colors.red : type === 'warn' ? colors.yellow : colors.blue;
  console.log(`${color}${message}${colors.reset}`);
}

function logTest(name) {
  console.log(`\n${'='.repeat(50)}`);
  log(`测试页面: ${name}`, 'info');
  console.log('='.repeat(50));
}

function logStep(step, passed, details = '') {
  const icon = passed ? '✓' : '✗';
  const status = passed ? 'pass' : 'fail';
  log(`  ${icon} ${step}${details ? ` - ${details}` : ''}`, status);
}

function logWarn(step, details = '') {
  log(`  ⚠ ${step}${details ? ` - ${details}` : ''}`, 'warn');
}

// 测试结果统计
class TestResult {
  constructor(pageName) {
    this.pageName = pageName;
    this.passed = 0;
    this.failed = 0;
    this.warnings = 0;
    this.errors = [];
  }

  addPass() { this.passed++; }
  addFail(step, error) {
    this.failed++;
    this.errors.push({ step, error: error.message || error });
  }
  addWarn() { this.warnings++; }

  print() {
    console.log(`\n${'-'.repeat(50)}`);
    log(`${this.pageName} 测试结果:`, 'info');
    log(`  通过: ${this.passed}`, 'pass');
    if (this.failed > 0) {
      log(`  失败: ${this.failed}`, 'fail');
      this.errors.forEach(e => log(`    - ${e.step}: ${e.error}`, 'fail'));
    }
    if (this.warnings > 0) {
      log(`  警告: ${this.warnings}`, 'warn');
    }
  }
}

module.exports = {
  BASE_URL,
  colors,
  log,
  logTest,
  logStep,
  logWarn,
  TestResult
};
