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
  description: ''
})

// 支付过账
const payForm = reactive({
  employeeGuid: '',
  amount: 0,
  cashAccountName: '',
  description: ''
})
const employeeOptions = ref<{ guid: string; name: string }[]>([])

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
        description: expenseForm.description || null
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '报销过账失败')
    message.value = '报销/差旅过账成功'
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
  loading.value = true
  message.value = ''
  try {
    const res = await fetch(`${apiBase}/api/employee/pay/post`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bookGuid: props.bookGuid,
        employeeGuid: payForm.employeeGuid,
        amountCent: Math.round(Number(payForm.amount) * 100),
        cashAccountName: payForm.cashAccountName || null,
        description: payForm.description || null
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '支付过账失败')
    message.value = '员工付款过账成功'
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

onMounted(loadEmployees)
watch(
  () => props.bookGuid,
  (v) => v && loadEmployees()
)

onMounted(() => {
  window.addEventListener('employees-updated', loadEmployees)
})
</script>

<template>
  <div class="panel">
    <h3 v-if="action === '员工档案'">员工档案</h3>
    <h3 v-else-if="action === '报销/差旅'">报销/差旅过账</h3>
    <h3 v-else>支付结算过账</h3>

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
        <span>金额（元）</span>
        <input v-model.number="expenseForm.amount" type="number" min="0" step="0.01" />
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
        <span>金额（元）</span>
        <input v-model.number="payForm.amount" type="number" min="0" step="0.01" />
      </label>
      <label class="field">
        <span>付款账户（默认银行存款）</span>
        <input v-model.trim="payForm.cashAccountName" type="text" placeholder="银行存款" />
      </label>
      <label class="field">
        <span>描述</span>
        <input v-model.trim="payForm.description" type="text" placeholder="支付说明" />
      </label>
      <button type="button" @click="submitPay" :disabled="loading">过账付款</button>
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
