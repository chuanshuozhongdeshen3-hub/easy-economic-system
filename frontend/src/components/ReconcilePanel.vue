<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'

const apiBase = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'
const props = defineProps<{
  module?: string
  action?: string
  bookGuid?: string
}>()

const form = reactive({
  bankGuid: '',
  bizGuid: '',
  reconcileDate: ''
})
const loading = ref(false)
const message = ref('')
const bankSplits = ref<{ guid: string; memo: string; amount: number }[]>([])
const bizSplits = ref<{ guid: string; memo: string; amount: number }[]>([])

const formatOption = (item: { guid: string; memo: string; amount: number }) => {
  const amt = (item.amount / 100).toFixed(2)
  return `${amt} | ${item.memo || item.guid}`
}

const submit = async () => {
  if (!form.bankGuid || !form.bizGuid) {
    message.value = '请先选择银行流水与业务分录'
    return
  }
  loading.value = true
  message.value = ''
  try {
    const res = await fetch(`${apiBase}/api/bank/reconcile`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bankSplitGuids: [form.bankGuid],
        bizSplitGuids: [form.bizGuid],
        reconcileDate: form.reconcileDate || null
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '对账失败')
    message.value = '对账成功'
    form.bankGuid = ''
    form.bizGuid = ''
    await loadLists()
  } catch (e) {
    message.value = e instanceof Error ? e.message : '对账失败'
  } finally {
    loading.value = false
  }
}

const loadLists = async () => {
  if (!props.bookGuid) return
  try {
    const res = await fetch(`${apiBase}/api/reconcile/splits?bookGuid=${props.bookGuid}`)
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error()
    const bank = (data.data?.bank || []).map((s: any) => ({
      guid: s.guid,
      memo: s.memo,
      amount: s.amount
    }))
    const biz = (data.data?.biz || []).map((s: any) => ({
      guid: s.guid,
      memo: s.memo,
      amount: s.amount
    }))
    bankSplits.value = bank
    bizSplits.value = biz
  } catch {
    bankSplits.value = []
    bizSplits.value = []
  }
}

onMounted(loadLists)
watch(
  () => props.bookGuid,
  (v) => v && loadLists()
)
</script>

<template>
  <div class="panel">
    <h3>对账</h3>
    <p class="muted">模块：{{ props.module || '通用' }} | 操作：{{ props.action || '对账' }}</p>
    <div class="form">
      <label class="field">
        <span>银行流水分录</span>
        <select v-model="form.bankGuid">
          <option value="" disabled>请选择</option>
          <option v-for="s in bankSplits" :key="s.guid" :value="s.guid">{{ formatOption(s) }}</option>
        </select>
      </label>
      <label class="field">
        <span>业务分录</span>
        <select v-model="form.bizGuid">
          <option value="" disabled>请选择</option>
          <option v-for="s in bizSplits" :key="s.guid" :value="s.guid">{{ formatOption(s) }}</option>
        </select>
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
input,
select {
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
