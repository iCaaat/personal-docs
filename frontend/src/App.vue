<script setup>
import {computed, onMounted, ref, watch} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {
  Collection,
  Delete,
  Document,
  Download,
  Files,
  FolderOpened,
  Plus,
  Search,
  UserFilled,
  View
} from '@element-plus/icons-vue'
import MarkdownViewer from './components/MarkdownViewer.vue'
import PdfViewer from './components/PdfViewer.vue'
import {clearAuth, contentBlob, contentText, listDocs, login, previewPdfBlob, remove, upload} from './api'

const username = ref('admin'), password = ref('ChangeMe_123!'), docs = ref([]), keyword = ref('')
const loading = ref(false), uploading = ref(false), loggedIn = ref(!!localStorage.getItem('token'))
const previewVisible = ref(false), selected = ref(null), markdownContent = ref(''), pdfSource = ref(null),
    htmlSource = ref(''), previewLoading = ref(false)
const filteredDocs = computed(() => docs.value.filter(d => d.originalName.toLowerCase().includes(keyword.value.toLowerCase())))
const totalSize = computed(() => docs.value.reduce((sum, d) => sum + d.size, 0))
const formatSize = value => value < 1024 * 1024 ? `${(value / 1024).toFixed(1)} KB` : `${(value / 1024 / 1024).toFixed(1)} MB`
const formatDate = value => new Date(value).toLocaleString('zh-CN', {dateStyle: 'medium', timeStyle: 'short'})
const extension = d => d.originalName.split('.').pop().toLowerCase()
const viewerType = d => ['md', 'markdown'].includes(extension(d)) ? 'markdown' : extension(d) === 'pdf' ? 'pdf' : ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx'].includes(extension(d)) ? 'office' : ['html', 'htm'].includes(extension(d)) ? 'html' : 'unsupported'
const fileKind = d => extension(d) === 'pdf' ? 'PDF' : ['doc', 'docx'].includes(extension(d)) ? 'WORD' : ['xls', 'xlsx'].includes(extension(d)) ? 'EXCEL' : ['ppt', 'pptx'].includes(extension(d)) ? 'PPT' : ['md', 'markdown'].includes(extension(d)) ? 'MD' : '文件'
const refresh = async () => {
  loading.value = true;
  try {
    docs.value = await listDocs()
  } catch (e) {
    ElMessage.error(e.message);
    logout()
  } finally {
    loading.value = false
  }
}
onMounted(() => loggedIn.value && refresh())
const submit = async () => {
  try {
    await login(username.value, password.value);
    loggedIn.value = true;
    await refresh();
    ElMessage.success('登录成功，欢迎回来')
  } catch (e) {
    ElMessage.error(e.message)
  }
}
const logout = () => {
  clearAuth();
  loggedIn.value = false;
  docs.value = [];
  selected.value = null;
  previewVisible.value = false
}
const uploadFile = async ({file}) => {
  uploading.value = true;
  try {
    await upload(file);
    await refresh();
    ElMessage.success(`「${file.name}」上传成功`)
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    uploading.value = false
  }
}
const openPreview = d => {
  selected.value = d;
  previewVisible.value = true
}
const deleteFile = async d => {
  try {
    await ElMessageBox.confirm(`确定删除「${d.originalName}」吗？此操作不可恢复。`, '删除文档', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await remove(d.id);
    docs.value = docs.value.filter(item => item.id !== d.id);
    ElMessage.success('文档已删除')
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e.message || '删除失败')
  }
}
const download = async d => {
  try {
    const url = URL.createObjectURL(await contentBlob(d.id));
    const a = document.createElement('a');
    a.href = url;
    a.download = d.originalName;
    a.click();
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error(e.message)
  }
}
watch([selected, previewVisible], async ([d, visible]) => {
  if (htmlSource.value) URL.revokeObjectURL(htmlSource.value);
  markdownContent.value = '';
  pdfSource.value = null;
  htmlSource.value = '';
  if (!d || !visible) return;
  previewLoading.value = true;
  try {
    if (viewerType(d) === 'markdown') markdownContent.value = await contentText(d.id);
    if (viewerType(d) === 'html') htmlSource.value = URL.createObjectURL(await contentBlob(d.id));
    if (['pdf', 'office'].includes(viewerType(d))) {
      const blob = await previewPdfBlob(d.id);
      pdfSource.value = new Uint8Array(await blob.arrayBuffer())
    }
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    previewLoading.value = false
  }
})
</script>
<template>
  <main v-if="!loggedIn" class="login-page">
    <section class="login-card">
      <div class="login-visual">
        <div class="logo-mark">
          <el-icon>
            <FolderOpened/>
          </el-icon>
        </div>
        <h1>云笺文档库</h1>
        <p>安全存放，随时查阅你的每一份资料</p>
        <div class="visual-orb orb-one"></div>
        <div class="visual-orb orb-two"></div>
      </div>
      <el-form class="login-form" @submit.prevent="submit"><h2>欢迎登录</h2>
        <p class="muted">请输入账号信息以继续</p>
        <el-form-item>
          <el-input v-model="username" size="large" placeholder="用户名" :prefix-icon="UserFilled"
                    autocomplete="username"/>
        </el-form-item>
        <el-form-item>
          <el-input v-model="password" size="large" type="password" placeholder="密码" show-password
                    :prefix-icon="Document" autocomplete="current-password" @keyup.enter="submit"/>
        </el-form-item>
        <el-button type="primary" size="large" class="login-button" @click="submit">登录文档库</el-button>
        <p class="login-tip">首次启动账号：admin　默认密码：ChangeMe_123!</p></el-form>
    </section>
  </main>
  <el-container v-else class="app-shell">
    <el-aside width="248px" class="sidebar">
      <div class="brand">
        <div class="brand-icon">
          <el-icon>
            <FolderOpened/>
          </el-icon>
        </div>
        <span>云笺文档库</span></div>
      <el-upload :show-file-list="false" :http-request="uploadFile" :disabled="uploading" class="upload-entry">
        <el-button type="primary" size="large" :loading="uploading" class="upload-button">
          <el-icon>
            <Plus/>
          </el-icon>
          {{ uploading ? '正在上传' : '上传文档' }}
        </el-button>
      </el-upload>
      <nav>
        <div class="nav-item active">
          <el-icon>
            <Collection/>
          </el-icon>
          我的文档 <span>{{ docs.length }}</span></div>
      </nav>
      <div class="sidebar-footer">
        <div class="storage-label"><span>已用空间</span><b>{{ formatSize(totalSize) }}</b></div>
        <el-progress :percentage="Math.min(100,totalSize/1024/1024/1024*100)" :show-text="false" :stroke-width="6"/>
        <el-button text class="logout" @click="logout">退出登录</el-button>
      </div>
    </el-aside>
    <el-main class="content">
      <header class="topbar">
        <div><p class="eyebrow">个人空间</p>
          <h2>我的文档</h2></div>
        <el-avatar :size="38" class="avatar">管</el-avatar>
      </header>
      <section class="hero">
        <div><p>文档，井然有序</p><span>上传、预览和管理你的私人资料</span></div>
        <el-icon>
          <Files/>
        </el-icon>
      </section>
      <section class="toolbar">
        <el-input v-model="keyword" :prefix-icon="Search" placeholder="搜索文档名称" clearable/>
        <span>{{ filteredDocs.length }} 份文档</span></section>
      <el-skeleton :loading="loading" animated :rows="6">
        <template #default>
          <section v-if="filteredDocs.length" class="document-grid">
            <article v-for="doc in filteredDocs" :key="doc.id" class="document-card" @click="openPreview(doc)">
              <div class="file-top"><span class="file-type"
                                          :class="fileKind(doc).toLowerCase()">{{ fileKind(doc) }}</span>
                <el-dropdown trigger="click" @click.stop>
                  <el-button text circle @click.stop>···</el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item :icon="View" @click="openPreview(doc)">查看文档</el-dropdown-item>
                      <el-dropdown-item :icon="Download" @click="download(doc)">下载文件</el-dropdown-item>
                      <el-dropdown-item divided :icon="Delete" @click="deleteFile(doc)">删除文档</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
              <div class="file-icon">
                <el-icon>
                  <Document/>
                </el-icon>
              </div>
              <h3 :title="doc.originalName">{{ doc.originalName }}</h3>
              <p>{{ formatSize(doc.size) }} · {{ formatDate(doc.createdAt) }}</p></article>
          </section>
          <el-empty v-else :image-size="150" description="还没有找到文档">
            <el-upload :show-file-list="false" :http-request="uploadFile">
              <el-button type="primary">上传第一份文档</el-button>
            </el-upload>
          </el-empty>
        </template>
      </el-skeleton>
    </el-main>
  </el-container>
  <el-dialog v-model="previewVisible" class="preview-dialog" width="94%" top="3vh" destroy-on-close>
    <template #header>
      <div class="preview-title">
        <span>{{ selected?.originalName }}</span><small>{{ selected && formatSize(selected.size) }} ·
        {{ selected && fileKind(selected) }} 预览</small></div>
    </template>
    <div v-if="selected" v-loading="previewLoading" class="preview-body">
      <MarkdownViewer v-if="viewerType(selected)==='markdown'&&!previewLoading" :content="markdownContent"/>
      <PdfViewer v-else-if="['pdf','office'].includes(viewerType(selected))&&!previewLoading" :source="pdfSource"/>
      <iframe v-else-if="viewerType(selected)==='html'&&!previewLoading" class="html-preview" :src="htmlSource"
              title="HTML 文档预览" sandbox="allow-scripts allow-forms allow-popups"></iframe>
      <div v-else-if="!previewLoading" class="unsupported">
        <el-icon :size="48">
          <Document/>
        </el-icon>
        <h3>暂不支持在线预览此格式</h3>
        <p>目前支持 Markdown、HTML、PDF 以及 Office 文档在线预览。</p>
        <el-button type="primary" :icon="Download" @click="download(selected)">下载后查看</el-button>
      </div>
    </div>
    <template #footer>
      <el-button @click="previewVisible=false">关闭</el-button>
      <el-button type="primary" :icon="Download" @click="download(selected)">下载文件</el-button>
    </template>
  </el-dialog>
</template>
