<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'

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
type AccountOption = { guid: string; name: string; accountType: string }
const accounts = ref<AccountOption[]>([])

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
  } catch (e) {
    message.value = e instanceof Error ? e.message : '支付过账失败'
  } finally {
    loading.value = false
  }
}

const loadEmployees = async () => {
  if (!props.bookGuid) return
  try {
    const res = await fetch(`${apiBase}/api/business/employees?bookGuid=${props.bookGuid}`)
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error()
    employeeOptions.value = data.data || []
  } catch {
    employeeOptions.value = []
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

onMounted(loadEmployees)
onMounted(loadExpenses)
watch(
  () => props.bookGuid,
  (v) => v && loadEmployees()
)
watch(
  () => props.bookGuid,
  (v) => v && loadExpenses()
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
    accounts.value = flat
  } catch {
    accounts.value = []
  }
})
</script>

<template>
  <div class="panel">
    <h3 v-if="action === '员工档案'">员工档案</h3>
    <h3 v-else-if="action === '支付'">支付结算</h3>
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
        <input v-model.trim="employeeForm.project" type="text" />
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
          <option
            v-for="a in accounts.filter((x) => x.accountType === 'EXPENSE')"
            :key="a.guid"
            :value="a.name"
          >
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

    <div v-else class="form">
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
          <option
            v-for="a in accounts.filter((x) => x.accountType === 'ASSET')"
            :key="a.guid"
            :value="a.name"
          >
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
</style>
