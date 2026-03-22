#!/bin/bash

# ============================================
# 修复Windows换行符问题脚本
# ============================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "正在修复脚本文件的换行符..."

# 检查dos2unix工具是否可用
if command -v dos2unix &> /dev/null; then
    echo "使用dos2unix工具转换..."
    dos2unix ${SCRIPT_DIR}/deploy.sh
    dos2unix ${SCRIPT_DIR}/update.sh
    dos2unix ${SCRIPT_DIR}/quick-deploy.sh
    echo "✓ 转换完成"
else
    echo "使用sed命令转换..."
    # 转换CRLF为LF
    sed -i 's/\r$//' ${SCRIPT_DIR}/deploy.sh
    sed -i 's/\r$//' ${SCRIPT_DIR}/update.sh
    sed -i 's/\r$//' ${SCRIPT_DIR}/quick-deploy.sh
    echo "✓ 转换完成"
fi

# 确保文件有执行权限
chmod +x ${SCRIPT_DIR}/deploy.sh
chmod +x ${SCRIPT_DIR}/update.sh
chmod +x ${SCRIPT_DIR}/quick-deploy.sh

echo ""
echo "修复完成！现在可以运行："
echo "  ./deploy.sh"
echo "  ./update.sh"
echo "  ./quick-deploy.sh"
