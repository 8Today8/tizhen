import { onScopeDispose, ref } from 'vue'

import type { BattleRoom } from '@/api/battle'

export type BattleSocketState = 'connecting' | 'online' | 'offline' | 'closed'

export interface BattleSocketMessage {
  type: string
  payload: Record<string, unknown>
  serverTime: string
}

export interface BattleSocketHandlers {
  onRoom: (room: BattleRoom) => void
  onEvent?: (message: BattleSocketMessage) => void
  onClose?: (code: number, reason: string) => void
  onError?: (message: string) => void
}

/**
 * 对战 WebSocket 客户端。只发送心跳，所有写操作仍通过 REST。
 * 断线后不自动重连，由页面提供明确的重连入口。
 */
export function useBattleSocket(roomId: string, handlers: BattleSocketHandlers) {
  const state = ref<BattleSocketState>('connecting')
  let socket: WebSocket | null = null
  let manualClose = false

  function connect() {
    closeSocket()
    manualClose = false
    state.value = 'connecting'

    const ws = new WebSocket(buildUrl(roomId))
    socket = ws

    ws.onopen = () => {
      if (socket === ws) {
        state.value = 'online'
      }
    }

    ws.onmessage = (event) => {
      if (socket !== ws || typeof event.data !== 'string') {
        return
      }
      let message: BattleSocketMessage
      try {
        message = JSON.parse(event.data) as BattleSocketMessage
      } catch {
        return
      }

      if (message.type === 'battle.ping') {
        if (ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify({ type: 'battle.pong' }))
        }
        return
      }

      if (message.type === 'battle.snapshot') {
        const room = message.payload?.room as BattleRoom | undefined
        if (room) {
          handlers.onRoom(room)
        }
        return
      }

      if (message.type === 'battle.error') {
        const errorMessage = message.payload?.message
        handlers.onError?.(typeof errorMessage === 'string' ? errorMessage : '对战消息处理失败')
        return
      }

      handlers.onEvent?.(message)
    }

    ws.onerror = () => {
      if (socket === ws && state.value === 'online') {
        state.value = 'offline'
      }
    }

    ws.onclose = (event) => {
      if (socket !== ws) {
        return
      }
      socket = null
      if (manualClose) {
        state.value = 'closed'
        return
      }
      state.value = 'offline'
      handlers.onClose?.(event.code, event.reason)
    }
  }

  function reconnect() {
    connect()
  }

  function closeSocket() {
    const current = socket
    socket = null
    if (current && current.readyState <= WebSocket.OPEN) {
      current.close()
    }
  }

  function disconnect() {
    manualClose = true
    closeSocket()
    state.value = 'closed'
  }

  connect()
  onScopeDispose(disconnect)

  return {
    state,
    reconnect,
    disconnect
  }
}

function buildUrl(roomId: string): string {
  // VITE_WS_BASE_URL 已包含 /ws 前缀，这里只追加具体房间路径。
  const socketPath = `battle/rooms/${encodeURIComponent(roomId)}`
  const configured = (import.meta.env.VITE_WS_BASE_URL as string | undefined)?.trim()
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'

  if (configured) {
    const base = configured.replace(/\/+$/, '')
    if (/^wss?:\/\//i.test(base)) {
      return `${base}/${socketPath}`
    }
    if (base.startsWith('/')) {
      return `${protocol}//${window.location.host}${base}/${socketPath}`
    }
    return `${protocol}//${base}/${socketPath}`
  }

  return `${protocol}//${window.location.host}/ws/${socketPath}`
}
