#!/bin/bash

# ============================================
# 强制设置Java 17为默认版本
# 用法: sudo ./fix-java17-setup.sh
# ============================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

echo -e "${YELLOW}[Java 17设置] 检测并设置Java 17为默认版本...${NC}"

# 检查是否有sudo权限
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}✗ 请使用 sudo 执行此脚本${NC}"
    echo -e "${YELLOW}  sudo $0${NC}"
    exit 1
fi

# 检查当前Java版本
CURRENT_JAVA=$(java -version 2>&1 | head -1 | sed 's/.*version "\(.*\)".*/\1/')
echo -e "${YELLOW}当前Java版本: ${CURRENT_JAVA}${NC}"

# 检查Java 17是否存在
if [ ! -d "/usr/lib/jvm/java-17-openjdk" ]; then
    echo -e "${RED}✗ Java 17未找到 (/usr/lib/jvm/java-17-openjdk)${NC}"
    echo -e "${YELLOW}请先安装JDK 17:${NC}"
    echo -e "${YELLOW}  yum install -y java-17-openjdk java-17-openjdk-devel${NC}"
    exit 1
fi

# 如果当前Java不是17，强制设置为17
if ! echo "$CURRENT_JAVA" | grep -q "^17"; then
    echo -e "${YELLOW}当前Java不是17，正在强制设置...${NC}"

    # 使用alternatives设置Java 17为默认
    if command -v alternatives &> /dev/null; then
        echo -e "${YELLOW}使用alternatives设置Java 17为默认版本...${NC}"
        alternatives --install /usr/bin/java java /usr/lib/jvm/java-17-openjdk/bin/java 1700 2>&1 | grep -v "There is" || true
        alternatives --set java /usr/lib/jvm/java-17-openjdk/bin/java 2>&1 | grep -v "There is" || true
        echo -e "${GREEN}✓ Java 17已通过alternatives设置为默认版本${NC}"
    else
        echo -e "${YELLOW}alternatives不可用，使用环境变量设置Java 17...${NC}"

        # 写入系统配置文件
        cat > /etc/profile.d/java17.sh << 'EOF'
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH
EOF
        chmod +x /etc/profile.d/java17.sh
        echo -e "${GREEN}✓ Java 17环境变量已写入 /etc/profile.d/java17.sh${NC}"
        echo -e "${YELLOW}⚠ 请执行以下命令使环境变量生效:${NC}"
        echo -e "${YELLOW}  source /etc/profile.d/java17.sh${NC}"
    fi
fi

# 验证设置结果
NEW_JAVA=$(/usr/lib/jvm/java-17-openjdk/bin/java -version 2>&1 | head -1)
echo -e "${GREEN}✓ Java 17路径: /usr/lib/jvm/java-17-openjdk${NC}"
echo -e "${GREEN}✓ 版本信息: ${NEW_JAVA}${NC}"

echo ""
echo -e "${GREEN}╔══════════════════════════════════════╗${NC}"
echo -e "${GREEN}║         Java 17 设置完成！           ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}请重新登录或执行以下命令使设置生效:${NC}"
echo -e "${YELLOW}  source /etc/profile.d/java17.sh 2>/dev/null || true${NC}"
echo -e "${YELLOW}  java -version${NC}"
echo ""
echo -e "${YELLOW}然后运行部署脚本:${NC}"
echo -e "${YELLOW}  cd /opt/reference_material_management && sudo ./scripts/deploy.sh${NC}"
echo ""

exit 0