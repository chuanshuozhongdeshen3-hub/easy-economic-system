<script setup lang="ts">
import AuthPanel from './components/AuthPanel.vue'
</script>

<template>
  <main class="page">
    <AuthPanel />
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: radial-gradient(circle at 20% 20%, #e0f2fe, transparent 30%),
    radial-gradient(circle at 80% 0%, #dcfce7, transparent 32%),
    linear-gradient(145deg, #eef2ff, #fff);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
}
</style>
