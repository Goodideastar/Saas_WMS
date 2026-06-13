<template>
  <button class="ai-trigger" @click="$emit('toggle')" :title="open ? '收起AI助手' : '展开AI助手'">
    <svg class="ai-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="1.5" class="icon-ring" />
      <path d="M8 12h8M12 8v8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" class="icon-cross" />
      <circle cx="17" cy="7" r="1.5" fill="currentColor" class="icon-dot dot-1" />
      <circle cx="19" cy="12" r="1" fill="currentColor" class="icon-dot dot-2" />
      <circle cx="17" cy="17" r="1.2" fill="currentColor" class="icon-dot dot-3" />
    </svg>
    <span class="ai-label">AI</span>
    <span class="ai-glow"></span>
  </button>
</template>

<script setup>
defineProps({ open: Boolean })
defineEmits(['toggle'])
</script>

<style scoped>
.ai-trigger {
  position: relative;
  height: 32px;
  padding: 0 10px;
  display: flex;
  align-items: center;
  gap: 5px;
  border: 1px solid rgba(99, 102, 241, 0.3);
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.1), rgba(139, 92, 246, 0.1));
  color: #818cf8;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s ease;
  font-family: inherit;
}

.ai-trigger:hover {
  border-color: rgba(99, 102, 241, 0.6);
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.2), rgba(139, 92, 246, 0.2));
  box-shadow: 0 0 12px rgba(99, 102, 241, 0.25);
}

.ai-trigger:hover .icon-dot {
  animation: dotPulse 0.8s ease-in-out infinite;
}

.ai-trigger:hover .icon-ring {
  stroke-dasharray: 62;
  stroke-dashoffset: 62;
  animation: ringDraw 0.6s ease-out forwards;
}

.ai-glow {
  position: absolute;
  inset: -4px;
  border-radius: 20px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6, #6366f1);
  opacity: 0;
  z-index: -1;
  filter: blur(8px);
  transition: opacity 0.5s ease;
}

.ai-trigger:hover .ai-glow {
  opacity: 0.15;
  animation: glowPulse 2s ease-in-out infinite;
}

.ai-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.icon-dot {
  opacity: 0.6;
}

.dots-1 { animation-delay: 0s; }
.dots-2 { animation-delay: 0.15s; }
.dots-3 { animation-delay: 0.3s; }

.ai-label {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.03em;
  line-height: 1;
}

@keyframes dotPulse {
  0%, 100% { opacity: 0.4; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.3); }
}

@keyframes ringDraw {
  to { stroke-dashoffset: 0; }
}

@keyframes glowPulse {
  0%, 100% { opacity: 0.1; }
  50% { opacity: 0.25; }
}
</style>
