<script setup lang="ts">
import { reactive, ref } from 'vue'

const message = ref('')
const apiBase = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'
const props = defineProps<{ bookGuid: string }>()
const loading = ref(false)
const form = reactive({
  invoiceNo: '',
  customerGuid: '',
  description: ''
})
const entryForm = reactive({
  accountGuid: '',
  amount: 0,
  description: ''
})
const entries = ref<{ accountGuid: string; amount: number; description: string }[]>([])

const submit = async () => {
  if (!props.bookGuid) {
    message.value = '缺少账本'
    return
  }
  loading.value = true
  message.value = ''
  try {
    const res = await fetch(`${apiBase}/api/business/sales/invoices`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bookGuid: props.bookGuid,
        customerGuid: form.customerGuid,
        invoiceId: form.invoiceNo || null,
        notes: form.description || null
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '创建失败')
    message.value = '销售发票创建成功'
    if (entries.value.length) {
      await submitEntries(data.data)
    }
  } catch (e) {
    message.value = e instanceof Error ? e.message : '创建失败'
  } finally {
    loading.value = false
  }
}

const addEntry = () => {
  if (!entryForm.accountGuid || !entryForm.amount) {
    message.value = '请填写科目和金额'
    return
  }
  entries.value.push({
    accountGuid: entryForm.accountGuid,
    amount: entryForm.amount,
    description: entryForm.description
  })
  entryForm.accountGuid = ''
  entryForm.amount = 0
  entryForm.description = ''
}

const submitEntries = async (invoiceGuid: string) => {
  const res = await fetch(`${apiBase}/api/business/sales/invoices/${invoiceGuid}/entries`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      bookGuid: props.bookGuid,
      items: entries.value.map((it) => ({
        accountGuid: it.accountGuid,
        amountCent: Math.round(it.amount * 100),
        description: it.description || null
      }))
    })
  })
  const data = await res.json()
  if (!res.ok || !data.success) throw new Error(data.message || '行项创建失败')
}
</script>

<template>
  <div class="panel">
    <h3>新增销售发票</h3>
    <div class="form">
      <label class="field">
        <span>发票编号</span>
        <input v-model.trim="form.invoiceNo" type="text" placeholder="SI-001" />
      </label>
      <label class="field">
        <span>客户</span>
        <input v-model.trim="form.customerGuid" type="text" placeholder="客户 GUID" />
      </label>
      <label class="field">
        <span>备注</span>
        <input v-model.trim="form.description" type="text" placeholder="说明" />
      </label>
      <button type="button" @click="submit" :disabled="loading">保存</button>
    </div>
    <div class="entry">
      <h4>行项</h4>
      <div class="entry-form">
        <label class="field">
          <span>科目 GUID</span>
          <input v-model.trim="entryForm.accountGuid" type="text" placeholder="科目 GUID" />
        </label>
        <label class="field">
          <span>金额（元）</span>
          <input v-model.number="entryForm.amount" type="number" min="0" step="0.01" />
        </label>
        <label class="field">
          <span>描述</span>
          <input v-model.trim="entryForm.description" type="text" placeholder="描述" />
        </label>
        <button type="button" class="ghost" @click="addEntry">添加行</button>
      </div>
      <ul class="entry-list">
        <li v-for="(item, idx) in entries" :key="idx">
          {{ item.accountGuid }} - {{ item.amount }} 元 - {{ item.description || '无' }}
        </li>
      </ul>
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
.form {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px 12px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
input {
  padding: 10px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
}
button {
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #16a34a;
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: #fff;
  cursor: pointer;
  grid-column: span 2;
}
.message {
  margin-top: 8px;
  color: #0ea5e9;
  font-weight: 600;
}

.entry {
  margin-top: 12px;
}
.entry-form {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 8px 10px;
  align-items: end;
}
.entry-list {
  padding-left: 16px;
  color: #334155;
  font-size: 14px;
}
.ghost {
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #0f172a;
}
</style>
