import DOMPurify from 'dompurify'
import MarkdownIt from 'markdown-it'

/**
 * 学习阶段内容同时兼容 Markdown 与运营录入的富文本 HTML，
 * 渲染结果统一经 DOMPurify 清洗，并强制新窗口打开外部链接。
 */
const markdown = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true,
  typographer: false
})

markdown.renderer.rules.link_open = (tokens, index, options, _environment, self) => {
  tokens[index].attrSet('target', '_blank')
  tokens[index].attrSet('rel', 'noopener noreferrer')
  return self.renderToken(tokens, index, options)
}

export function renderMarkdown(source: string | null | undefined): string {
  if (!source?.trim()) {
    return ''
  }
  return DOMPurify.sanitize(markdown.render(source), {
    USE_PROFILES: { html: true }
  })
}
