<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'

import {
  updatePlayerProfile,
  uploadPlayerAvatar,
  type PlayerProfile
} from '@/api/account'
import { resolveApiErrorMessage } from '@/utils/apiError'

const MAX_AVATAR_BYTES = 2 * 1024 * 1024

const props = defineProps<{
  avatarUrl: string | null
  initial: string
}>()

const emit = defineEmits<{
  updated: [profile: PlayerProfile]
}>()

const inputRef = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const removing = ref(false)
const dragging = ref(false)
const localPreview = ref('')
const errorMessage = ref('')

const busy = computed(() => uploading.value || removing.value)
const preview = computed(() => localPreview.value || props.avatarUrl || undefined)

function openPicker() {
  if (!busy.value) {
    inputRef.value?.click()
  }
}

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (file) {
    void handleFile(file)
  }
}

function onDragOver(event: DragEvent) {
  event.preventDefault()
  if (!busy.value) {
    dragging.value = true
  }
}

function onDragLeave(event: DragEvent) {
  const related = event.relatedTarget as Node | null
  if (related && (event.currentTarget as HTMLElement).contains(related)) {
    return
  }
  dragging.value = false
}

function onDrop(event: DragEvent) {
  event.preventDefault()
  dragging.value = false
  if (busy.value) {
    return
  }
  const file = event.dataTransfer?.files?.[0]
  if (file) {
    void handleFile(file)
  }
}

function validate(file: File): string {
  const type = file.type.toLowerCase()
  const validType = type === 'image/jpeg' || type === 'image/png'
  const validName = /\.(jpe?g|png)$/i.test(file.name)
  if (!validType && !validName) {
    return '仅支持 JPG 或 PNG 格式的图片'
  }
  if (file.size > MAX_AVATAR_BYTES) {
    return '头像图片不能超过 2MB'
  }
  return ''
}

async function handleFile(file: File) {
  const message = validate(file)
  if (message) {
    errorMessage.value = message
    ElMessage.warning(message)
    return
  }

  errorMessage.value = ''
  const objectUrl = URL.createObjectURL(file)
  localPreview.value = objectUrl
  uploading.value = true
  try {
    const profile = await uploadPlayerAvatar(file)
    emit('updated', profile)
    ElMessage.success('头像已更新')
  } catch (error) {
    const failure = resolveApiErrorMessage(error, '头像上传失败，请稍后重试')
    errorMessage.value = failure
    ElMessage.error(failure)
  } finally {
    // 等父组件把新地址回填到 props 后再释放本地预览，避免短暂显示旧头像。
    await nextTick()
    URL.revokeObjectURL(objectUrl)
    localPreview.value = ''
    uploading.value = false
  }
}

async function removeAvatar() {
  if (busy.value) {
    return
  }
  try {
    await ElMessageBox.confirm('移除后将恢复为昵称首字母头像，确认移除？', '移除头像', {
      confirmButtonText: '移除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  errorMessage.value = ''
  removing.value = true
  try {
    const profile = await updatePlayerProfile({ avatarUrl: null })
    emit('updated', profile)
    ElMessage.success('头像已移除')
  } catch (error) {
    const failure = resolveApiErrorMessage(error, '移除头像失败，请稍后重试')
    errorMessage.value = failure
    ElMessage.error(failure)
  } finally {
    removing.value = false
  }
}
</script>

<template>
  <div
    class="profile-avatar-uploader"
    :class="{
      'profile-avatar-uploader--dragging': dragging,
      'profile-avatar-uploader--busy': busy
    }"
    role="button"
    tabindex="0"
    aria-label="上传头像"
    @click="openPicker"
    @keydown.enter.prevent="openPicker"
    @keydown.space.prevent="openPicker"
    @dragover="onDragOver"
    @dragleave="onDragLeave"
    @drop="onDrop"
  >
    <div class="profile-avatar-uploader__preview">
      <el-avatar :size="72" :src="preview" class="profile-avatar-uploader__avatar">
        {{ initial }}
      </el-avatar>
      <span v-if="uploading" class="profile-avatar-uploader__mask">
        <el-icon class="is-loading"><Loading /></el-icon>
      </span>
    </div>

    <div class="profile-avatar-uploader__body">
      <strong>头像</strong>
      <p class="profile-avatar-uploader__hint">
        {{ dragging ? '松开鼠标即可上传头像' : '点击选择图片，或把图片拖到这里' }}
      </p>
      <p class="profile-avatar-uploader__meta">支持 JPG/PNG，大小不超过 2MB。</p>
      <div class="profile-avatar-uploader__actions">
        <el-button
          size="small"
          type="primary"
          plain
          :loading="uploading"
          @click.stop="openPicker"
        >
          选择图片
        </el-button>
        <el-button
          v-if="avatarUrl"
          size="small"
          text
          type="danger"
          :loading="removing"
          :disabled="uploading"
          @click.stop="removeAvatar"
        >
          移除头像
        </el-button>
      </div>
      <p v-if="errorMessage" class="profile-avatar-uploader__error">{{ errorMessage }}</p>
    </div>

    <input
      ref="inputRef"
      class="profile-avatar-uploader__input"
      type="file"
      accept="image/png,image/jpeg"
      tabindex="-1"
      aria-hidden="true"
      @click.stop
      @change="onFileChange"
    />
  </div>
</template>
