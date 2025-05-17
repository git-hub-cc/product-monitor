# 商品抢购监控系统

一个基于Java Swing的图形化商品抢购监控系统，支持多商品同时监控、价格追踪和自动下单功能。

## 功能特点

- 📊 图形化界面，操作简单直观
- 🔄 支持多商品同时监控
- 💰 实时价格追踪和比对
- 📝 详细的操作日志记录
- 🛍️ 自动下单功能
- ⚙️ 可配置的监控参数
- 🔔 状态实时更新
- 🎯 支持动态修改目标价格
- 📁 分类存储的日志管理

## 系统要求

- JDK 11或更高版本
- Maven 3.6或更高版本
- 操作系统：Windows/Linux/MacOS

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/git-hub-cc/product-monitor.git
cd product-monitor
```

### 2. 配置文件

在项目根目录创建`config.properties`文件：

```properties
# API配置
TIME_MILLISECONDS=2000
SHORT_NAME=YE
DEV_TYPE=2
DELAY_HOURS =5
MAX_RETRIES=3
RETRY_DELAY=1000

# API端点URL配置
ORDER_URL=https\://api.x-metash.cn/h5/order/unifiedPay
SEARCH_URL=https\://api.x-metash.cn/h5/home/searchApp
ARCHIVE_URL=https\://api.x-metash.cn/h5/goods/archive
PRE_CREATE_URL=https\://api.x-metash.cn/h5/goods/preCreate
CREATE_URL=https\://api.x-metash.cn/h5/goods/create/v2
UNIFIED_PAY_URL=https\://api.x-metash.cn/h5/goods/create/unifiedPay
```

### 3. 编译运行

```bash
mvn clean package
java -jar target/product-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## 使用说明

### 主窗口

1. 添加商品监控
    - 输入商品名称
    - 输入目标价格
    - 点击"添加监控"按钮

2. 商品列表管理
    - 查看所有监控中的商品
    - 打开/关闭监控窗口
    - 删除商品监控

### 监控窗口

1. 状态显示
    - 当前价格
    - 目标价格
    - 监控状态

2. 操作控制
    - 停止/继续监控
    - 修改目标价格
    - 查看日志文件

3. 日志记录
    - 实时价格更新
    - 操作记录
    - 错误信息

## 配置说明
- `TIME_MILLISECONDS`: 监控间隔时间（毫秒）
- `MAX_RETRIES`: 最大重试次数
- `RETRY_DELAY`: 重试延迟时间


- `DEV_TYPE`：支付方式
- `DELAY_HOURS`：预售时间比发行延后数小时


## 日志管理

### 日志存储结构

```
logs/
├── 商品1/
│   └── 商品1_2024-01-01.log
├── 商品2/
│   └── 商品2_2024-01-01.log
└── ...
```

### 日志清理

- 通过工具菜单进行日志清理
- 支持按天数保留日志
- 自动清理过期日志

## 开发说明

### 添加新功能

1. 实现相关接口
2. 注册到监控系统
3. 更新配置文件

### 自定义监控逻辑

修改`ProductMonitor`类，可以修改购买逻辑：

```java
public void startMonitoring() {
    // 自定义监控逻辑
}
```

修改`ProductRelease`类，可以修改发布预购逻辑：

```java
public void publishPreOrder() {
}
```

## 常见问题

1. 配置文件找不到
    - 确认config.properties位于正确位置,新版本可能需要更新
    - 检查文件权限

2. 日志文件创建失败
    - 检查目录权限
    - 确保磁盘空间充足

3. 监控无响应
    - 检查网络连接
    - 验证TOKEN有效性

## 贡献指南

1. Fork 项目
2. 创建特性分支
3. 提交改动
4. 发起Pull Request

## 版本历史

- v1.0.0 (2025-05-10)
    - 初始版本发布
    - 基础监控功能
    - 图形化界面


## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 免责声明

本项目仅供学习和研究使用，请勿用于商业用途。使用本项目造成的任何问题，作者不承担任何责任。

---

如有问题或建议，欢迎提交 Issue 或 Pull Request。
