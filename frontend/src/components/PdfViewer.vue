<script setup>
import {ref} from 'vue'
import VuePdfEmbed from 'vue-pdf-embed'
import {ArrowLeft, ArrowRight} from '@element-plus/icons-vue'

defineProps({source: {type: [Uint8Array, ArrayBuffer], default: null}})
const currentPage = ref(1), pageCount = ref(1)
const loaded = pdf => {
  pageCount.value = pdf?.numPages || 1;
  currentPage.value = 1
}
</script>
<template>
  <section class="pdf-viewer">
    <div class="pdf-toolbar"><span>PDF 在线预览</span>
      <div>
        <el-button circle :icon="ArrowLeft" :disabled="currentPage <= 1" @click="currentPage--"/>
        <span class="page-number">第 {{ currentPage }} / {{ pageCount }} 页</span>
        <el-button circle :icon="ArrowRight" :disabled="currentPage >= pageCount" @click="currentPage++"/>
      </div>
    </div>
    <div class="pdf-page">
      <VuePdfEmbed v-if="source" :source="source" :page="currentPage" @loaded="loaded"/>
    </div>
  </section>
</template>
<style scoped>
.pdf-viewer {
  height: 100%;
  overflow: auto;
  background: #eef1f6
}

.pdf-toolbar {
  position: sticky;
  z-index: 2;
  top: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 18px;
  background: #fff;
  border-bottom: 1px solid #e5e9f0;
  color: #526178;
  font-size: 13px
}

.pdf-toolbar > div {
  display: flex;
  align-items: center;
  gap: 9px
}

.page-number {
  min-width: 92px;
  text-align: center
}

.pdf-page {
  display: grid;
  place-items: start center;
  min-height: 100%;
  padding: 24px
}

.pdf-page :deep(canvas) {
  max-width: 100%;
  height: auto !important;
  box-shadow: 0 4px 16px #26395825
}
</style>
