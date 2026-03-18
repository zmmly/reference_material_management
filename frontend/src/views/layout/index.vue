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

        <el-sub-menu index="basic">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>基础数据</span>
          </template>
          <el-menu-item index="/basic/material">标准物质</el-menu-item>
          <el-menu-item index="/basic/category">分类管理</el-menu-item>
          <el-menu-item index="/basic/location">位置管理</el-menu-item>
          <el-menu-item index="/basic/metadata">元数据配置</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="stock">
          <template #title>
            <el-icon><Box /></el-icon>
            <span>库存管理</span>
          </template>
          <el-menu-item index="/stock">库存查询</el-menu-item>
          <el-menu-item index="/stock-in">入库登记</el-menu-item>
          <el-menu-item index="/stock-out">出库管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="purchase">
          <template #title>
            <el-icon><ShoppingCart /></el-icon>
            <span>采购管理</span>
          </template>
          <el-menu-item index="/purchase">采购申请</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="check">
          <template #title>
            <el-icon><DocumentChecked /></el-icon>
            <span>盘点管理</span>
          </template>
          <el-menu-item index="/stock-check">盘点任务</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="alert">
          <template #title>
            <el-icon><Bell /></el-icon>
            <span>预警中心</span>
          </template>
          <el-menu-item index="/alert">预警管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="system">
          <template #title>
            <el-icon><Tools /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/user">用户管理</el-menu-item>
          <el-menu-item index="/system/role">角色管理</el-menu-item>
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
  background: linear-gradient(180deg, var(--bg-primary) 0%, var(--bg-secondary) 100%);
  border-right: 1px solid var(--border-color);
  transition: width 0.3s ease;

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    padding: 0 16px;
    border-bottom: 1px solid var(--border-color);
    position: relative;

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 20px;
      right: 20px;
      height: 1px;
      background: linear-gradient(90deg, transparent, var(--accent-purple), transparent);
      opacity: 0.5;
    }

    .logo-icon {
      font-size: 24px;
    }

    .logo-text {
      color: var(--text-primary);
      font-size: 16px;
      font-weight: 600;
      background: var(--gradient-purple);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
  }
}

// 深色菜单样式
.dark-menu {
  background: transparent !important;
  border-right: none !important;
  padding: 8px;

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    color: var(--text-secondary) !important;
    border-radius: var(--radius-sm);
    margin: 2px 0;
    transition: var(--transition-normal);

    &:hover {
      background: rgba(139, 92, 246, 0.1) !important;
      color: var(--text-primary) !important;
    }
  }

  :deep(.el-menu-item.is-active) {
    background: linear-gradient(90deg, rgba(139, 92, 246, 0.2), transparent) !important;
    color: var(--accent-purple) !important;
    border-left: 3px solid var(--accent-purple);
    padding-left: 17px !important;
  }

  :deep(.el-sub-menu) {
    .el-menu {
      background: transparent !important;
    }

    .el-menu-item {
      padding-left: 50px !important;
      font-size: 14px;
    }
  }

  :deep(.el-menu--collapse) {
    .el-menu-item,
    .el-sub-menu__title {
      justify-content: center;
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
