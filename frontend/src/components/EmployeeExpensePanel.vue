<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

const props = defineProps<{
  bookGuid: string
  action: string
}>()
const apiBase = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'

const message = ref('')
const loading = ref(false)

// 员工档案
const employeeForm = reactive({
  name: '',
  id: '',
  email: '',
  phone: '',
  costCenter: '',
  project: ''
})

// 报销/差旅
const expenseForm = reactive({
  employeeGuid: '',
  amount: 0,
  description: '',
  postDate: new Date().toISOString().slice(0, 10),
  debitAccountName: '管理费用',
  expenseNo: ''
})

// 支付过账
const payForm = reactive({
  employeeGuid: '',
  expenseGuid: '',
  amount: 0,
  cashAccountName: '银行存款',
  description: '',
  payDate: new Date().toISOString().slice(0, 10)
})
const employeeOptions = ref<{ guid: string; name: string }[]>([])
const expenseOptions = ref<{ guid: string; name: string }[]>([])
const jobOptions = ref<{ guid: string; name: string }[]>([])
type AccountOption = { guid: string; name: string; accountType: string }
const accounts = ref<AccountOption[]>([])
const ROOT_BLOCK = ['根账户', '资产', '负债', '所有者权益', '收入', '费用']
const expenseAccounts = computed(() =>
  accounts.value.filter((a) => a.accountType === 'EXPENSE' && !ROOT_BLOCK.includes((a.name || '').trim()))
)
const assetAccounts = computed(() =>
  accounts.value.filter((a) => a.accountType === 'ASSET' && !ROOT_BLOCK.includes((a.name || '').trim()))
)
const employeeDetails = ref<{ guid: string; name: string; note?: string }[]>([])
const expenseDetails = ref<{ guid: string; name: string; status?: string; note?: string; date?: string; amount?: number }[]>([])
const newProject = reactive({ name: '', description: '' })

watch(
  () => props.action,
  () => {
    message.value = ''
  }
)

const saveEmployee = async () => {
  if (!props.bookGuid) {
    message.value = '缺少账本'
    return
  }
  loading.value = true
  message.value = ''
  try {
    const res = await fetch(`${apiBase}/api/employee`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bookGuid: props.bookGuid,
        name: employeeForm.name,
        id: employeeForm.id || null,
        email: employeeForm.email || null,
        phone: employeeForm.phone || null,
        notes: null,
        costCenter: employeeForm.costCenter || null,
        project: employeeForm.project || null
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '创建失败')
    message.value = `员工创建成功，GUID：${data.data}`
    await loadEmployees()
    window.dispatchEvent(new Event('employees-updated'))
  } catch (e) {
    message.value = e instanceof Error ? e.message : '创建失败'
  } finally {
    loading.value = false
  }
}

const submitExpense = async () => {
  if (!props.bookGuid) {
    message.value = '缺少账本'
    return
  }
  loading.value = true
  message.value = ''
  try {
    const res = await fetch(`${apiBase}/api/employee/expense/post`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bookGuid: props.bookGuid,
        employeeGuid: expenseForm.employeeGuid,
        amountCent: Math.round(Number(expenseForm.amount) * 100),
        description: expenseForm.description || null,
        debitAccountName: expenseForm.debitAccountName || null,
        postDate: expenseForm.postDate ? `${expenseForm.postDate}T00:00:00` : null,
        expenseNo: expenseForm.expenseNo || null
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '报销过账失败')
    message.value = '报销/差旅过账成功'
    expenseForm.amount = 0
    expenseForm.description = ''
    expenseForm.expenseNo = ''
    expenseForm.debitAccountName = '管理费用'
    expenseForm.postDate = new Date().toISOString().slice(0, 10)
    await loadExpenses()
    await loadExpenseDetails()
    await loadJobs()
  } catch (e) {
    message.value = e instanceof Error ? e.message : '报销过账失败'
  } finally {
    loading.value = false
  }
}

const submitPay = async () => {
  if (!props.bookGuid) {
    message.value = '缺少账本'
    return
  }
  if (!payForm.expenseGuid) {
    message.value = '请选择已过账编号'
    return
  }
  loading.value = true
  message.value = ''
  try {
    const res = await fetch(`${apiBase}/api/employee/pay/post`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bookGuid: props.bookGuid,
        employeeGuid: payForm.employeeGuid,
        expenseGuid: payForm.expenseGuid,
        amountCent: Math.round(Number(payForm.amount) * 100),
        cashAccountName: payForm.cashAccountName || null,
        description: payForm.description || null,
        payDate: payForm.payDate ? `${payForm.payDate}T00:00:00` : null
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '支付过账失败')
    message.value = '员工付款过账成功'
    payForm.expenseGuid = ''
    payForm.amount = 0
    payForm.description = ''
    payForm.cashAccountName = '银行存款'
    payForm.payDate = new Date().toISOString().slice(0, 10)
    await loadExpenses()
    await loadExpenseDetails()
    await loadJobs()
  } catch (e) {
    message.value = e instanceof Error ? e.message : '支付过账失败'
  } finally {
    loading.value = false
  }
}

const loadEmployees = async () => {
  if (!props.bookGuid) return
  try {
    const res = await fetch(`${apiBase}/api/business/employees/detail?bookGuid=${props.bookGuid}`)
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error()
    employeeOptions.value = (data.data || []).map((e: any) => ({ guid: e.guid, name: e.name }))
    employeeDetails.value =
      (data.data || []).map((e: any) => ({
        guid: e.guid,
        name: e.name,
        note: e.note
      })) ?? []
  } catch {
    employeeOptions.value = []
    employeeDetails.value = []
  }
}

const loadExpenses = async () => {
  if (!props.bookGuid) return
  try {
    const res = await fetch(`${apiBase}/api/employee/expenses?bookGuid=${props.bookGuid}`)
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error()
    expenseOptions.value = data.data || []
  } catch {
    expenseOptions.value = []
  }
}

const loadExpenseDetails = async () => {
  if (!props.bookGuid) return
  try {
    const res = await fetch(`${apiBase}/api/business/employee/expenses/detail?bookGuid=${props.bookGuid}`)
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error()
    expenseDetails.value =
      (data.data || []).map((d: any) => ({
        guid: d.guid,
        name: d.name,
        status: d.status,
        note: d.note,
        date: d.date ? String(d.date).replace('T', ' ') : '',
        amount: d.amount != null ? Number(d.amount) : undefined
      })) ?? []
  } catch {
    expenseDetails.value = []
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

const createProject = async () => {
  if (!props.bookGuid || !newProject.name) {
    message.value = '请填写项目名称'
    return
  }
  loading.value = true
  message.value = ''
  try {
    const res = await fetch(`${apiBase}/api/business/jobs`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bookGuid: props.bookGuid,
        name: newProject.name,
        description: newProject.description || null
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '创建失败')
    message.value = '项目创建成功'
    newProject.name = ''
    newProject.description = ''
    await loadJobs()
  } catch (e) {
    message.value = e instanceof Error ? e.message : '创建失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadEmployees)
onMounted(loadExpenses)
onMounted(loadExpenseDetails)
onMounted(loadJobs)
watch(
  () => props.bookGuid,
  (v) => {
    if (v) {
      loadEmployees()
      loadExpenses()
      loadExpenseDetails()
      loadJobs()
    }
  }
)

onMounted(() => {
  window.addEventListener('employees-updated', loadEmployees)
})

onMounted(async () => {
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
})
</script>

<template>
  <div class="panel">
    <h3 v-if="action === '员工档案'">员工档案</h3>
    <h3 v-else-if="action === '支付'">支付结算</h3>
    <h3 v-else-if="action === '项目管理'">项目管理</h3>
    <h3 v-else>报销/差旅过账</h3>

    <div v-if="action === '员工档案'" class="form">
      <label class="field">
        <span>姓名</span>
        <input v-model.trim="employeeForm.name" type="text" required />
      </label>
      <label class="field">
        <span>员工编号</span>
        <input v-model.trim="employeeForm.id" type="text" />
      </label>
      <label class="field">
        <span>邮箱</span>
        <input v-model.trim="employeeForm.email" type="email" />
      </label>
      <label class="field">
        <span>电话</span>
        <input v-model.trim="employeeForm.phone" type="text" />
      </label>
      <label class="field">
        <span>成本中心</span>
        <input v-model.trim="employeeForm.costCenter" type="text" />
      </label>
      <label class="field">
        <span>项目</span>
        <select v-model="employeeForm.project">
          <option value="">不选择项目</option>
          <option v-for="j in jobOptions" :key="j.guid" :value="j.name">{{ j.name }}</option>
        </select>
      </label>
      <button type="button" @click="saveEmployee" :disabled="loading">保存</button>
    </div>

    <div v-else-if="action === '报销/差旅'" class="form">
      <label class="field">
        <span>员工</span>
        <select v-model="expenseForm.employeeGuid">
          <option value="">请选择员工</option>
          <option v-for="e in employeeOptions" :key="e.guid" :value="e.guid">{{ e.name }}</option>
        </select>
      </label>
      <label class="field">
        <span>报销/差旅编号（可选）</span>
        <input v-model.trim="expenseForm.expenseNo" type="text" placeholder="EX-001" />
      </label>
      <label class="field">
        <span>金额（元）</span>
        <input v-model.number="expenseForm.amount" type="number" min="0" step="0.01" />
      </label>
      <label class="field">
        <span>入账日期</span>
        <input v-model="expenseForm.postDate" type="date" />
      </label>
      <label class="field">
        <span>支出科目（默认管理费用）</span>
        <select v-model="expenseForm.debitAccountName">
          <option value="">请选择科目（默认管理费用）</option>
          <option v-for="a in expenseAccounts" :key="a.guid" :value="a.name">
            {{ a.name }}
          </option>
        </select>
      </label>
      <label class="field">
        <span>描述</span>
        <input v-model.trim="expenseForm.description" type="text" placeholder="报销说明" />
      </label>
      <button type="button" @click="submitExpense" :disabled="loading">过账报销</button>
    </div>

    <div v-else-if="action === '支付'" class="form">
      <label class="field">
        <span>员工</span>
        <select v-model="payForm.employeeGuid">
          <option value="">请选择员工</option>
          <option v-for="e in employeeOptions" :key="e.guid" :value="e.guid">{{ e.name }}</option>
        </select>
      </label>
      <label class="field">
        <span>报销编号（已过账）</span>
        <select v-model="payForm.expenseGuid">
          <option value="">请选择编号</option>
          <option v-for="ex in expenseOptions" :key="ex.guid" :value="ex.guid">{{ ex.name }}</option>
        </select>
      </label>
      <label class="field">
        <span>金额（元）</span>
        <input v-model.number="payForm.amount" type="number" min="0" step="0.01" />
      </label>
      <label class="field">
        <span>支付账户（转出科目，默认银行存款）</span>
        <select v-model="payForm.cashAccountName">
          <option value="">请选择科目（默认银行存款）</option>
          <option v-for="a in assetAccounts" :key="a.guid" :value="a.name">
            {{ a.name }}
          </option>
        </select>
      </label>
      <label class="field">
        <span>支付日期</span>
        <input v-model="payForm.payDate" type="date" />
      </label>
      <label class="field">
        <span>描述</span>
        <input v-model.trim="payForm.description" type="text" placeholder="支付说明" />
      </label>
      <button type="button" @click="submitPay" :disabled="loading">提交支付</button>
    </div>

    <div v-else-if="action === '项目管理'" class="form">
      <label class="field">
        <span>项目名称</span>
        <input v-model.trim="newProject.name" type="text" placeholder="如：市场活动A" />
      </label>
      <label class="field">
        <span>项目描述（可选）</span>
        <input v-model.trim="newProject.description" type="text" placeholder="备注" />
      </label>
      <button type="button" @click="createProject" :disabled="loading">新增项目</button>
    </div>

    <p v-if="message" class="message">{{ message }}</p>
    <div v-if="action === '项目管理'" class="list">
      <p class="title">项目列表</p>
      <ul>
        <li v-for="j in jobOptions" :key="j.guid">
          <strong>{{ j.name }}</strong>
          <span class="muted">ID: {{ j.guid }}</span>
        </li>
      </ul>
    </div>
    <div v-else-if="employeeDetails.length && action !== '员工档案'" class="list">
      <h4>员工列表</h4>
      <ul>
        <li v-for="e in employeeDetails" :key="e.guid">
          <strong>{{ e.name }}</strong>
          <span class="muted">ID: {{ e.guid }}</span>
          <span class="muted" v-if="e.note">备注: {{ e.note }}</span>
        </li>
      </ul>
    </div>
    <div v-if="expenseDetails.length && action !== '员工档案' && action !== '项目管理'" class="list">
      <h4>报销/差旅记录</h4>
      <ul>
        <li v-for="ex in expenseDetails" :key="ex.guid">
          <strong>{{ ex.name }}</strong>
          <span class="muted">状态：{{ ex.status }}</span>
          <span class="muted" v-if="ex.date">日期：{{ ex.date }}</span>
          <span class="muted" v-if="ex.amount !== undefined">金额：{{ Number(ex.amount || 0).toFixed(2) }}</span>
          <span class="muted" v-if="ex.note">备注：{{ ex.note }}</span>
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
h3 {
  margin: 12px 0 8px;
  color: #0f172a;
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
  grid-column: span 2;
}
.message {
  margin-top: 8px;
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
