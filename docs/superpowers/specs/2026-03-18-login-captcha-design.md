# 登录验证码与安全加固设计方案

## 背景

当前登录页面仅支持用户名和密码认证，没有任何防暴力破解机制。系统需要部署在公网环境，存在以下安全风险：

1. 暴力破解攻击 - 攻击者可以无限次尝试密码
2. 字典攻击 - 自动化工具批量尝试常见密码组合
3. 账户枚举 - 通过响应差异判断用户是否存在

## 设计目标

- 增加图形验证码，阻止自动化攻击
- 实现登录失败次数限制，防止暴力破解
- 账户锁定机制，保护用户账户安全
- 保持良好的用户体验

## 技术方案

### 验证码机制

**类型**: 图形验证码（数字+字母混合）
**触发时机**: 始终显示
**存储方式**: 后端 Session 存储，5 分钟过期

**后端实现**:
- 使用 `com.github.whvcse/captcha` 库生成验证码
- 新增 `GET /api/auth/captcha` 接口
- 返回 captchaId 和 Base64 编码的图片

**前端实现**:
- 登录表单增加验证码输入框和图片
- 点击图片刷新验证码
- 验证码 4 位字符，忽略大小写

### 登录失败限制与账户锁定

**锁定策略**:
- 锁定键: IP + 用户名组合
- 触发条件: 连续失败 5 次
- 锁定时长: 30 分钟

**锁定行为**:
- 锁定期间即使密码正确也无法登录
- 返回剩余锁定时间提示

**存储方式**: Session 存储
- Key: `login_failed:{ip}:{username}`
- Value: `{count, firstFailTime}`
- 过期时间: 30 分钟

### Session 存储结构

```
captcha:{captchaId} → 验证码答案 (5分钟过期)
login_failed:{ip}:{username} → {count, firstFailTime} (30分钟过期)
```

## 实现细节

### 后端变更

**新增依赖** (`pom.xml`):
```xml
<dependency>
    <groupId>com.github.whvcse</groupId>
    <artifactId>captcha</artifactId>
    <version>1.0.0</version>
</dependency>
```

**新增/修改文件**:

| 文件 | 说明 |
|------|------|
| `controller/AuthController.java` | 新增 `getCaptcha()` 接口，修改 `login()` 增加验证 |
| `service/AuthService.java` | 增加验证码验证、失败次数检查、锁定检查逻辑 |
| `dto/LoginDTO.java` | 新增 `captchaId` 和 `captchaCode` 字段 |
| `vo/CaptchaVO.java` | 新增，返回验证码 ID 和 Base64 图片 |
| `util/CaptchaUtil.java` | 新增，验证码生成工具类 |

### 前端变更

**修改文件**:

| 文件 | 说明 |
|------|------|
| `views/login/index.vue` | 增加验证码输入框和图片显示 |
| `api/auth.js` | 新增 `getCaptcha()` API 调用 |
| `store/modules/user.js` | `login()` 方法增加验证码参数 |

**UI 布局**:
- 验证码输入框在密码框下方
- 验证码图片宽度 120px，高度与输入框一致
- 输入框和图片在同一行
- 图片支持点击刷新

**登录表单数据结构**:
```javascript
form = {
  username: '',
  password: '',
  captchaId: '',    // 验证码ID
  captchaCode: ''   // 用户输入的验证码
}
```

## API 变更

### 新增接口

**GET /api/auth/captcha**

获取验证码图片

响应:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "captchaId": "uuid-string",
    "captchaImage": "data:image/png;base64,..."
  }
}
```

### 修改接口

**POST /api/auth/login**

请求体新增字段:
```json
{
  "username": "admin",
  "password": "admin123",
  "captchaId": "uuid-string",
  "captchaCode": "A1B2"
}
```

## 错误处理

| 错误信息 | 场景 |
|----------|------|
| "验证码已过期，请刷新" | 验证码 Session 不存在或已过期 |
| "验证码错误" | 用户输入与实际不符 |
| "账户已锁定，请 X 分钟后重试" | 账户被锁定 |
| "用户名或密码错误，还剩 X 次尝试机会" | 密码错误，未锁定 |

## 安全考虑

1. **验证码验证失败不计入登录失败次数** - 防止攻击者通过故意输错验证码来消耗失败次数
2. **所有错误响应延迟 500ms** - 防止时序攻击（通过响应时间判断用户是否存在）
3. **验证码大小写不敏感** - 提升用户体验
4. **锁定基于 IP + 用户名组合** - 允许用户从其他 IP 正常登录
5. **验证码一次性使用** - 验证后立即删除，防止重放攻击
6. **易混淆字符排除** - 排除 0/O、1/I/l 等易混淆字符，提升用户体验

7. **锁定时可刷新验证码** - 账户锁定期间仍可刷新验证码

## 配置说明

- Session 超时需大于 30 分钟（建议 60 分钟），确保锁定机制正常工作
- 配置项: `server.servlet.session.timeout=3600` (Spring Boot 默认单位秒)

## 局限性说明
- 同一用户名被多 IP 同时攻击时，每个 IP 独立计算失败次数（5 次 × IP 数）
- 服务重启后 Session 丢失，需重新获取验证码

## 测试计划

1. 验证码生成和验证功能测试
2. 登录失败次数限制测试
3. 账户锁定和解锁测试
4. 前端 UI 交互测试
5. E2E 自动化测试更新
