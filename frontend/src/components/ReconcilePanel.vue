<script setup lang="ts">
import { reactive, ref } from 'vue'

const apiBase = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'
const props = defineProps<{
  module?: string
  action?: string
}>()

const form = reactive({
  splitGuids: '',
  reconcileDate: ''
})
const loading = ref(false)
const message = ref('')

const submit = async () => {
  const guids = form.splitGuids
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  if (!guids.length) {
    message.value = '请填写至少一个分录 GUID'
    return
  }
  loading.value = true
  message.value = ''
  try {
    const res = await fetch(`${apiBase}/api/reconcile/splits`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        splitGuids: guids,
        reconcileDate: form.reconcileDate || null
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '对账失败')
    message.value = '对账成功'
  } catch (e) {
    message.value = e instanceof Error ? e.message : '对账失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="panel">
    <h3>对账</h3>
    <p class="muted">模块：{{ props.module || '通用' }} | 操作：{{ props.action || '对账' }}</p>
    <div class="form">
      <label class="field">
        <span>分录 GUID 列表（逗号分隔）</span>
        <textarea v-model.trim="form.splitGuids" rows="3" placeholder="split-guid-1, split-guid-2"></textarea>
      </label>
      <label class="field">
        <span>对账日期（可选）</span>
        <input v-model="form.reconcileDate" type="date" />
      </label>
      <button type="button" @click="submit" :disabled="loading">提交对账</button>
    </div>
    <p v-if="message" class="message">{{ message }}</p>
  </div>
</template>

<style scoped>
.panel {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
  background: #f8fafc;
}
.muted {
  color: #64748b;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 8px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
textarea,
input {
  padding: 8px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
}
button {
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #16a34a;
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: #fff;
  cursor: pointer;
  align-self: flex-start;
}
.message {
  margin-top: 8px;
  color: #0ea5e9;
  font-weight: 600;
}
</style>
