<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'

const apiBase = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'
const props = defineProps<{ bookGuid: string; mode?: 'invoice' | 'receipt' | 'list' }>()

const message = ref('')
const loading = ref(false)

const invoiceForm = reactive({
  invoiceGuid: '',
  invoiceNo: '',
  amount: 0,
  description: '',
  receivableAccountName: '应收账款'
})
const panelMode = computed(() => props.mode ?? 'invoice')
const postDate = ref(new Date().toISOString().slice(0, 10))

const receiptForm = reactive({
  receiptNo: '',
  invoiceGuid: '',
  amount: 0,
  description: '',
  cashAccountName: '银行存款',
  receiptDate: new Date().toISOString().slice(0, 10)
})

type AccountOption = { guid: string; name: string; accountType: string }
const invoiceOptions = ref<{ guid: string; name: string }[]>([])
const accounts = ref<AccountOption[]>([])
const invoiceDetails = ref<{ guid: string; name: string; status?: string; note?: string; date?: string; amount?: number }[]>([])
const ROOT_BLOCK = ['根账户', '资产', '负债', '所有者权益', '收入', '费用']
const assetAccounts = computed(() =>
  accounts.value.filter((a) => a.accountType === 'ASSET' && !ROOT_BLOCK.includes((a.name || '').trim()))
)

const postInvoice = async () => {
  if (!props.bookGuid) return
  if (!invoiceForm.invoiceGuid) {
    message.value = '请选择待过账的销售发票'
    return
  }
  loading.value = true
  message.value = ''
  try {
    const res = await fetch(`${apiBase}/api/sales/invoice/post`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bookGuid: props.bookGuid,
        invoiceGuid: invoiceForm.invoiceGuid,
        invoiceNo: invoiceForm.invoiceNo || null,
        amountCent: Math.round(Number(invoiceForm.amount) * 100),
        description: invoiceForm.description || null,
        receivableAccountName: invoiceForm.receivableAccountName || null,
        postDate: postDate.value ? `${postDate.value}T00:00:00` : null
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '发票过账失败')
    message.value = '销售发票过账成功'
    invoiceForm.amount = 0
    invoiceForm.description = ''
    invoiceForm.invoiceNo = ''
    invoiceForm.invoiceGuid = ''
    postDate.value = new Date().toISOString().slice(0, 10)
    await loadInvoices()
    await loadInvoiceDetails()
  } catch (e) {
    message.value = e instanceof Error ? e.message : '发票过账失败'
  } finally {
    loading.value = false
  }
}

const postReceipt = async () => {
  if (!props.bookGuid) return
  if (!receiptForm.invoiceGuid) {
    message.value = '请选择要结算的发票'
    return
  }
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
        invoiceGuid: receiptForm.invoiceGuid || null,
        cashAccountName: receiptForm.cashAccountName || null,
        receiptDate: receiptForm.receiptDate ? `${receiptForm.receiptDate}T00:00:00` : null
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '收款过账失败')
    message.value = '销售收款记录成功'
    receiptForm.receiptNo = ''
    receiptForm.amount = 0
    receiptForm.description = ''
    receiptForm.cashAccountName = '银行存款'
    receiptForm.receiptDate = new Date().toISOString().slice(0, 10)
    await loadInvoices()
    await loadInvoiceDetails()
  } catch (e) {
    message.value = e instanceof Error ? e.message : '收款过账失败'
  } finally {
    loading.value = false
  }
}

const loadInvoices = async () => {
  if (!props.bookGuid) return
  const status = panelMode.value === 'invoice' ? 'DRAFT' : 'POSTED'
  try {
    const res = await fetch(`${apiBase}/api/business/sales/invoices?bookGuid=${props.bookGuid}&status=${status}`)
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error()
    invoiceOptions.value = data.data || []
  } catch {
    invoiceOptions.value = []
  }
}

const loadInvoiceDetails = async () => {
  if (!props.bookGuid) return
  try {
    const res = await fetch(`${apiBase}/api/business/sales/invoices/detail?bookGuid=${props.bookGuid}`)
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error()
    invoiceDetails.value =
      (data.data || []).map((d: any) => ({
        guid: d.guid,
        name: d.name,
        status: d.status,
        note: d.note,
        date: d.date ? String(d.date).replace('T', ' ') : '',
        amount: d.amount != null ? Number(d.amount) : undefined
      })) ?? []
  } catch {
    invoiceDetails.value = []
  }
}

const loadAccounts = async () => {
  if (!props.bookGuid) return
  try {
    const res = await fetch(`${apiBase}/api/accounts/tree?bookGuid=${props.bookGuid}`)
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error()
    const flat: AccountOption[] = []
    const walk = (nodes: any[]) => {
      nodes.forEach((n) => {
        flat.push({ guid: n.guid, name: n.name, accountType: n.accountType })
        if (n.children?.length) walk(n.children)
      })
    }
    walk(data.data || [])
    accounts.value = flat.filter((a) => !ROOT_BLOCK.includes((a.name || '').trim()))
  } catch {
    accounts.value = []
  }
}

onMounted(loadInvoices)
onMounted(loadAccounts)
onMounted(loadInvoiceDetails)
watch(
  () => props.bookGuid,
  (v) => {
    if (v) {
      loadInvoices()
      loadAccounts()
      loadInvoiceDetails()
    }
  }
)

onMounted(() => {
  window.addEventListener('sales-invoices-updated', loadInvoices)
})

onUnmounted(() => {
  window.removeEventListener('sales-invoices-updated', loadInvoices)
})
</script>

<template>
  <div class="panel">
    <h3 v-if="panelMode === 'invoice'">销售发票过账</h3>
    <h3 v-else-if="panelMode === 'receipt'">销售收款</h3>
    <h3 v-else>销售发票列表</h3>

    <form v-if="panelMode === 'invoice'" class="form" @submit.prevent="postInvoice">
      <label class="field">
        <span>发票（未过账）</span>
        <select v-model="invoiceForm.invoiceGuid" required>
          <option value="">请选择发票</option>
          <option v-for="i in invoiceOptions" :key="i.guid" :value="i.guid">{{ i.name || i.guid }}</option>
        </select>
      </label>
      <label class="field">
        <span>发票号（可选）</span>
        <input v-model.trim="invoiceForm.invoiceNo" type="text" placeholder="SI-001" />
      </label>
      <label class="field">
        <span>金额（元，含税）</span>
        <input v-model.number="invoiceForm.amount" type="number" min="0" step="0.01" required />
      </label>
      <label class="field">
        <span>入账日期</span>
        <input v-model="postDate" type="date" />
      </label>
      <label class="field">
        <span>入账科目（资产类，默认应收账款）</span>
        <select v-model="invoiceForm.receivableAccountName">
          <option value="">请选择科目（默认应收账款）</option>
          <option v-for="a in assetAccounts" :key="a.guid" :value="a.name">
            {{ a.name }}
          </option>
        </select>
      </label>
      <label class="field">
        <span>备注</span>
        <input v-model.trim="invoiceForm.description" type="text" placeholder="销售说明" />
      </label>
      <button type="submit" :disabled="loading">过账发票</button>
    </form>

    <form v-else-if="panelMode === 'receipt'" class="form" @submit.prevent="postReceipt">
      <label class="field">
        <span>收款单号（可选）</span>
        <input v-model.trim="receiptForm.receiptNo" type="text" placeholder="RCV-001" />
      </label>
      <label class="field">
        <span>发票（已过账）</span>
        <select v-model="receiptForm.invoiceGuid">
          <option value="">请选择发票</option>
          <option v-for="i in invoiceOptions" :key="i.guid" :value="i.guid">{{ i.name || i.guid }}</option>
        </select>
      </label>
      <label class="field">
        <span>金额（元）</span>
        <input v-model.number="receiptForm.amount" type="number" min="0" step="0.01" required />
      </label>
      <label class="field">
        <span>收款账户（默认银行存款）</span>
        <select v-model="receiptForm.cashAccountName">
          <option value="">请选择科目（默认银行存款）</option>
          <option v-for="a in assetAccounts" :key="a.guid" :value="a.name">
            {{ a.name }}
          </option>
        </select>
      </label>
      <label class="field">
        <span>收款日期</span>
        <input v-model="receiptForm.receiptDate" type="date" />
      </label>
      <label class="field">
        <span>备注</span>
        <input v-model.trim="receiptForm.description" type="text" placeholder="收款说明" />
      </label>
      <button type="submit" :disabled="loading">提交收款</button>
    </form>

    <div v-else class="form">
      <p class="muted">仅查看发票列表</p>
    </div>

    <p v-if="message" class="message">{{ message }}</p>
    <div class="list" v-if="invoiceDetails.length">
      <h4>销售发票列表</h4>
      <ul>
        <li v-for="i in invoiceDetails" :key="i.guid">
          <strong>{{ i.name }}</strong>
          <span class="muted">状态：{{ i.status || '—' }}</span>
          <span class="muted" v-if="i.date">日期：{{ i.date }}</span>
          <span class="muted" v-if="i.amount !== undefined">金额：{{ Number(i.amount || 0).toFixed(2) }}</span>
          <span class="muted" v-if="i.note">备注：{{ i.note }}</span>
        </li>
      </ul>
    </div>
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
  margin-bottom: 8px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 14px;
  color: #0f172a;
}

input,
select {
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
.list {
  margin-top: 12px;
  color: #334155;
  font-size: 14px;
}
.list ul {
  padding-left: 16px;
}
.muted {
  color: #64748b;
  margin-left: 6px;
}
</style>
