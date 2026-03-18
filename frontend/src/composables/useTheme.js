import { ref, watch, onMounted } from 'vue'

// 主题配置
export const themes = [
  { key: 'dark-tech', name: '深色科技', icon: '🌙' },
  { key: 'light-minimal', name: '极简白', icon: '☀️' },
  { key: 'forest-green', name: '护眼绿', icon: '🌿' },
  { key: 'warm-orange', name: '暖橙', icon: '🌅' },
  { key: 'elegant-purple', name: '优雅紫', icon: '💎' },
]

const STORAGE_KEY = 'rmm-theme'
const DEFAULT_THEME = 'dark-tech'

// 全局状态
const currentTheme = ref(DEFAULT_THEME)

// 应用主题到 DOM
function applyTheme(themeKey) {
  document.documentElement.setAttribute('data-theme', themeKey)
  // 同步更新 Element Plus 的暗黑模式
  if (themeKey === 'light-minimal') {
    document.documentElement.classList.remove('dark')
  } else {
    document.documentElement.classList.add('dark')
  }
}

// 切换主题
function setTheme(themeKey) {
  const theme = themes.find(t => t.key === themeKey)
  if (!theme) {
    console.warn(`Theme "${themeKey}" not found, using default`)
    themeKey = DEFAULT_THEME
  }

  currentTheme.value = themeKey
  applyTheme(themeKey)
  localStorage.setItem(STORAGE_KEY, themeKey)
}

// 获取当前主题信息
function getCurrentThemeInfo() {
  return themes.find(t => t.key === currentTheme.value) || themes[0]
}

// 初始化主题
function initTheme() {
  const savedTheme = localStorage.getItem(STORAGE_KEY)
  const themeKey = savedTheme && themes.find(t => t.key === savedTheme)
    ? savedTheme
    : DEFAULT_THEME

  currentTheme.value = themeKey
  applyTheme(themeKey)
}

// Composable
export function useTheme() {
  onMounted(() => {
    initTheme()
  })

  return {
    currentTheme,
    themes,
    setTheme,
    getCurrentThemeInfo,
    initTheme,
  }
}

// 导出单例方法供非组件使用
export const themeService = {
  get current() {
    return currentTheme.value
  },
  setTheme,
  getCurrentThemeInfo,
  initTheme,
}
