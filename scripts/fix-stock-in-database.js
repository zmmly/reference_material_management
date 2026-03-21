/**
 * 数据库修复脚本
 * 用于添加 product_certificate 字段到 stock_in 表
 *
 * 使用方法：
 * 1. 确保已安装 Node.js 和 mysql2 包
 * 2. 安装依赖: npm install mysql2
 * 3. 运行脚本: node fix-stock-in-database.js
 */

const mysql = require('mysql2/promise');

// 数据库配置
const dbConfig = {
  host: '8.138.246.23',
  port: 3306,
  user: 'root',
  password: 'xjYY3687!',
  database: 'reference_material_management',
  charset: 'utf8mb4'
};

async function fixDatabase() {
  let connection;
  try {
    console.log('正在连接数据库...');
    connection = await mysql.createConnection(dbConfig);
    console.log('数据库连接成功！');

    // 检查字段是否已存在
    const [rows] = await connection.execute(
      "SHOW COLUMNS FROM stock_in LIKE 'product_certificate'"
    );

    if (rows.length > 0) {
      console.log('product_certificate 字段已存在，无需修复');
      return;
    }

    // 添加字段
    console.log('正在添加 product_certificate 字段...');
    await connection.execute(
      `ALTER TABLE stock_in ADD COLUMN product_certificate VARCHAR(255) COMMENT '产品证书文件路径' AFTER remarks`
    );

    console.log('✅ 数据库修复成功！');
    console.log('字段 product_certificate 已添加到 stock_in 表');

  } catch (error) {
    console.error('❌ 数据库修复失败：', error.message);
    throw error;
  } finally {
    if (connection) {
      await connection.end();
      console.log('数据库连接已关闭');
    }
  }
}

// 执行修复
fixDatabase()
  .then(() => {
    console.log('修复完成');
    process.exit(0);
  })
  .catch((error) => {
    console.error('修复过程出错');
    process.exit(1);
  });
