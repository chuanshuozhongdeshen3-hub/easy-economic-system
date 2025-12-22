<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

const message = ref('')
const apiBase = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'
const props = defineProps<{ bookGuid: string }>()
const loading = ref(false)
const form = reactive({
  orderNo: '',
  vendorGuid: '',
  jobGuid: '',
  description: ''
})
type AccountOption = { guid: string; name: string; accountType: string }
const accounts = ref<AccountOption[]>([])
const ROOT_BLOCK = ['根账户', '资产', '负债', '所有者权益', '收入', '费用']
const expenseAccounts = computed(() =>
  accounts.value.filter((a) => a.accountType === 'EXPENSE' && !ROOT_BLOCK.includes((a.name || '').trim()))
)
const vendorOptions = ref<{ guid: string; name: string }[]>([])
const jobOptions = ref<{ guid: string; name: string }[]>([])
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
    const res = await fetch(`${apiBase}/api/business/purchase/orders`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bookGuid: props.bookGuid,
        vendorGuid: form.vendorGuid,
        jobGuid: form.jobGuid || null,
        orderId: form.orderNo || null,
        notes: form.description || null
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '创建失败')
    message.value = '采购订单创建成功'
    // 如果有行项则提交行项
    if (entries.value.length) {
      await submitEntries(data.data)
    }
    await loadVendors()
    await loadJobs()
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

const submitEntries = async (orderGuid: string) => {
  const res = await fetch(`${apiBase}/api/business/purchase/orders/${orderGuid}/entries`, {
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

const loadVendors = async () => {
  if (!props.bookGuid) return
  try {
    const res = await fetch(`${apiBase}/api/business/vendors?bookGuid=${props.bookGuid}`)
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error()
    vendorOptions.value = data.data || []
  } catch {
    vendorOptions.value = []
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
  loadVendors()
  loadJobs()
  window.addEventListener('vendors-updated', loadVendors)
})
watch(
  () => props.bookGuid,
  (v) => {
    if (v) {
      loadAccounts()
      loadVendors()
      loadJobs()
    }
  }
)
</script>

<template>
  <div class="panel">
    <h3>新增采购订单</h3>
    <div class="form">
      <label class="field">
        <span>订单编号</span>
        <input v-model.trim="form.orderNo" type="text" placeholder="PO-001" />
      </label>
      <label class="field">
        <span>供应商</span>
        <select v-model="form.vendorGuid">
          <option value="">请选择供应商</option>
          <option v-for="v in vendorOptions" :key="v.guid" :value="v.guid">{{ v.name }}</option>
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
            <option v-for="a in expenseAccounts" :key="a.guid" :value="a.guid">{{ a.name }}</option>
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
</style>
