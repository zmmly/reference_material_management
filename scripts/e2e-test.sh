#!/bin/bash
# 标准物质管理系统 - 前端自动化测试脚本
# 用法: ./e2e-test.sh

set -e

FRONTEND_URL="http://localhost:3000"
BACKEND_URL="http://localhost:8080"
TEST_USER="admin"
TEST_PASS="admin123"

echo "======================================"
echo "  标准物质管理系统 - 自动化测试"
echo "======================================"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

ERRORS=0
WARNINGS=0

# 测试后端健康状态
test_backend_health() {
    echo -n "测试后端健康状态... "
    if curl -s -o /dev/null -w "%{http_code}" "$BACKEND_URL/api/auth/login" -X POST \
         -H "Content-Type: application/json" \
         -d "{\"username\":\"$TEST_USER\",\"password\":\"$TEST_PASS\"}" | grep -q "200"; then
        echo -e "${GREEN}✓ 通过${NC}"
        return 0
    else
        echo -e "${RED}✗ 失败${NC}"
        ERRORS=$((ERRORS + 1))
        return 1
    fi
}

# 测试登录API
test_login_api() {
    echo -n "测试登录API... "
    RESPONSE=$(curl -s "$BACKEND_URL/api/auth/login" -X POST \
         -H "Content-Type: application/json" \
         -d "{\"username\":\"$TEST_USER\",\"password\":\"$TEST_PASS\"}")

    if echo "$RESPONSE" | grep -q "token"; then
        TOKEN=$(echo "$RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
        echo -e "${GREEN}✓ 通过${NC}"
        echo "TOKEN=$TOKEN"
        return 0
    else
        echo -e "${RED}✗ 失败${NC}"
        ERRORS=$((ERRORS + 1))
        return 1
    fi
}

# 测试API端点
test_api_endpoint() {
    local name=$1
    local endpoint=$2
    local token=$3

    echo -n "测试 $name ... "
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BACKEND_URL$endpoint" \
         -H "Authorization: Bearer $token")

    if [ "$HTTP_CODE" = "200" ]; then
        echo -e "${GREEN}✓ 通过 (200)${NC}"
        return 0
    elif [ "$HTTP_CODE" = "500" ]; then
        echo -e "${RED}✗ 失败 (500 - 服务器错误)${NC}"
        ERRORS=$((ERRORS + 1))
        return 1
    elif [ "$HTTP_CODE" = "404" ]; then
        echo -e "${RED}✗ 失败 (404 - 未找到)${NC}"
        ERRORS=$((ERRORS + 1))
        return 1
    elif [ "$HTTP_CODE" = "401" ]; then
        echo -e "${YELLOW}⚠ 警告 (401 - 未授权)${NC}"
        WARNINGS=$((WARNINGS + 1))
        return 1
    else
        echo -e "${YELLOW}⚠ 警告 (HTTP $HTTP_CODE)${NC}"
        WARNINGS=$((WARNINGS + 1))
        return 1
    fi
}

# 测试前端页面
test_frontend_page() {
    local name=$1
    local path=$2

    echo -n "测试前端页面: $name ... "
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$FRONTEND_URL$path")

    if [ "$HTTP_CODE" = "200" ]; then
        echo -e "${GREEN}✓ 通过${NC}"
        return 0
    else
        echo -e "${RED}✗ 失败 (HTTP $HTTP_CODE)${NC}"
        ERRORS=$((ERRORS + 1))
        return 1
    fi
}

# 测试Knife4j文档
test_api_docs() {
    echo -n "测试API文档... "
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BACKEND_URL/doc.html")

    if [ "$HTTP_CODE" = "200" ]; then
        echo -e "${GREEN}✓ 通过${NC}"
        return 0
    else
        echo -e "${RED}✗ 失败 (HTTP $HTTP_CODE)${NC}"
        ERRORS=$((ERRORS + 1))
        return 1
    fi
}

# 执行测试
echo "1. 后端API测试"
echo "----------------"
test_backend_health

# 获取Token
RESPONSE=$(curl -s "$BACKEND_URL/api/auth/login" -X POST \
     -H "Content-Type: application/json" \
     -d "{\"username\":\"$TEST_USER\",\"password\":\"$TEST_PASS\"}")
TOKEN=$(echo "$RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -n "$TOKEN" ]; then
    echo ""
    echo "2. 核心API端点测试"
    echo "-------------------"
    test_api_endpoint "分类树" "/api/basic/category/tree" "$TOKEN"
    test_api_endpoint "位置列表" "/api/basic/location/all" "$TOKEN"
    test_api_endpoint "元数据(入库原因)" "/api/basic/metadata/type/STOCK_IN_REASON" "$TOKEN"
    test_api_endpoint "标准物质列表" "/api/materials?current=1&size=10" "$TOKEN"
    test_api_endpoint "库存列表" "/api/stocks?current=1&size=10" "$TOKEN"
    test_api_endpoint "入库记录" "/api/stock-in?current=1&size=10" "$TOKEN"
    test_api_endpoint "出库记录" "/api/stock-out?current=1&size=10" "$TOKEN"
    test_api_endpoint "采购列表" "/api/purchase?current=1&size=10" "$TOKEN"
    test_api_endpoint "盘点列表" "/api/stock-check?current=1&size=10" "$TOKEN"
    test_api_endpoint "预警列表" "/api/alerts?status=0" "$TOKEN"
    test_api_endpoint "仪表盘统计" "/api/dashboard/stats" "$TOKEN"
    test_api_endpoint "角色列表" "/api/system/role" "$TOKEN"
    test_api_endpoint "用户列表" "/api/system/user?current=1&size=10" "$TOKEN"
fi

echo ""
echo "3. 前端页面测试"
echo "----------------"
test_frontend_page "登录页" "/login"
test_frontend_page "首页" "/dashboard"

echo ""
echo "4. API文档测试"
echo "---------------"
test_api_docs

# 输出总结
echo ""
echo "======================================"
echo "  测试总结"
echo "======================================"
echo -e "错误: ${RED}$ERRORS${NC}"
echo -e "警告: ${YELLOW}$WARNINGS${NC}"
echo ""

if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✓ 所有测试通过!${NC}"
    exit 0
else
    echo -e "${RED}✗ 有 $ERRORS 个测试失败${NC}"
    exit 1
fi
