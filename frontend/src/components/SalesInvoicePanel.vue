<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'

const message = ref('')
const apiBase = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'
const props = defineProps<{ bookGuid: string }>()
const loading = ref(false)
const form = reactive({
  invoiceNo: '',
  customerGuid: '',
  jobGuid: '',
  description: ''
})
const entryForm = reactive({
  accountGuid: '',
  amount: 0,
  description: ''
})
const entries = ref<{ accountGuid: string; amount: number; description: string }[]>([])
type AccountOption = { guid: string; name: string; accountType: string }
const accounts = ref<AccountOption[]>([])
const ROOT_BLOCK = ['根账户', '资产', '负债', '所有者权益', '收入', '费用']
const incomeAccounts = computed(() =>
  accounts.value.filter((a) => a.accountType === 'INCOME' && !ROOT_BLOCK.includes((a.name || '').trim()))
)
const customers = ref<{ guid: string; name: string }[]>([])
const jobOptions = ref<{ guid: string; name: string }[]>([])

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
        jobGuid: form.jobGuid || null,
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
    window.dispatchEvent(new Event('sales-invoices-updated'))
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

const loadCustomers = async () => {
  if (!props.bookGuid) return
  try {
    const res = await fetch(`${apiBase}/api/business/customers?bookGuid=${props.bookGuid}`)
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error()
    customers.value = data.data || []
  } catch {
    customers.value = []
  }
}

const loadJobs = async () => {
  if (!props.bookGuid) return
  try {
    const res = await fetch(`${apiBase}/api/business/jobs?bookGuid=${props.bookGuid}`)
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error()
    jobOptions.value = data.data || []
  } catch {
    jobOptions.value = []
  }
}

onMounted(() => {
  loadAccounts()
  loadCustomers()
  loadJobs()
})
watch(
  () => props.bookGuid,
  (v) => {
    if (v) {
      loadAccounts()
      loadCustomers()
      loadJobs()
    }
  }
)

// 监听客户更新事件
onMounted(() => {
  window.addEventListener('customers-updated', loadCustomers)
})

onUnmounted(() => {
  window.removeEventListener('customers-updated', loadCustomers)
})
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
        <select v-model="form.customerGuid">
          <option value="">请选择客户</option>
          <option v-for="c in customers" :key="c.guid" :value="c.guid">{{ c.name }}</option>
        </select>
      </label>
      <label class="field">
        <span>项目（可选）</span>
        <select v-model="form.jobGuid">
          <option value="">不选择项目</option>
          <option v-for="j in jobOptions" :key="j.guid" :value="j.guid">{{ j.name }}</option>
        </select>
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
          <span>科目</span>
          <select v-model="entryForm.accountGuid">
            <option value="">请选择科目</option>
            <option v-for="a in incomeAccounts" :key="a.guid" :value="a.guid">{{ a.name }}</option>
          </select>
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
select {
  padding: 10px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
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
.muted {
  color: #64748b;
  margin-left: 6px;
}
.list {
  margin-top: 12px;
  color: #334155;
  font-size: 14px;
}
.list ul {
  padding-left: 16px;
}
</style>
