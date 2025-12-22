<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

const apiBase = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'
const props = defineProps<{ bookGuid: string; mode?: string }>()

type TaxRate = { guid: string; name: string; ratePercent: number; direction: string; payableAccountGuid: string }
type AccountOption = { guid: string; name: string; accountType: string }

const message = ref('')
const loading = ref(false)
const rates = ref<TaxRate[]>([])
const accounts = ref<AccountOption[]>([])
const result = reactive({ base: 0, tax: 0, total: 0, rate: 0 })

const calcForm = reactive({
  amount: 0,
  taxTableGuid: '',
  baseAccountGuid: '',
  cashAccountGuid: '',
  description: ''
})

const rateForm = reactive({
  name: '',
  ratePercent: 6,
  direction: 'OUTPUT',
  payableAccountGuid: '',
  description: ''
})

const currentDirection = computed(() => {
  const r = rates.value.find((x) => x.guid === calcForm.taxTableGuid)
  return r?.direction ?? 'OUTPUT'
})

const expenseAccounts = computed(() =>
  accounts.value.filter((a) => a.accountType === 'EXPENSE' || a.accountType === 'ASSET')
)
const incomeAccounts = computed(() => accounts.value.filter((a) => a.accountType === 'INCOME'))
const cashAccounts = computed(() => accounts.value.filter((a) => a.accountType === 'ASSET'))
const payableAccounts = computed(() => accounts.value.filter((a) => a.accountType === 'LIABILITY'))

const loadRates = async () => {
  if (!props.bookGuid) return
  try {
    const res = await fetch(`${apiBase}/api/tax/rates?bookGuid=${props.bookGuid}`)
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '加载税率失败')
    rates.value = data.data || []
    if (rates.value.length) {
      calcForm.taxTableGuid = rates.value[0]?.guid || ''
    }
  } catch (e) {
    rates.value = []
    message.value = e instanceof Error ? e.message : '加载税率失败'
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
    accounts.value = flat
  } catch {
    accounts.value = []
  }
}

const calculate = async () => {
  if (!calcForm.taxTableGuid || !calcForm.amount) {
    message.value = '请选择税率并输入金额'
    return
  }
  loading.value = true
  message.value = ''
  try {
    const res = await fetch(`${apiBase}/api/tax/calc`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        taxTableGuid: calcForm.taxTableGuid,
        amountCent: Math.round(Number(calcForm.amount) * 100)
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '计算失败')
    result.base = data.data.baseCent / 100
    result.tax = data.data.taxCent / 100
    result.total = data.data.totalCent / 100
    result.rate = data.data.ratePercent
  } catch (e) {
    message.value = e instanceof Error ? e.message : '计算失败'
  } finally {
    loading.value = false
  }
}

const post = async () => {
  if (!calcForm.taxTableGuid || !calcForm.amount || !calcForm.baseAccountGuid) {
    message.value = '请完善税率、金额、基础科目'
    return
  }
  loading.value = true
  message.value = ''
  try {
    const res = await fetch(`${apiBase}/api/tax/post`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bookGuid: props.bookGuid,
        taxTableGuid: calcForm.taxTableGuid,
        amountCent: Math.round(Number(calcForm.amount) * 100),
        baseAccountGuid: calcForm.baseAccountGuid,
        cashAccountGuid: calcForm.cashAccountGuid || null,
        description: calcForm.description || null
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '过账失败')
    message.value = '税务过账成功'
  } catch (e) {
    message.value = e instanceof Error ? e.message : '过账失败'
  } finally {
    loading.value = false
  }
}

const createRate = async () => {
  if (!rateForm.name || !rateForm.payableAccountGuid) {
    message.value = '请填写税率名称和挂账科目'
    return
  }
  loading.value = true
  message.value = ''
  try {
    const res = await fetch(`${apiBase}/api/tax/rates`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bookGuid: props.bookGuid,
        name: rateForm.name,
        ratePercent: rateForm.ratePercent,
        direction: rateForm.direction,
        payableAccountGuid: rateForm.payableAccountGuid,
        description: rateForm.description || null
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '创建税率失败')
    message.value = '税率创建成功'
    await loadRates()
  } catch (e) {
    message.value = e instanceof Error ? e.message : '创建税率失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadRates()
  loadAccounts()
})
watch(
  () => props.bookGuid,
  (v) => v && (loadRates(), loadAccounts())
)
</script>

<template>
  <div class="panel">
    <h3>{{ mode === '税率维护' ? '税率维护' : '税务计算与过账' }}</h3>

    <div v-if="mode !== '税率维护'" class="form">
      <label class="field">
        <span>税率</span>
        <select v-model="calcForm.taxTableGuid">
          <option value="">请选择</option>
          <option v-for="r in rates" :key="r.guid" :value="r.guid">
            {{ r.name }} ({{ r.ratePercent }}%, {{ r.direction }})
          </option>
        </select>
      </label>
      <label class="field">
        <span>金额（元）</span>
        <input v-model.number="calcForm.amount" type="number" min="0" step="0.01" />
      </label>
      <label class="field">
        <span>基础科目</span>
        <select v-model="calcForm.baseAccountGuid">
          <option value="">请选择</option>
          <option
            v-for="a in currentDirection === 'INPUT' ? expenseAccounts : incomeAccounts"
            :key="a.guid"
            :value="a.guid"
          >
            {{ a.name }}
          </option>
        </select>
      </label>
      <label class="field">
        <span>现金科目（平衡，默认银行存款）</span>
        <select v-model="calcForm.cashAccountGuid">
          <option value="">默认银行存款</option>
          <option v-for="a in cashAccounts" :key="a.guid" :value="a.guid">{{ a.name }}</option>
        </select>
      </label>
      <label class="field">
        <span>备注</span>
        <input v-model.trim="calcForm.description" type="text" placeholder="可选" />
      </label>
      <div class="actions">
        <button type="button" @click="calculate" :disabled="loading">计算</button>
        <button type="button" @click="post" :disabled="loading">计算并过账</button>
      </div>
      <div class="result" v-if="result.total">
        <p>基数：{{ result.base.toFixed(2) }} 元</p>
        <p>税额：{{ result.tax.toFixed(2) }} 元</p>
        <p>合计：{{ result.total.toFixed(2) }} 元（税率 {{ result.rate }}%）</p>
      </div>
    </div>

    <div v-else class="form">
      <label class="field">
        <span>名称</span>
        <input v-model.trim="rateForm.name" type="text" />
      </label>
      <label class="field">
        <span>税率（%）</span>
        <input v-model.number="rateForm.ratePercent" type="number" min="0" step="0.01" />
      </label>
      <label class="field">
        <span>方向</span>
        <select v-model="rateForm.direction">
          <option value="OUTPUT">OUTPUT（销项）</option>
          <option value="INPUT">INPUT（进项）</option>
        </select>
      </label>
      <label class="field">
        <span>挂账科目（应交税费）</span>
        <select v-model="rateForm.payableAccountGuid">
          <option value="">请选择</option>
          <option v-for="a in payableAccounts" :key="a.guid" :value="a.guid">{{ a.name }}</option>
        </select>
      </label>
      <label class="field">
        <span>备注</span>
        <input v-model.trim="rateForm.description" type="text" placeholder="可选" />
      </label>
      <button type="button" @click="createRate" :disabled="loading">保存税率</button>

      <div class="list" v-if="rates.length">
        <p class="title">已有税率：</p>
        <ul>
          <li v-for="r in rates" :key="r.guid">
            <strong>{{ r.name }}</strong>
            <span class="muted">{{ r.ratePercent }}% · {{ r.direction }}</span>
          </li>
        </ul>
      </div>
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
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 10px 12px;
  margin-bottom: 8px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
input,
select {
  padding: 10px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
}
.actions {
  display: flex;
  gap: 10px;
}
button {
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #16a34a;
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: #fff;
  cursor: pointer;
}
.result {
  grid-column: span 2;
  background: #fff;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  padding: 10px;
}
.message {
  margin-top: 8px;
  color: #0ea5e9;
  font-weight: 600;
}
.list {
  grid-column: span 2;
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
.title {
  margin: 0 0 4px;
}
</style>
