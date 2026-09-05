# 记账本 App（Android 前端）

记账本应用的 Android 客户端，配合后端 [accounting-app-backend](https://github.com/mengge237/accounting-app-backend) 使用。

## ✨ 功能特性

- 🔐 **用户体系**：注册 / 登录 / 找回密码（JWT 鉴权）
- 📒 **记账核心**：账单增删改查、账本管理、收支分类
- 📊 **统计报表**：收支统计、分类汇总、趋势视图
- 📅 **日程提醒**：日程管理与提醒
- 🍳 **扩展模块**：菜谱、分类（菜系）、背景图
- 🎨 **界面定制**：主题设置、布局设置、组件管理、动态布局构建

## 🛠 技术栈

| 层 | 技术 |
|---|---|
| 语言 | Java |
| UI | AndroidX / Material Design / ConstraintLayout |
| 网络 | Retrofit 2 / OkHttp / Gson |
| 本地存储 | SQLite（含自选分类等本地数据） |
| 构建 | Gradle（AGP），minSdk 24 / targetSdk 34 |

## 📁 项目结构

```
├── app/
│   └── src/main/java/com/smxy/myapplication/
│       ├── activity/      # 界面（登录/注册/主容器/设置等）
│       ├── fragment/      # 页面片段（收支/统计/分类等）
│       ├── adapter/       # RecyclerView 适配器
│       ├── network/       # RetrofitClient / ApiService / ApiClient
│       ├── model/         # 数据模型
│       ├── db/            # 本地 SQLite
│       └── builder/       # 动态布局构建
├── core/                  # 公共模块（libs/src）
├── build.gradle / settings.gradle
└── gradle/
```

## 🚀 快速开始

```bash
# 1. Android Studio 打开项目根目录，等待 Gradle 同步

# 2. 启动后端（见 accounting-app-backend，默认 8080 端口）
#    https://github.com/mengge237/accounting-app-backend

# 3. 网络配置（app/src/main/java/.../network/RetrofitClient.java）
#    模拟器默认  http://10.0.2.2:8080/   （访问宿主机）
#    真机请改为  后端所在电脑的局域网 IP

# 4. Run ▶ 运行到模拟器/真机
```

## 🔗 配套后端

- [accounting-app-backend](https://github.com/mengge237/accounting-app-backend)：Express + MySQL + JWT 的服务端 API
