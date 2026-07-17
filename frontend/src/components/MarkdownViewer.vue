<script setup>
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
const props = defineProps({ content: { type: String, default: '' } })
const markdown = new MarkdownIt({ html: false, linkify: true, typographer: true, highlight(code, language) { if (language && hljs.getLanguage(language)) return `<pre class="hljs"><code>${hljs.highlight(code, { language, ignoreIllegals: true }).value}</code></pre>`; return `<pre class="hljs"><code>${markdown.utils.escapeHtml(code)}</code></pre>` } })
const rendered = computed(() => markdown.render(props.content))
</script>
<template><article class="markdown-viewer" v-html="rendered"></article></template>
<style scoped>
.markdown-viewer{max-width:980px;margin:0 auto;padding:34px 48px;color:#26344d;font-size:15px;line-height:1.8}.markdown-viewer :deep(h1),.markdown-viewer :deep(h2),.markdown-viewer :deep(h3){margin:1.4em 0 .55em;line-height:1.3;color:#172641}.markdown-viewer :deep(h1){padding-bottom:.35em;border-bottom:1px solid #e5eaf2;font-size:2em}.markdown-viewer :deep(h2){font-size:1.5em}.markdown-viewer :deep(p){margin:0 0 1em}.markdown-viewer :deep(code){padding:.15em .4em;border-radius:4px;background:#f0f3f8;font-family:Consolas,monospace;font-size:.88em}.markdown-viewer :deep(pre){overflow:auto;padding:18px;border-radius:9px;background:#24292f}.markdown-viewer :deep(pre code){padding:0;background:transparent}.markdown-viewer :deep(blockquote){margin:1em 0;padding:0 1em;border-left:4px solid #9db9ff;color:#64718a}.markdown-viewer :deep(table){display:block;overflow:auto;border-collapse:collapse}.markdown-viewer :deep(th),.markdown-viewer :deep(td){padding:7px 12px;border:1px solid #dce3ed}.markdown-viewer :deep(img){max-width:100%}
</style>
