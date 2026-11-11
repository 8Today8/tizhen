<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Compartment, EditorState } from '@codemirror/state'
import { EditorView, keymap } from '@codemirror/view'
import { indentWithTab } from '@codemirror/commands'
import { cpp } from '@codemirror/lang-cpp'
import { python } from '@codemirror/lang-python'
import { basicSetup } from 'codemirror'

const props = withDefaults(
  defineProps<{
    modelValue: string
    language: string
    readonly?: boolean
  }>(),
  { readonly: false }
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const host = ref<HTMLDivElement | null>(null)
const languageCompartment = new Compartment()
const readOnlyCompartment = new Compartment()

const editorTheme = EditorView.theme(
  {
    '&': {
      height: '100%',
      color: '#e9e7f0',
      backgroundColor: '#1d1e26'
    },
    '.cm-scroller': {
      fontFamily: 'var(--tz-font-mono)',
      fontSize: '13.5px',
      lineHeight: '1.65'
    },
    '.cm-content': {
      padding: '14px 0'
    },
    '.cm-gutters': {
      color: '#777b8c',
      backgroundColor: '#191a21',
      borderRight: '1px solid #2b2d38'
    },
    '.cm-activeLine': {
      backgroundColor: 'rgba(255, 255, 255, 0.035)'
    },
    '.cm-activeLineGutter': {
      backgroundColor: 'rgba(255, 255, 255, 0.045)'
    },
    '.cm-cursor, .cm-dropCursor': {
      borderLeftColor: '#f0c987'
    },
    '.cm-selectionBackground, &.cm-focused .cm-selectionBackground, ::selection': {
      backgroundColor: 'rgba(115, 87, 232, 0.38)'
    },
    '.cm-tooltip': {
      color: '#e9e7f0',
      backgroundColor: '#252733',
      border: '1px solid #353846'
    },
    '.cm-panels': {
      backgroundColor: '#1b1c24',
      color: '#c9c7d2'
    }
  },
  { dark: true }
)

let view: EditorView | null = null

function languageExtension(language: string) {
  return language === 'python310' ? python() : cpp()
}

function readOnlyExtension(readonly: boolean) {
  return [EditorState.readOnly.of(readonly), EditorView.editable.of(!readonly)]
}

onMounted(() => {
  if (!host.value) {
    return
  }
  view = new EditorView({
    parent: host.value,
    state: EditorState.create({
      doc: props.modelValue,
      extensions: [
        basicSetup,
        keymap.of([indentWithTab]),
        EditorState.tabSize.of(4),
        editorTheme,
        languageCompartment.of(languageExtension(props.language)),
        readOnlyCompartment.of(readOnlyExtension(props.readonly)),
        EditorView.updateListener.of((update) => {
          if (update.docChanged) {
            emit('update:modelValue', update.state.doc.toString())
          }
        })
      ]
    })
  })
})

watch(
  () => props.modelValue,
  (value) => {
    if (!view) {
      return
    }
    const current = view.state.doc.toString()
    if (current === value) {
      return
    }
    view.dispatch({
      changes: { from: 0, to: current.length, insert: value }
    })
  }
)

watch(
  () => props.language,
  (language) => {
    view?.dispatch({
      effects: languageCompartment.reconfigure(languageExtension(language))
    })
  }
)

watch(
  () => props.readonly,
  (readonly) => {
    view?.dispatch({
      effects: readOnlyCompartment.reconfigure(readOnlyExtension(readonly))
    })
  }
)

onBeforeUnmount(() => {
  view?.destroy()
  view = null
})
</script>

<template>
  <div ref="host" class="code-editor" aria-label="代码编辑器"></div>
</template>

<style scoped>
.code-editor {
  overflow: hidden;
  height: clamp(360px, 52vh, 520px);
  border: 1px solid var(--tz-color-border-strong);
  border-radius: var(--tz-radius-md);
  background: #1d1e26;
}

.code-editor :deep(.cm-editor) {
  height: 100%;
}

.code-editor :deep(.cm-editor.cm-focused) {
  outline: none;
}
</style>
