<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

const props = defineProps<{
  bookGuid: string
}>()

type AccountOption = {
  guid: string
  name: string
  accountType: string
}

type ReconcileItem = {
  guid: string
  memo: string
  amountCent: number
  date: string
}

const apiBase = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'
const ROOT_BLOCK = ['根账户', '资产', '负债', '所有者权益', '收入', '费用']

const accounts = ref<AccountOption[]>([])
const items = ref<ReconcileItem[]>([])
const selectedGuids = ref<string[]>([])
const loading = ref(false)
const message = ref('')
const listTotalCent = ref(0)

const form = reactive({
  accountGuid: '',
  start: '',
  end: '',
  includeChildren: true,
  beginBalance: 0,
  endBalance: 0
})

const formatAmount = (cent: number) =>
  (cent / 100).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const formatDate = (value: string) => {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}

const selectedSumCent = computed(() =>
  items.value
    .filter((i) => selectedGuids.value.includes(i.guid))
    .reduce((sum, i) => sum + Number(i.amountCent || 0), 0)
)

const listTotalYuan = computed(() => listTotalCent.value / 100)
const selectedSumYuan = computed(() => selectedSumCent.value / 100)
const reconciledBalance = computed(() => form.beginBalance + selectedSumYuan.value)
const diff = computed(() => form.endBalance - reconciledBalance.value)

const loadAccounts = async () => {
  if (!props.bookGuid) return
  try {
    const res = await fetch(`${apiBase}/api/accounts/tree?bookGuid=${props.bookGuid}`)
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '加载科目失败')
    const flat: AccountOption[] = []
    const walk = (nodes: any[]) => {
      nodes.forEach((n) => {
        flat.push({ guid: n.guid, name: n.name, accountType: n.accountType })
        if (n.children?.length) walk(n.children)
      })
    }
    walk(data.data || [])
    accounts.value = flat.filter((a) => !ROOT_BLOCK.includes((a.name || '').trim()))
  } catch (e) {
    console.error(e)
    accounts.value = []
  }
}

const loadItems = async () => {
  if (!props.bookGuid || !form.accountGuid) {
    message.value = '请选择要对账的科目'
    return
  }
  loading.value = true
  message.value = ''
  selectedGuids.value = []
  try {
    const params = new URLSearchParams({
      bookGuid: props.bookGuid,
      accountGuid: form.accountGuid,
      includeChildren: String(form.includeChildren)
    })
    if (form.start) params.append('start', form.start)
    if (form.end) params.append('end', form.end)
    const res = await fetch(`${apiBase}/api/reconcile/account?${params.toString()}`)
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '查询失败')
    items.value =
      (data.data?.items || []).map((i: any) => ({
        guid: i.guid,
        memo: i.memo,
        amountCent: Number(i.amountCent || 0),
        date: i.date || ''
      })) ?? []
    listTotalCent.value = Number(data.data?.totalCent || 0)
    if (!items.value.length) {
      message.value = '该时间范围内暂无流水'
    }
  } catch (e) {
    message.value = e instanceof Error ? e.message : '查询失败'
    items.value = []
    listTotalCent.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  const today = new Date()
  const start = new Date()
  start.setDate(1)
  form.start = start.toISOString().slice(0, 10)
  form.end = today.toISOString().slice(0, 10)
})

onMounted(loadAccounts)
watch(
  () => props.bookGuid,
  (val, oldVal) => {
    if (val && val !== oldVal) {
      loadAccounts()
      items.value = []
      selectedGuids.value = []
    }
  }
)
</script>

<template>
  <div class="panel">
    <header class="header">
      <div>
        <h3>对账</h3>
        <p class="muted">选择科目、输入期初/期末金额，勾选流水辅助核对</p>
      </div>
      <button class="ghost" type="button" @click="loadItems" :disabled="loading">加载明细</button>
    </header>

    <div class="filters">
      <label class="field">
        <span>科目</span>
        <select v-model="form.accountGuid">
          <option value="" disabled>请选择科目</option>
          <option v-for="a in accounts" :key="a.guid" :value="a.guid">
            {{ a.name }}（{{ a.accountType }}）
          </option>
        </select>
      </label>
      <label class="field">
        <span>开始日期</span>
        <input v-model="form.start" type="date" />
      </label>
      <label class="field">
        <span>结束日期</span>
        <input v-model="form.end" type="date" />
      </label>
      <label class="field">
        <span>期初余额（元）</span>
        <input v-model.number="form.beginBalance" type="number" step="0.01" />
      </label>
      <label class="field">
        <span>对账金额/期末余额（元）</span>
        <input v-model.number="form.endBalance" type="number" step="0.01" />
      </label>
      <label class="checkbox">
        <input v-model="form.includeChildren" type="checkbox" />
        <span>包含子科目流水</span>
      </label>
    </div>

    <p v-if="message" class="message">{{ message }}</p>

    <div class="summary">
      <div class="summary-item">
        <span class="label">列表合计</span>
        <strong>¥ {{ listTotalYuan.toFixed(2) }}</strong>
      </div>
      <div class="summary-item">
        <span class="label">已选金额</span>
        <strong>¥ {{ selectedSumYuan.toFixed(2) }}</strong>
      </div>
      <div class="summary-item">
        <span class="label">已对账余额（期初+已选）</span>
        <strong>¥ {{ reconciledBalance.toFixed(2) }}</strong>
      </div>
      <div class="summary-item" :class="{ danger: Math.abs(diff) > 0.0001 }">
        <span class="label">差额（期末-已对账）</span>
        <strong>¥ {{ diff.toFixed(2) }}</strong>
      </div>
    </div>

    <div class="list" v-if="items.length">
      <div class="list-header">
        <span>选择</span>
        <span>日期</span>
        <span>摘要</span>
        <span class="amount-col">金额（元）</span>
      </div>
      <div v-for="item in items" :key="item.guid" class="list-row">
        <input v-model="selectedGuids" type="checkbox" :value="item.guid" />
        <span>{{ formatDate(item.date) }}</span>
        <span>{{ item.memo || '无摘要' }}</span>
        <span class="amount" :class="{ negative: item.amountCent < 0 }">
          ¥ {{ formatAmount(item.amountCent) }}
        </span>
      </div>
    </div>
    <div v-else class="empty">暂无对账流水</div>
  </div>
</template>

<style scoped>
.panel {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
  background: #f8fafc;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 8px;
  margin-bottom: 10px;
}
.muted {
  color: #64748b;
  margin: 4px 0 0;
}
.filters {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 10px;
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
  padding: 8px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #fff;
}
.checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}
.ghost {
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #fff;
  cursor: pointer;
}
.message {
  color: #0ea5e9;
  margin: 6px 0;
}
.summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 8px;
  margin: 12px 0;
}
.summary-item {
  padding: 10px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #e2e8f0;
}
.summary-item .label {
  color: #64748b;
  font-size: 13px;
}
.summary-item strong {
  display: block;
  margin-top: 4px;
  font-size: 16px;
}
.summary-item.danger {
  border-color: #fca5a5;
  background: #fef2f2;
  color: #b91c1c;
}
.list {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
}
.list-header,
.list-row {
  display: grid;
  grid-template-columns: 60px 140px 1fr 140px;
  gap: 8px;
  align-items: center;
  padding: 10px;
}
.list-header {
  background: #f1f5f9;
  font-weight: 700;
}
.list-row:not(:last-child) {
  border-bottom: 1px solid #e2e8f0;
}
.amount {
  text-align: right;
  font-weight: 700;
}
.amount-col {
  text-align: right;
}
.negative {
  color: #b91c1c;
}
.empty {
  color: #94a3b8;
  text-align: center;
  padding: 12px 0;
}
</style>
