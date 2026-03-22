#!/bin/bash

# ============================================
# 强制设置Java 17为默认版本
# ============================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

echo -e "${YELLOW}[Java 17设置] 检测并设置Java 17为默认版本...${NC}"

# 检查当前Java版本
CURRENT_JAVA=$(java -version 2>&1 | head -1 | sed 's/.*version "\(.*\)".*/\1/')
echo -e "${YELLOW}当前Java版本: ${CURRENT_JAVA}${NC}"

# 检查Java 17是否存在
if [ ! -d "/usr/lib/jvm/java-17-openjdk" ]; then
    echo -e "${RED}✗ Java 17未找到${NC}"
    echo -e "${YELLOW}请先安装JDK 17${NC}"
    exit 1
fi

# 如果当前Java不是17，强制设置为17
if ! echo "$CURRENT_JAVA" | grep -q "^17"; then
    echo -e "${YELLOW}当前Java不是17，正在强制设置...${NC}"

    # 使用alternatives设置Java 17为默认
    if command -v alternatives &> /dev/null; then
        echo -e "${YELLOW}使用alternatives设置Java 17为默认版本...${NC}"
        alternatives --install /usr/bin/java java /usr/lib/jvm/java-17-openjdk/bin/java 2>&1 | grep -v "There is" || true
        alternatives --set java /usr/lib/jvm/java-17-openjdk/bin/java 2>&1 | grep -v "There is" || true
        echo -e "${GREEN}✓ Java 17已设置为默认版本${NC}"
    else
        echo -e "${YELLOW}使用环境变量设置Java 17...${NC}"
        # 设置环境变量
        export JAVA_HOME="/usr/lib/jvm/java-17-openjdk"
        export PATH="${JAVA_HOME}/bin:$PATH"

        # 写入系统配置文件
        echo "export JAVA_HOME=/usr/lib/jvm/java-17-openjdk" | sudo tee /etc/profile.d/reference-material-management-java17.sh > /dev/null
        echo "export PATH=${JAVA_HOME}/bin:\$PATH" | sudo tee -a /etc/profile.d/reference-material-management-java17.sh > /dev/null
        echo -e "${GREEN}✓ Java 17环境变量已设置${NC}"

        # 重新加载环境
        source /etc/profile.d/reference-material-management-java17.sh
    fi

    # 验证设置结果
    NEW_JAVA=$(java -version 2>&1 | head -1 | sed 's/.*version "\(.*\)".*/\1/')
    echo -e "${GREEN}✓ Java版本已设置: ${NEW_JAVA}${NC}"

    return 0
else
    echo -e "${GREEN}✓ Java 17已是默认版本${NC}"
    return 0
fi

echo -e "${GREEN}╔══════════════════════════╗${NC}"
echo -e "${GREEN}║ Java 17设置完成！                    ║${NC}"
echo -e "${GREEN}╚══════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}现在可以运行部署脚本：${NC}"
echo -e "${YELLOW}  sudo ./scripts/deploy.sh${NC}"
echo ""