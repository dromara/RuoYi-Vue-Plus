<template>
  <div class="editor-shell">
    <EditorToolbar :editor="editorRef" :default-config="toolbarConfig" mode="default" class="editor-toolbar" />
    <div class="editor-body" :style="styles">
      <WangEditor
        v-model="content"
        :default-config="editorConfig"
        mode="default"
        class="editor-content"
        @onCreated="handleCreated"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import '@wangeditor-next/editor/dist/css/style.css';
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor-next/editor';
import { Editor as WangEditor, Toolbar as EditorToolbar } from '@wangeditor-next/editor-for-vue';
import { listByIds } from '@/api/system/oss';
import modal from '@/plugins/modal';
import { propTypes } from '@/utils/propTypes';
import { globalHeaders } from '@/utils/request';

const OSS_MARKER_RE = /oss:\/\/([\w-]+)/g;
const props = defineProps({
  /* 编辑器的内容 */
  modelValue: propTypes.string,
  /* 高度 */
  height: propTypes.number.def(400),
  /* 最小高度 */
  minHeight: propTypes.number.def(400),
  /* 只读 */
  readOnly: propTypes.bool.def(false),
  /* 图片上传文件大小限制(MB) */
  fileSize: propTypes.number.def(5),
  /* 视频上传文件大小限制(MB) */
  videoSize: propTypes.number.def(100),
  /* 类型（base64格式、url格式） url模式下存储 oss://ossId 标记，展示时通过后端动态解析为可用URL */
  type: propTypes.string.def('url')
});

const emit = defineEmits(['update:modelValue']);

const editorRef = shallowRef<IDomEditor>();
const content = ref('');

const baseUrl = import.meta.env.VITE_APP_BASE_API;
const uploadOssUrl = baseUrl + '/resource/oss/upload';

// URL → ossId 映射，在上传和解析阶段填充
const ossUrlToId = new Map<string, string>();
// 防止 modelValue ↔ content 双向 watch 循环
const isResolvingContent = ref(false);
// 记录最后一次 encode 后 emit 的值，用于跳过自身触发的 modelValue 变更
const lastEncodedValue = ref('');

const styles = computed(() => {
  const style: Record<string, string> = {};
  if (props.minHeight) {
    style.minHeight = `${props.minHeight}px`;
  }
  if (props.height) {
    style.height = `${props.height}px`;
  }
  return style;
});

const toolbarConfig = computed<Partial<IToolbarConfig>>(() => {
  const excludeKeys = ['fullScreen'];

  if (!props.type) {
    excludeKeys.push('uploadImage');
    excludeKeys.push('uploadVideo');
  }

  return {
    modalAppendToBody: false,
    excludeKeys
  };
});

/* ==================== OSS 上传 & 内容转换 ==================== */

const uploadToOss = async (file: File): Promise<{ url: string; fileName: string; ossId: string }> => {
  const formData = new FormData();
  formData.append('file', file);

  const res = await fetch(uploadOssUrl, {
    method: 'POST',
    headers: globalHeaders(),
    body: formData
  });
  const data = await res.json();

  if (data.code === 200) {
    ossUrlToId.set(data.data.url, String(data.data.ossId));
    return data.data;
  }
  throw new Error(data.msg || '上传失败');
};

/**
 * 编码：将编辑器内容中的 OSS 真实 URL 替换为 oss://ossId 标记（用于存储）
 */
const encodeOssContent = (html: string): string => {
  if (!html) return html;
  let result = html;
  for (const [url, ossId] of ossUrlToId) {
    result = result.replaceAll(url, `oss://${ossId}`);
  }
  return result;
};

/**
 * 解码：将存储的 oss://ossId 标记批量替换为真实可用 URL（用于编辑/展示）
 */
const decodeOssContent = async (html: string): Promise<string> => {
  if (!html) return html;

  const matches = [...html.matchAll(OSS_MARKER_RE)];
  if (matches.length === 0) return html;

  const ossIds = [...new Set(matches.map(m => m[1]))];

  try {
    const res = await listByIds(ossIds.join(','));
    let result = html;
    for (const oss of res.data) {
      const id = String(oss.ossId);
      ossUrlToId.set(oss.url, id);
      result = result.replaceAll(`oss://${id}`, oss.url);
    }
    return result;
  } catch {
    return html;
  }
};

/* ==================== 文件校验 ==================== */

const validateImageFile = (file: File) => {
  const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/svg', 'image/svg+xml'];
  if (!allowedTypes.includes(file.type)) {
    modal.msgError('图片格式错误!');
    return false;
  }

  if (props.fileSize) {
    const isLt = file.size / 1024 / 1024 < props.fileSize;
    if (!isLt) {
      modal.msgError(`上传图片大小不能超过 ${props.fileSize} MB!`);
      return false;
    }
  }

  return true;
};

const validateVideoFile = (file: File) => {
  const allowedTypes = ['video/mp4', 'video/webm', 'video/ogg'];
  if (!allowedTypes.includes(file.type)) {
    modal.msgError('视频格式错误, 请上传 mp4/webm/ogg 格式!');
    return false;
  }

  if (props.videoSize) {
    const isLt = file.size / 1024 / 1024 < props.videoSize;
    if (!isLt) {
      modal.msgError(`上传视频大小不能超过 ${props.videoSize} MB!`);
      return false;
    }
  }

  return true;
};

/* ==================== wangeditor 菜单配置 ==================== */

const getUploadImageMenuConfig = () => {
  if (props.type === 'base64') {
    return {
      allowedFileTypes: ['image/jpeg', 'image/jpg', 'image/png', 'image/svg+xml'],
      customUpload(file: File, insertFn: (url: string, alt?: string, href?: string) => void) {
        if (!validateImageFile(file)) return;

        modal.loading('正在上传图片，请稍候...');
        const reader = new FileReader();

        reader.onload = () => {
          insertFn(reader.result as string, file.name);
          modal.closeLoading();
        };

        reader.onerror = () => {
          modal.msgError('图片插入失败');
          modal.closeLoading();
        };

        reader.readAsDataURL(file);
      }
    };
  }

  return {
    allowedFileTypes: ['image/jpeg', 'image/jpg', 'image/png', 'image/svg+xml'],
    async customUpload(file: File, insertFn: (url: string, alt?: string, href?: string) => void) {
      if (!validateImageFile(file)) return;

      modal.loading('正在上传图片，请稍候...');
      try {
        const result = await uploadToOss(file);
        insertFn(result.url, file.name, result.url);
      } catch {
        modal.msgError('图片上传失败');
      } finally {
        modal.closeLoading();
      }
    }
  };
};

const getUploadVideoMenuConfig = () => ({
  allowedFileTypes: ['video/mp4', 'video/webm', 'video/ogg'],
  async customUpload(file: File, insertFn: (url: string, poster?: string) => void) {
    if (!validateVideoFile(file)) return;

    modal.loading('正在上传视频，请稍候...');
    try {
      const result = await uploadToOss(file);
      insertFn(result.url);
    } catch {
      modal.msgError('视频上传失败');
    } finally {
      modal.closeLoading();
    }
  }
});

const editorConfig = computed<Partial<IEditorConfig>>(() => ({
  placeholder: '请输入内容',
  autoFocus: false,
  MENU_CONF: {
    uploadImage: getUploadImageMenuConfig() as any,
    uploadVideo: getUploadVideoMenuConfig() as any
  }
}));

/* ==================== 双向数据绑定 ==================== */

const syncReadOnly = () => {
  const editor = editorRef.value;
  if (!editor) {
    return;
  }

  if (props.readOnly) {
    editor.disable();
  } else {
    editor.enable();
  }
};

watch(
  () => props.modelValue,
  async value => {
    const nextValue = value || '';

    // 跳过由自身 emit 引起的回传
    if (nextValue === lastEncodedValue.value) return;

    isResolvingContent.value = true;
    const resolved = await decodeOssContent(nextValue);
    if (resolved !== content.value) {
      content.value = resolved;
    }
    nextTick(() => {
      isResolvingContent.value = false;
    });
  },
  { immediate: true }
);

watch(content, value => {
  if (isResolvingContent.value) return;

  const encoded = encodeOssContent(value);
  lastEncodedValue.value = encoded;
  if (encoded !== props.modelValue) {
    emit('update:modelValue', encoded);
  }
});

watch(
  () => props.readOnly,
  () => {
    syncReadOnly();
  }
);

const handleCreated = (editor: IDomEditor) => {
  editorRef.value = editor;
  syncReadOnly();
};

onBeforeUnmount(() => {
  editorRef.value?.destroy();
});
</script>

<style scoped>
.editor-shell {
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  overflow: hidden;
  background: var(--el-bg-color);
}

.editor-toolbar {
  border-bottom: 1px solid var(--el-border-color);
}

.editor-body {
  display: flex;
  flex-direction: column;
}

.editor-content {
  height: 100%;
}

.editor-shell :deep(.w-e-toolbar) {
  border: 0 !important;
  background-color: transparent;
}

.editor-shell :deep(.w-e-text-container) {
  border: 0 !important;
  background-color: transparent;
}

.editor-shell :deep(.w-e-text-container [data-slate-editor]) {
  min-height: inherit;
}
</style>
