<script setup lang="ts">
import { computed, ref } from 'vue'
import type { UploadFile } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Camera, Loading, UserFilled } from '@element-plus/icons-vue'

import { resolveApiErrorMessage } from '@/utils/apiError'

const MAX_AVATAR_BYTES = 2 * 1024 * 1024
const ACCEPTED_TYPES = ['image/jpeg', 'image/png']

const props = withDefaults(
  defineProps<{
    modelValue: string | null
    uploader: (file: File) => Promise<string>
    size?: number
    disabled?: boolean
  }>(),
  {
    size: 96,
    disabled: false
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: string | null]
  'update:uploading': [value: boolean]
}>()

const uploading = ref(false)
const boxStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`
}))

function handleChange(uploadFile: UploadFile) {
  const raw = uploadFile.raw
  if (!raw) {
    return
  }
  const looksLikeImage =
    ACCEPTED_TYPES.includes(raw.type) || /\.(jpe?g|png)$/i.test(raw.name)
  if (!looksLikeImage) {
    ElMessage.error('仅支持 JPG/PNG 格式的图片')
    return
  }
  if (raw.size > MAX_AVATAR_BYTES) {
    ElMessage.error('头像图片不能超过 2MB')
    return
  }
  void upload(raw)
}

async function upload(file: File) {
  uploading.value = true
  emit('update:uploading', true)
  try {
    const url = await props.uploader(file)
    emit('update:modelValue', url)
    ElMessage.success('头像已上传')
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '头像上传失败'))
  } finally {
    uploading.value = false
    emit('update:uploading', false)
  }
}

function clearAvatar() {
  emit('update:modelValue', null)
}
</script>

<template>
  <div class="avatar-uploader">
    <el-upload
      :auto-upload="false"
      :show-file-list="false"
      :disabled="disabled || uploading"
      accept="image/jpeg,image/png"
      :on-change="handleChange"
    >
      <div v-loading="uploading" class="avatar-uploader__box" :style="boxStyle">
        <el-avatar :size="size" :src="modelValue || undefined">
          <el-icon><UserFilled /></el-icon>
        </el-avatar>
        <div class="avatar-uploader__mask">
          <el-icon :class="{ 'is-loading': uploading }">
            <Loading v-if="uploading" />
            <Camera v-else />
          </el-icon>
          <span>{{ uploading ? '上传中' : '更换头像' }}</span>
        </div>
      </div>
    </el-upload>
    <el-button
      v-if="modelValue && !disabled && !uploading"
      class="avatar-uploader__remove"
      link
      type="danger"
      size="small"
      @click="clearAvatar"
    >
      移除头像
    </el-button>
  </div>
</template>

<style scoped>
.avatar-uploader {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.avatar-uploader__box {
  position: relative;
  overflow: hidden;
  border: 1px dashed var(--tz-color-border-strong, var(--tz-color-border));
  border-radius: 50%;
  cursor: pointer;
  transition: border-color 0.2s ease;
}

.avatar-uploader__box:hover {
  border-color: var(--tz-color-primary);
}

.avatar-uploader__box :deep(.el-avatar) {
  width: 100%;
  height: 100%;
  font-size: 28px;
}

.avatar-uploader__mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  background: rgb(0 0 0 / 45%);
  color: #fff;
  font-size: 11px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.avatar-uploader__box:hover .avatar-uploader__mask {
  opacity: 1;
}

.avatar-uploader__remove {
  height: auto;
  padding: 0;
  font-size: 12px;
}
</style>
