# 云笺文档库｜SDLC 项目说明

> 文档版本：0.1（当前开发阶段）  
> 项目定位：部署在个人服务器上的私有文档库，用于安全保存、管理和在线查看个人文档。

这份文档同时站在使用者和开发者角度记录项目进度。它不是严格的企业流程文件，重点是让后续开发、部署和功能取舍更清晰。

## 1. 需求阶段

### 1.1 已确认的核心需求

- 用户需要登录后才能管理自己的文档。
- 文档支持上传、列表查看、搜索、下载和删除。
- 常见文档需要尽量在线预览：Markdown、HTML、PDF、Word、Excel、PowerPoint。
- 系统部署在 Ubuntu 22.04 云服务器，数据保留在自己的服务器中。
- 文档要按用户隔离，不能通过猜测文件地址访问别人的文件。
- 登录要具备基础防暴力破解能力。

### 1.2 MoSCoW 分析

MoSCoW 用于把功能按优先级分组：Must（必须）、Should（应该）、Could（可以有）、Won't（当前暂不做）。

| 优先级 | 内容 | 当前状态 |
| --- | --- | --- |
| Must | 登录、JWT 鉴权、用户文档隔离 | 已完成 |
| Must | 文档上传、列表、下载、删除 | 已完成 |
| Must | Markdown、HTML、PDF 在线预览 | 已完成 |
| Must | Office 转 PDF 后预览 | 已完成，依赖服务器 LibreOffice |
| Must | Redis 登录失败锁定、双 Token 刷新 | 已完成 |
| Should | 移动端适配、搜索、文档类型展示 | 已完成基础版本 |
| Should | 文件大小/类型白名单、上传进度、错误提示优化 | 待完善 |
| Should | 管理员管理用户、查看存储使用情况 | 待规划 |
| Could | 文档目录、标签、收藏、回收站 | 待规划 |
| Could | 腾讯云 COS 备份、全文检索、分享链接 | 待规划 |
| Could | Office 转换任务队列和预览缓存管理 | 待规划 |
| Won't | 多人协同编辑、实时评论、复杂工作流 | 当前版本不做 |

### 1.3 当前的边界与未知项

- 当前是“个人文档库”定位，不建议直接用于医疗、财务等高敏感生产数据。
- Office 转换效果受 LibreOffice 版本和原始文件复杂度影响；复杂图表、宏、特殊字体可能与 Microsoft Office 有差异。
- 文件目前保存在服务器磁盘。磁盘损坏、误删或云服务器故障可能导致数据丢失，因此上线后应配置备份。
- 当前默认只有初始化管理员账号；多用户注册、找回密码、邮件通知尚未实现。

## 2. 设计阶段

设计阶段主要确定系统的整体架构、技术选型以及核心功能的实现方案。本项目以“个人私有文档库”为目标，因此优先考虑安全性、易部署和低维护成本，而不是高并发或复杂协同功能。

### 2.1 系统架构设计

系统采用前后端分离架构，由以下几个部分组成：

- 前端（Vue 3）：负责页面展示、文件上传和在线预览。
- 后端（Spring Boot）：负责用户认证、权限控制、文档管理和预览接口。
- MySQL：保存用户信息和文档元数据。
- Redis：保存刷新 Token、登录失败次数以及账号锁定状态。
- LibreOffice：负责 Office 文档转换为 PDF。
- Docker Compose：统一管理服务部署。
- Nginx：负责静态资源服务与反向代理。

整体架构如下【图】：

> 浏览器
>  |
>  v
>  Vue3 + Element Plus
>  |
>  v
>  Spring Boot API
>  |
>  +-----------------------------+
>
> |      |      |      |
> | ---- | ---- | ---- |
> |      |      |      |
>  v v v v
>  MySQL Redis LibreOffice 文件存储目录

### 2.2 功能模块设计

系统目前划分为四个核心模块：

| 模块         | 作用                               |
| ------------ | ---------------------------------- |
| 用户认证模块 | 登录、JWT 鉴权、刷新 Token 管理    |
| 文档管理模块 | 上传、查询、下载、删除             |
| 文档预览模块 | Markdown、PDF、Office 文档在线查看 |
| 安全控制模块 | 登录失败限制、权限校验、文件隔离   |

其中，每个模块之间通过 REST API 进行交互，尽量降低模块之间的耦合。

### 2.3 文档存储设计

为了避免用户能够通过文件路径直接访问服务器文件，系统采用：

- 原始文件名与实际存储文件名分离。
- 文件上传后使用随机 UUID 命名。
- 数据库存储文件元数据。
- 文件访问必须经过后端权限校验。

例如：

> 用户上传：
>
> 简历.docx
>
> ↓
>
> 服务器实际存储：
>
> e6b31f6f-acde-xxxx.docx
>
> ↓
>
> 数据库记录：
>
> 用户ID
>  文件名称
>  文件类型
>  文件大小
>  存储路径
>  上传时间

用户无法通过猜测文件路径访问其他人的文件。

### 2.4 文档预览设计

不同类型的文件采用不同的预览方案：

| 文件类型   | 方案                   |
| ---------- | ---------------------- |
| Markdown   | markdown-it 渲染 HTML  |
| HTML       | iframe 沙箱模式预览    |
| PDF        | vue-pdf-embed 在线查看 |
| Word       | LibreOffice 转 PDF     |
| Excel      | LibreOffice 转 PDF     |
| PowerPoint | LibreOffice 转 PDF     |

Office 文件统一转换为 PDF 后进行预览，主要考虑：

- 浏览器兼容性更好。
- 无需依赖 Microsoft Office Online。
- 文件不会上传到第三方服务器。
- 更适合私有部署场景。

虽然无法做到完全还原 Microsoft Office 的显示效果，但能够满足个人文档浏览需求。

### 2.5 安全设计

当前版本采用以下安全策略：

1. JWT 双 Token 登录机制。
2. BCrypt 加密存储用户密码。
3. Redis 登录失败计数与账号锁定。
4. 文件访问必须通过身份校验。
5. 文件路径随机化存储。
6. HTML 文档使用受限 iframe 预览。
7. 建议生产环境启用 HTTPS。

登录失败锁定机制：

> 第1次锁定：30 秒
>  ↓
>  第2次锁定：1 分钟
>  ↓
>  第3次锁定：2 分钟
>  ↓
>  ......
>  ↓
>  最长锁定：2 小时

该方案能够有效降低暴力破解风险。

### 2.6 技术选型说明

| 技术            | 选择原因                     |
| --------------- | ---------------------------- |
| Vue 3           | 生态成熟，适合中小型项目开发 |
| Element Plus    | 中文资料丰富，组件完善       |
| Spring Boot 3   | Java 主流开发框架            |
| Spring Security | 权限控制与 JWT 支持较完善    |
| MySQL           | 数据存储稳定可靠             |
| Redis           | Token 管理与缓存支持         |
| LibreOffice     | 支持离线转换 Office 文档     |
| Docker Compose  | 部署简单，适合个人服务器     |
| Nginx           | 静态资源服务与反向代理       |

整体技术方案优先考虑：

- 易部署。
- 易维护。
- 不依赖第三方在线服务。
- 能够长期运行在低配置云服务器上。

## 3. 开发阶段

### 3.1 当前技术方案

| 层级 | 方案 | 作用 |
| --- | --- | --- |
| 前端 | Vue 3、Vite、Element Plus | 中文界面、上传、文档卡片、预览弹窗 |
| Markdown 预览 | markdown-it、highlight.js | Markdown 排版与代码高亮 |
| PDF 预览 | vue-pdf-embed | PDF 翻页查看 |
| 后端 | Spring Boot 3、Spring Security、JPA | REST 接口、权限、文档元数据 |
| 数据库 | MySQL 8 | 用户和文档信息 |
| 缓存/安全 | Redis 5 | 刷新令牌、登录失败计数与锁定 |
| Office 预览 | LibreOffice headless | 将 Office 文档转换为 PDF |
| 部署 | Docker Compose、Nginx | 统一运行 Web、API、MySQL、Redis |

### 3.2 已完成开发内容

1. **账户与安全**
   - 用户名和 BCrypt 密码校验。
   - 短期访问 Token（默认 30 分钟）与刷新 Token（默认 7 天）。
   - 刷新 Token 存在 Redis 中，并在刷新后轮换，旧 Token 立即失效。
   - 连续输错 5 次密码后锁定：30 秒起，每轮翻倍，最长 2 小时。

2. **文档管理**
   - 上传、文档列表、删除、下载。
   - 每份文档绑定上传用户，接口按当前用户查询。
   - 本地文件采用随机存储名，避免直接暴露原始文件路径。

3. **在线预览**
   - Markdown：浏览器渲染，支持常见代码块高亮。
   - HTML：使用受限 iframe 预览。
   - PDF：使用前端 PDF 查看器。
   - Word、Excel、PowerPoint：后端调用 LibreOffice 生成 PDF，前端复用 PDF 查看器。

4. **界面**
   - 全中文登录页与文档库主页。
   - 文档类型标识、搜索、存储用量展示、删除确认。
   - 大尺寸弹窗预览，适配较小屏幕的基础布局。

### 3.3 建议的后续开发顺序

1. 增加文件后缀/大小白名单和更明确的上传失败提示。
2. 增加文档目录、标签、回收站，避免误删后无法恢复。
3. 为 Office 转换加入异步任务，避免大文件转换时接口等待过久。
4. 增加用户管理与密码修改功能。
5. 增加数据库迁移工具（Flyway）和接口测试。
6. 加入定时备份，或将原始文档备份至腾讯云 COS。

## 4. 测试阶段

当前已完成前端生产构建检查。后续建议按下面的简单清单执行人工测试，并逐步补充自动化测试。

### 4.1 功能测试清单

- 使用正确/错误密码登录；连续输错 5 次，确认锁定时间正确增长。
- 访问 Token 过期后，确认前端可自动使用刷新 Token 恢复登录。
- 上传 Markdown、HTML、PDF、docx、xlsx、pptx 文件并分别预览。
- 用两个账号上传文件，确认账号 A 无法通过接口访问账号 B 的文档。
- 删除文件后确认列表、磁盘文件和 Office 预览缓存均被清理。
- 上传超出限制的文件，确认页面有合理报错。

### 4.2 非功能检查

- 使用手机或缩窄浏览器窗口检查主要页面布局。
- 观察大 Office 文件转换时的内存和 CPU 使用情况。2 核 4G 服务器适合个人低并发使用，不建议多人同时转换大文件。
- 重启容器后检查 MySQL、Redis 和上传文件卷是否还在。

## 5. 部署阶段（Ubuntu 22.04）

以下以服务器已安装 Docker Engine 和 Docker Compose Plugin 为前提。MySQL、Redis、LibreOffice 已经被 Docker Compose 与后端镜像包含，不需要在宿主机额外安装。若选择不使用 Docker，请自行准备 Java 17、MySQL、Redis 和 LibreOffice。

### 5.1 服务器准备

1. 登录腾讯云 Ubuntu 22.04 服务器。

```bash
ssh ubuntu@你的服务器IP
sudo apt update && sudo apt upgrade -y
```

2. 安装 Docker（若尚未安装）。可使用 Docker 官方安装方式，安装后验证：

```bash
docker --version
docker compose version
```

3. 防火墙或腾讯云安全组只需放行实际要使用的端口。项目默认对外使用 `8088`；若前面有 Nginx/Caddy，可只开放 80/443。

### 5.2 上传项目与准备环境变量

1. 将项目上传到服务器，例如：

```bash
sudo mkdir -p /opt/personal-docs
sudo chown -R $USER:$USER /opt/personal-docs
cd /opt/personal-docs
# 通过 git clone、SCP 或 rsync 将项目文件放到此目录
```

2. 创建 `.env` 文件。不要把真实密码提交到 Git：

```bash
cp .env.example .env
nano .env
```

至少填写以下值：

```dotenv
JWT_SECRET=使用随机生成的至少32字符长密钥
DB_PASSWORD=为应用数据库设置的强密码
MYSQL_ROOT_PASSWORD=为MySQL root设置的强密码
BOOTSTRAP_ADMIN_PASSWORD=首次管理员账号admin的强密码
```

可用下面的命令生成 JWT 密钥：

```bash
openssl rand -base64 48
```

### 5.3 构建并启动项目

在项目根目录执行：

```bash
docker compose up -d --build
docker compose ps
```

第一次构建会下载 Java、Node、MySQL、Redis 与 LibreOffice 相关镜像/依赖，耗时相对较长。正常情况下会有四个服务：

- `web`：Nginx 托管 Vue 页面，对外暴露 8088。
- `api`：Spring Boot 接口和 LibreOffice 文档转换。
- `mysql`：保存用户与文档元数据。
- `redis`：保存刷新令牌和登录失败限制状态。

访问地址：`http://服务器IP:8088`。首次使用管理员账号 `admin` 和 `.env` 中的 `BOOTSTRAP_ADMIN_PASSWORD` 登录。

### 5.4 部署验证与日志查看

```bash
docker compose ps
docker compose logs -f api
docker compose logs -f web
```

建议依次验证：能否登录、上传 Markdown、预览 PDF、预览 xlsx。若 Office 预览失败，优先查看 `api` 日志中 LibreOffice 的转换错误。

### 5.5 域名与 HTTPS（建议上线时完成）

当前 Compose 将网站暴露在 `8088`。生产环境推荐：

1. 域名 A 记录指向腾讯云公网 IP。
2. 在宿主机或额外容器使用 Nginx/Caddy 监听 80 和 443。
3. 反向代理到 `http://127.0.0.1:8088`。
4. 使用 Let's Encrypt 申请 HTTPS 证书，并强制 HTTP 跳转 HTTPS。

使用 HTTPS 后，JWT、登录密码和文档传输才不会在公网明文传输。

### 5.6 数据、备份与日常维护

- Docker 的 `mysql-data`、`redis-data`、`document-data` 是持久卷；不要随意执行 `docker compose down -v`，这会删除卷中的数据。
- 至少每周备份一次 MySQL 数据库和上传文件卷。重要文档建议另备份到腾讯云 COS 或其他可信位置。
- 更新代码后的常用命令：

```bash
cd /opt/personal-docs
docker compose up -d --build
docker image prune -f
```

- 遇到问题时先查看日志，不要直接删除数据卷：

```bash
docker compose logs --tail=200 api
docker compose logs --tail=200 mysql
```

## 6. 当前验收结论

当前版本已达到“进入开发、可本地运行和可容器化部署”的目标。它适合个人学习和自用的文档管理场景。正式长期使用前，应至少完成 HTTPS、强密码、备份和上传限制四项工作。
