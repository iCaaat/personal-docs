# Personal Docs

个人文档库的开发起点：Spring Boot 3 + Vue 3 + MySQL（生产）/H2（本地开发）。

## 当前能力

- 用户名密码登录与 JWT 鉴权（首次启动默认管理员 `admin` / `ChangeMe_123!`）
- 文档上传、列表、下载、删除
- PDF、HTML、Markdown 浏览器内预览；Word 先下载，生产环境可接入 LibreOffice 转 PDF
- 按用户隔离文档，并预留管理员角色
- Redis 双令牌登录：30 分钟访问令牌、7 天可轮换刷新令牌；连续错误 5 次后按 30 秒、60 秒、120 秒递增锁定，最高 2 小时

## 本地启动

前置：JDK 17、Apache Maven 3.9+、Node.js 20。

```powershell
cd backend
mvn spring-boot:run

cd ../frontend
npm install
npm run dev
```

访问 `http://localhost:5173`。前端会将 `/api` 代理到 `http://localhost:8080`。

本地 Redis 默认连接 `localhost:6379`，可使用 `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD` 覆盖。Redis 不可用时，登录和令牌刷新会失败，以避免绕过登录保护。

## 生产部署方向

使用 Docker Compose 运行 `app + mysql + nginx`；文件目录挂载到宿主机，后续接腾讯云 COS 可替换 `LocalStorageService`。密钥、初始管理员密码必须通过环境变量覆盖。

Word 预览建议在服务器安装 LibreOffice（headless），上传后异步转换为 PDF 并复用本项目 PDF 预览页。

## Office 在线预览

后端已提供 `GET /api/documents/{id}/preview`，会将 `doc/docx/xls/xlsx/ppt/pptx` 转换为 PDF 后返回，并缓存转换结果。Docker 镜像已包含 LibreOffice；直接部署 Java 服务时，请在 Ubuntu 安装：

```bash
sudo apt update
sudo apt install -y libreoffice-core libreoffice-writer libreoffice-calc libreoffice-impress
```

可通过 `LIBRE_OFFICE_COMMAND` 和 `PREVIEW_TIMEOUT_SECONDS` 覆盖可执行命令及转换超时（默认 120 秒）。
