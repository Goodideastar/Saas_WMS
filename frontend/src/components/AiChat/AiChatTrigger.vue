<template>
  <button class="ai-trigger" @click="$emit('toggle')" :title="open ? '收起AI助手' : '展开AI助手'">
    AI
  </button>
</template>

<script setup>
defineProps({ open: Boolean })
defineEmits(['toggle'])
</script>

<style scoped>
.ai-trigger {
  position: fixed; right: 20px; bottom: 80px; width: 50px; height: 50px;
  border-radius: 50%; border: none; background: #409eff; color: #fff;
  font-size: 14px; font-weight: bold; cursor: pointer; box-shadow: 0 2px 10px rgba(0,0,0,0.2);
  z-index: 999; display: flex; align-items: center; justify-content: center;
}
</style>
