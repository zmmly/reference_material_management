<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <div class="logo">
        <span class="logo-icon">🎯</span>
        <span v-show="!isCollapse" class="logo-text">标准物质管理系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        router
        class="dark-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>

        <el-sub-menu index="basic" v-if="canAccess('basic')">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>基础数据</span>
          </template>
          <el-menu-item index="/basic/material">标准物质</el-menu-item>
          <el-menu-item index="/basic/category">分类管理</el-menu-item>
          <el-menu-item index="/basic/location">位置管理</el-menu-item>
          <el-menu-item index="/basic/supplier">供应商管理</el-menu-item>
          <el-menu-item index="/basic/metadata">元数据配置</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="purchase" v-if="canAccess('purchase')">
          <template #title>
            <el-icon><ShoppingCart /></el-icon>
            <span>采购管理</span>
          </template>
          <el-menu-item index="/purchase">采购申请</el-menu-item>
          <el-menu-item index="/purchase-acceptance">采购验收</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="stock" v-if="canAccess('stock')">
          <template #title>
            <el-icon><Box /></el-icon>
            <span>库存管理</span>
          </template>
          <el-menu-item index="/stock">库存查询</el-menu-item>
          <el-menu-item index="/stock-in">入库登记</el-menu-item>
          <el-menu-item index="/stock-out">出库管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="check" v-if="canAccess('check')">
          <template #title>
            <el-icon><DocumentChecked /></el-icon>
            <span>盘点管理</span>
          </template>
          <el-menu-item index="/stock-check">盘点任务</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="alert" v-if="canAccess('alert')">
          <template #title>
            <el-icon><Bell /></el-icon>
            <span>预警中心</span>
          </template>
          <el-menu-item index="/alert">预警管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="system" v-if="canAccess('system')">
          <template #title>
            <el-icon><Tools /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/user">用户管理</el-menu-item>
          <el-menu-item index="/system/role">角色管理</el-menu-item>
          <el-menu-item index="/system/operation-log">操作日志</el-menu-item>
          <el-menu-item index="/system/backup">系统备份</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
          <component :is="isCollapse ? 'Expand' : 'Fold'" />
        </el-icon>
        <div class="header-right">
          <ThemeSwitcher />
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              {{ userStore.userInfo?.realName || userStore.userInfo?.username || '用户' }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import ThemeSwitcher from '@/components/ThemeSwitcher.vue'
import { useTheme } from '@/composables/useTheme'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 初始化主题
useTheme()

const isCollapse = ref(false)
const activeMenu = computed(() => route.path)

// 角色权限配置
const rolePermissions = {
  ADMIN: ['basic', 'stock', 'purchase', 'check', 'alert', 'system'],
  MANAGER: ['basic', 'stock', 'purchase', 'check', 'alert'],
  USER: ['stock', 'check']
}

// 检查用户是否有权限访问某个模块
const canAccess = (module) => {
  const roleCode = userStore.userInfo?.roleCode
  if (!roleCode) return false
  const permissions = rolePermissions[roleCode] || []
  return permissions.includes(module)
}

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style lang="scss" scoped>
.layout-container {
  height: 100vh;
  background: var(--bg-primary);
}

.aside {
  background: var(--sidebar-bg);
  box-shadow:
    2px 0 8px rgba(0, 0, 0, 0.15),
    inset -1px 0 0 rgba(255, 255, 255, 0.03);
  transition: width 0.3s ease;
  position: relative;
  z-index: 100;

  // 顶部装饰光效
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 120px;
    background: linear-gradient(180deg, rgba(139, 92, 246, 0.08) 0%, transparent 100%);
    pointer-events: none;
    z-index: 0;
  }

  .logo {
    height: 64px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    padding: 0 16px;
    position: relative;
    z-index: 1;

    // Logo区域底部渐变分隔线
    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 16px;
      right: 16px;
      height: 1px;
      background: linear-gradient(90deg,
        transparent 0%,
        rgba(139, 92, 246, 0.3) 20%,
        rgba(139, 92, 246, 0.5) 50%,
        rgba(139, 92, 246, 0.3) 80%,
        transparent 100%
      );
    }

    .logo-icon {
      font-size: 26px;
      filter: drop-shadow(0 2px 4px rgba(139, 92, 246, 0.3));
    }

    .logo-text {
      color: var(--text-primary);
      font-size: 15px;
      font-weight: 600;
      letter-spacing: 0.5px;
      background: var(--gradient-purple);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
  }
}

// 深色菜单样式 - 增强层次感
.dark-menu {
  background: transparent !important;
  border-right: none !important;
  padding: 12px 8px;
  position: relative;
  z-index: 1;

  // 一级菜单项
  :deep(.el-menu-item) {
    color: var(--text-secondary) !important;
    border-radius: 8px;
    margin: 3px 4px;
    height: 44px;
    transition: all 0.25s ease;
    position: relative;

    .el-icon {
      font-size: 18px;
      transition: transform 0.25s ease;
    }

    &:hover {
      background: rgba(139, 92, 246, 0.12) !important;
      color: var(--text-primary) !important;
      transform: translateX(2px);

      .el-icon {
        transform: scale(1.1);
      }
    }

    &.is-active {
      background: linear-gradient(90deg, rgba(139, 92, 246, 0.25), rgba(139, 92, 246, 0.08)) !important;
      color: var(--accent-purple) !important;
      font-weight: 500;
      box-shadow:
        inset 3px 0 0 var(--accent-purple),
        0 2px 8px rgba(139, 92, 246, 0.15);

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 8px;
        bottom: 8px;
        width: 3px;
        background: var(--accent-purple);
        border-radius: 0 2px 2px 0;
        box-shadow: 0 0 8px var(--accent-purple);
      }
    }
  }

  // 一级子菜单标题
  :deep(.el-sub-menu__title) {
    color: var(--text-secondary) !important;
    border-radius: 8px;
    margin: 3px 4px;
    height: 44px;
    transition: all 0.25s ease;

    .el-icon {
      font-size: 18px;
      transition: transform 0.25s ease;
    }

    &:hover {
      background: rgba(139, 92, 246, 0.12) !important;
      color: var(--text-primary) !important;

      .el-icon {
        transform: scale(1.1);
      }
    }
  }

  // 展开的子菜单标题
  :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
    color: var(--accent-purple) !important;
    font-weight: 500;

    .el-icon {
      color: var(--accent-purple) !important;
    }
  }

  // 子菜单容器
  :deep(.el-sub-menu) {
    .el-menu {
      background: rgba(0, 0, 0, 0.15) !important;
      border-radius: 8px;
      margin: 4px 8px 8px 8px;
      padding: 4px 0;
      box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.1);
    }

    // 二级菜单项
    .el-menu-item {
      padding-left: 52px !important;
      height: 38px;
      font-size: 13px;
      color: var(--text-muted) !important;
      margin: 1px 4px;
      border-radius: 6px;

      &::before {
        content: '';
        position: absolute;
        left: 32px;
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: var(--text-muted);
        opacity: 0.4;
        transition: all 0.25s ease;
      }

      &:hover {
        background: rgba(139, 92, 246, 0.1) !important;
        color: var(--text-secondary) !important;

        &::before {
          background: var(--accent-purple);
          opacity: 1;
          box-shadow: 0 0 6px var(--accent-purple);
        }
      }

      &.is-active {
        background: rgba(139, 92, 246, 0.15) !important;
        color: var(--accent-purple) !important;
        font-weight: 500;

        &::before {
          background: var(--accent-purple);
          opacity: 1;
          box-shadow: 0 0 6px var(--accent-purple);
        }

        box-shadow: none;
      }
    }
  }

  // 折叠状态
  :deep(.el-menu--collapse) {
    .el-menu-item,
    .el-sub-menu__title {
      justify-content: center;
      padding-left: 0 !important;

      .el-icon {
        margin-right: 0;
      }
    }
  }
}

.header {
  background: transparent;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 64px;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent, var(--border-color), transparent);
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  font-size: 20px;
  color: var(--text-secondary);
  transition: var(--transition-fast);

  &:hover {
    color: var(--accent-purple);
  }
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  transition: var(--transition-fast);
  padding: 8px 12px;
  border-radius: var(--radius-sm);

  &:hover {
    background: rgba(139, 92, 246, 0.1);
    color: var(--accent-purple);
  }
}

.main {
  background: var(--bg-primary);
  padding: 20px;
}
</style>
