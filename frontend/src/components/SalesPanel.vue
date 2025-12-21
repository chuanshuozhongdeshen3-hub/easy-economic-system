<script setup lang="ts">
import { reactive, ref } from 'vue'

const apiBase = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'
const props = defineProps<{ bookGuid: string }>()

const message = ref('')
const loading = ref(false)

const invoiceForm = reactive({
  invoiceNo: '',
  amount: 0,
  description: ''
})

const receiptForm = reactive({
  receiptNo: '',
  amount: 0,
  description: '',
  cashAccountName: ''
})

const postInvoice = async () => {
  if (!props.bookGuid) return
  loading.value = true
  message.value = ''
  try {
    const res = await fetch(`${apiBase}/api/sales/invoice/post`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bookGuid: props.bookGuid,
        invoiceNo: invoiceForm.invoiceNo || null,
        amountCent: Math.round(Number(invoiceForm.amount) * 100),
        description: invoiceForm.description || null
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '发票过账失败')
    message.value = '销售发票过账成功'
  } catch (e) {
    message.value = e instanceof Error ? e.message : '发票过账失败'
  } finally {
    loading.value = false
  }
}

const postReceipt = async () => {
  if (!props.bookGuid) return
  loading.value = true
  message.value = ''
  try {
    const res = await fetch(`${apiBase}/api/sales/receipt/post`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bookGuid: props.bookGuid,
        receiptNo: receiptForm.receiptNo || null,
        amountCent: Math.round(Number(receiptForm.amount) * 100),
        description: receiptForm.description || null,
        cashAccountName: receiptForm.cashAccountName || null
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '收款过账失败')
    message.value = '销售收款过账成功'
  } catch (e) {
    message.value = e instanceof Error ? e.message : '收款过账失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="panel">
    <h3>销售发票过账</h3>
    <form class="form" @submit.prevent="postInvoice">
      <label class="field">
        <span>发票号（可选）</span>
        <input v-model.trim="invoiceForm.invoiceNo" type="text" placeholder="SI-001" />
      </label>
      <label class="field">
        <span>金额（元，含税）</span>
        <input v-model.number="invoiceForm.amount" type="number" min="0" step="0.01" required />
      </label>
      <label class="field">
        <span>备注</span>
        <input v-model.trim="invoiceForm.description" type="text" placeholder="销售说明" />
      </label>
      <button type="submit" :disabled="loading">过账发票</button>
    </form>

    <h3>销售收款过账</h3>
    <form class="form" @submit.prevent="postReceipt">
      <label class="field">
        <span>收款单号（可选）</span>
        <input v-model.trim="receiptForm.receiptNo" type="text" placeholder="RCV-001" />
      </label>
      <label class="field">
        <span>金额（元）</span>
        <input v-model.number="receiptForm.amount" type="number" min="0" step="0.01" required />
      </label>
      <label class="field">
        <span>收款账户（默认银行存款）</span>
        <input v-model.trim="receiptForm.cashAccountName" type="text" placeholder="银行存款" />
      </label>
      <label class="field">
        <span>备注</span>
        <input v-model.trim="receiptForm.description" type="text" placeholder="收款说明" />
      </label>
      <button type="submit" :disabled="loading">过账收款</button>
    </form>

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

h3 {
  margin: 12px 0 8px;
  color: #0f172a;
}

.form {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px 12px;
  margin-bottom: 8px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 14px;
  color: #0f172a;
}

input {
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #fff;
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
  color: #0ea5e9;
  font-weight: 600;
}
</style>
