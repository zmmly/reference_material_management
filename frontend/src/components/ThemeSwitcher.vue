<template>
  <el-dropdown trigger="click" @command="handleThemeChange">
    <div class="theme-switcher">
      <span class="theme-icon">{{ currentThemeInfo?.icon }}</span>
      <span class="theme-name">{{ currentThemeInfo?.name }}</span>
    </div>
    <template #dropdown>
      <el-dropdown-menu class="theme-dropdown">
        <el-dropdown-item
          v-for="theme in themes"
          :key="theme.key"
          :command="theme.key"
          :class="{ 'is-active': currentTheme === theme.key }"
        >
          <span class="theme-option">
            <span class="theme-option-icon">{{ theme.icon }}</span>
            <span class="theme-option-name">{{ theme.name }}</span>
            <el-icon v-if="currentTheme === theme.key" class="check-icon"><Check /></el-icon>
          </span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { computed } from 'vue'
import { useTheme } from '@/composables/useTheme'

const { currentTheme, themes, setTheme, getCurrentThemeInfo } = useTheme()

const currentThemeInfo = computed(() => getCurrentThemeInfo())

const handleThemeChange = (themeKey) => {
  setTheme(themeKey)
}
</script>

<style lang="scss" scoped>
.theme-switcher {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: var(--transition-fast);
  color: var(--text-secondary);

  &:hover {
    background: rgba(var(--accent-purple-rgb, 139, 92, 246), 0.1);
    color: var(--text-primary);
  }
}

.theme-icon {
  font-size: 18px;
}

.theme-name {
  font-size: 14px;
}

.theme-dropdown {
  min-width: 160px;

  :deep(.el-dropdown-menu__item) {
    padding: 10px 16px;

    &.is-active {
      background: rgba(var(--accent-purple-rgb, 139, 92, 246), 0.1);
      color: var(--accent-purple);
    }
  }
}

.theme-option {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.theme-option-icon {
  font-size: 18px;
}

.theme-option-name {
  flex: 1;
}

.check-icon {
  color: var(--accent-purple);
  font-size: 16px;
}
</style>
