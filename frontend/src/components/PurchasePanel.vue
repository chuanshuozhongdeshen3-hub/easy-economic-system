<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'

const props = defineProps<{
  bookGuid: string
}>()

const apiBase = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'
const message = ref('')
const loading = ref(false)

const invoiceForm = reactive({
  invoiceNo: '',
  invoiceGuid: '',
  orderGuid: '',
  amount: 0,
  description: '',
  debitAccountName: ''
})

const paymentForm = reactive({
  payNo: '',
  invoiceGuid: '',
  amount: 0,
  description: '',
  cashAccountName: ''
})
const orderOptions = ref<{ guid: string; name: string }[]>([])
const invoiceOptions = ref<{ guid: string; name: string }[]>([])

const postInvoice = async () => {
  if (!props.bookGuid) return
  loading.value = true
  message.value = ''
  try {
    const res = await fetch(`${apiBase}/api/purchase/invoice/post`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bookGuid: props.bookGuid,
        invoiceNo: invoiceForm.invoiceNo || null,
        amountCent: Math.round(Number(invoiceForm.amount) * 100),
        description: invoiceForm.description || null,
        debitAccountName: invoiceForm.debitAccountName || null,
        invoiceGuid: invoiceForm.invoiceGuid || null,
        orderGuid: invoiceForm.orderGuid || null
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '过账失败')
    message.value = '采购发票过账成功'
    await loadOrders()
    await loadInvoices()
  } catch (e) {
    message.value = e instanceof Error ? e.message : '过账失败'
  } finally {
    loading.value = false
  }
}

const postPayment = async () => {
  if (!props.bookGuid) return
  loading.value = true
  message.value = ''
  try {
    const res = await fetch(`${apiBase}/api/purchase/payment/post`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bookGuid: props.bookGuid,
        payNo: paymentForm.payNo || null,
        amountCent: Math.round(Number(paymentForm.amount) * 100),
        description: paymentForm.description || null,
        cashAccountName: paymentForm.cashAccountName || null,
        invoiceGuid: paymentForm.invoiceGuid || null
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '付款过账失败')
    message.value = '采购付款过账成功'
    await loadInvoices()
  } catch (e) {
    message.value = e instanceof Error ? e.message : '付款过账失败'
  } finally {
    loading.value = false
  }
}

const loadOrders = async () => {
  if (!props.bookGuid) return
  try {
    const res = await fetch(`${apiBase}/api/business/purchase/orders?bookGuid=${props.bookGuid}`)
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error()
    orderOptions.value = data.data || []
  } catch {
    orderOptions.value = []
  }
}

const loadInvoices = async () => {
  if (!props.bookGuid) return
  try {
    const res = await fetch(`${apiBase}/api/business/purchase/invoices?bookGuid=${props.bookGuid}`)
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error()
    invoiceOptions.value = data.data || []
  } catch {
    invoiceOptions.value = []
  }
}

onMounted(() => {
  loadOrders()
  loadInvoices()
})
watch(
  () => props.bookGuid,
  (v) => {
    if (v) {
      loadOrders()
      loadInvoices()
    }
  }
)
</script>

<template>
  <div class="panel">
    <h3>采购发票过账</h3>
    <form class="form" @submit.prevent="postInvoice">
      <label class="field">
        <span>发票号（可选）</span>
        <input v-model.trim="invoiceForm.invoiceNo" type="text" placeholder="PI-001" />
      </label>
      <label class="field">
        <span>发票 GUID（可选，用于回写状态）</span>
        <input v-model.trim="invoiceForm.invoiceGuid" type="text" placeholder="发票 GUID" />
      </label>
      <label class="field">
        <span>订单（可选）</span>
        <select v-model="invoiceForm.orderGuid">
          <option value="">请选择订单</option>
          <option v-for="o in orderOptions" :key="o.guid" :value="o.guid">{{ o.name || o.guid }}</option>
        </select>
      </label>
      <label class="field">
        <span>金额（元，含税）</span>
        <input v-model.number="invoiceForm.amount" type="number" min="0" step="0.01" required />
      </label>
      <label class="field">
        <span>借方科目（默认存货/成本/管理费用）</span>
        <input v-model.trim="invoiceForm.debitAccountName" type="text" placeholder="存货" />
      </label>
      <label class="field">
        <span>备注</span>
        <input v-model.trim="invoiceForm.description" type="text" placeholder="采购说明" />
      </label>
      <button type="submit" :disabled="loading">过账发票</button>
    </form>

    <h3>采购付款过账</h3>
    <form class="form" @submit.prevent="postPayment">
      <label class="field">
        <span>付款单号（可选）</span>
        <input v-model.trim="paymentForm.payNo" type="text" placeholder="PAY-001" />
      </label>
      <label class="field">
        <span>发票 GUID（可选，用于结算）</span>
        <select v-model="paymentForm.invoiceGuid">
          <option value="">请选择发票</option>
          <option v-for="i in invoiceOptions" :key="i.guid" :value="i.guid">{{ i.name || i.guid }}</option>
        </select>
      </label>
      <label class="field">
        <span>金额（元）</span>
        <input v-model.number="paymentForm.amount" type="number" min="0" step="0.01" required />
      </label>
      <label class="field">
        <span>付款账户（默认银行存款）</span>
        <input v-model.trim="paymentForm.cashAccountName" type="text" placeholder="银行存款" />
      </label>
      <label class="field">
        <span>备注</span>
        <input v-model.trim="paymentForm.description" type="text" placeholder="付款说明" />
      </label>
      <button type="submit" :disabled="loading">过账付款</button>
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
