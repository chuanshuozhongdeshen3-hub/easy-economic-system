<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

const props = defineProps<{
  bookGuid: string
  mode?: 'post' | 'pay' | 'list'
}>()

const apiBase = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'
const message = ref('')
const loading = ref(false)

const orderForm = reactive({
  orderGuid: '',
  orderId: '',
  amount: 0,
  description: '',
  payableAccountName: '应付账款'
})
const panelMode = computed(() => props.mode ?? 'post')
const postDate = ref(new Date().toISOString().slice(0, 10))
const draftOrders = ref<{ guid: string; name: string }[]>([])
const postedOrders = ref<{ guid: string; name: string }[]>([])
const orderDetails = ref<{ guid: string; name: string; status?: string; note?: string; date?: string; amount?: number }[]>([])
type AccountOption = { guid: string; name: string; accountType: string }
const accounts = ref<AccountOption[]>([])
const ROOT_BLOCK = ['根账户', '资产', '负债', '所有者权益', '收入', '费用']
const liabilityAccounts = computed(() =>
  accounts.value.filter((a) => a.accountType === 'LIABILITY' && !ROOT_BLOCK.includes((a.name || '').trim()))
)
const assetAccounts = computed(() =>
  accounts.value.filter((a) => a.accountType === 'ASSET' && !ROOT_BLOCK.includes((a.name || '').trim()))
)

const paymentForm = reactive({
  orderGuid: '',
  payNo: '',
  amount: 0,
  description: '',
  cashAccountName: '银行存款',
  payDate: new Date().toISOString().slice(0, 10)
})

const postOrder = async () => {
  if (!props.bookGuid) return
  if (!orderForm.orderGuid) {
    message.value = '请选择未过账的采购订单'
    return
  }
  loading.value = true
  message.value = ''
  try {
    const res = await fetch(`${apiBase}/api/purchase/invoice/post`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bookGuid: props.bookGuid,
        orderGuid: orderForm.orderGuid,
        invoiceNo: orderForm.orderId || null,
        amountCent: Math.round(Number(orderForm.amount) * 100),
        description: orderForm.description || null,
        payableAccountName: orderForm.payableAccountName || null,
        postDate: postDate.value ? `${postDate.value}T00:00:00` : null
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '过账失败')
    message.value = '采购订单过账成功'
    orderForm.orderGuid = ''
    orderForm.orderId = ''
    orderForm.amount = 0
    orderForm.description = ''
    orderForm.payableAccountName = '应付账款'
    postDate.value = new Date().toISOString().slice(0, 10)
    await loadDraftOrders()
    await loadPostedOrders()
    await loadOrderDetails()
  } catch (e) {
    message.value = e instanceof Error ? e.message : '过账失败'
  } finally {
    loading.value = false
  }
}

const postPayment = async () => {
  if (!props.bookGuid) return
  if (!paymentForm.orderGuid) {
    message.value = '请选择已过账的订单'
    return
  }
  loading.value = true
  message.value = ''
  try {
    const res = await fetch(`${apiBase}/api/purchase/payment/post`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bookGuid: props.bookGuid,
        orderGuid: paymentForm.orderGuid,
        payNo: paymentForm.payNo || null,
        amountCent: Math.round(Number(paymentForm.amount) * 100),
        description: paymentForm.description || null,
        cashAccountName: paymentForm.cashAccountName || null,
        payDate: paymentForm.payDate ? `${paymentForm.payDate}T00:00:00` : null
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '支付失败')
    message.value = '支付记录成功'
    paymentForm.orderGuid = ''
    paymentForm.payNo = ''
    paymentForm.amount = 0
    paymentForm.description = ''
    paymentForm.cashAccountName = '银行存款'
    paymentForm.payDate = new Date().toISOString().slice(0, 10)
    await loadPostedOrders()
    await loadOrderDetails()
  } catch (e) {
    message.value = e instanceof Error ? e.message : '支付失败'
  } finally {
    loading.value = false
  }
}

const loadDraftOrders = async () => {
  if (!props.bookGuid) return
  try {
    const res = await fetch(`${apiBase}/api/business/purchase/orders?bookGuid=${props.bookGuid}&status=DRAFT`)
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error()
    draftOrders.value = data.data || []
  } catch {
    draftOrders.value = []
  }
}

const loadPostedOrders = async () => {
  if (!props.bookGuid) return
  try {
    const res = await fetch(`${apiBase}/api/business/purchase/orders?bookGuid=${props.bookGuid}&status=POSTED`)
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error()
    postedOrders.value = data.data || []
  } catch {
    postedOrders.value = []
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

const loadOrderDetails = async () => {
  if (!props.bookGuid) return
  try {
    const res = await fetch(`${apiBase}/api/business/purchase/orders/detail?bookGuid=${props.bookGuid}`)
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error()
    orderDetails.value =
      (data.data || []).map((d: any) => ({
        guid: d.guid,
        name: d.name,
        status: d.status,
        note: d.note,
        date: d.date ? String(d.date).replace('T', ' ') : '',
        amount: d.amount != null ? Number(d.amount) : undefined
      })) ?? []
  } catch {
    orderDetails.value = []
  }
}

onMounted(() => {
  loadDraftOrders()
  loadPostedOrders()
  loadAccounts()
  loadOrderDetails()
})
watch(
  () => props.bookGuid,
  (v) => {
    if (v) {
      loadDraftOrders()
      loadPostedOrders()
      loadOrderDetails()
      loadAccounts()
    }
  }
)
watch(
  () => props.mode,
  () => {
    // 切换模式时刷新对应列表
    loadDraftOrders()
    loadPostedOrders()
    loadOrderDetails()
  }
)
</script>

<template>
  <div class="panel">
    <h3 v-if="panelMode === 'post'">采购订单过账</h3>
    <h3 v-else-if="panelMode === 'pay'">采购支付</h3>
    <h3 v-else>采购订单列表</h3>

    <form v-if="panelMode === 'post'" class="form" @submit.prevent="postOrder">
      <label class="field">
        <span>订单（未过账）</span>
        <select v-model="orderForm.orderGuid" required>
          <option value="">请选择订单</option>
          <option v-for="o in draftOrders" :key="o.guid" :value="o.guid">{{ o.name || o.guid }}</option>
        </select>
      </label>
      <label class="field">
        <span>金额（元，含税）</span>
        <input v-model.number="orderForm.amount" type="number" min="0" step="0.01" required />
      </label>
      <label class="field">
        <span>入账日期</span>
        <input v-model="postDate" type="date" />
      </label>
      <label class="field">
        <span>入账科目（负债类，默认应付账款）</span>
        <select v-model="orderForm.payableAccountName">
          <option value="">请选择科目（默认应付账款）</option>
          <option v-for="a in liabilityAccounts" :key="a.guid" :value="a.name">
            {{ a.name }}
          </option>
        </select>
      </label>
      <label class="field">
        <span>订单号/备注（可选）</span>
        <input v-model.trim="orderForm.orderId" type="text" placeholder="PO-001" />
      </label>
      <label class="field">
        <span>备注</span>
        <input v-model.trim="orderForm.description" type="text" placeholder="采购说明" />
      </label>
      <button type="submit" :disabled="loading">过账订单</button>
    </form>

    <form v-else-if="panelMode === 'pay'" class="form" @submit.prevent="postPayment">
      <label class="field">
        <span>订单（已过账）</span>
        <select v-model="paymentForm.orderGuid" required>
          <option value="">请选择订单</option>
          <option v-for="o in postedOrders" :key="o.guid" :value="o.guid">{{ o.name || o.guid }}</option>
        </select>
      </label>
      <label class="field">
        <span>支付单号（可选）</span>
        <input v-model.trim="paymentForm.payNo" type="text" placeholder="PAY-001" />
      </label>
      <label class="field">
        <span>金额（元）</span>
        <input v-model.number="paymentForm.amount" type="number" min="0" step="0.01" required />
      </label>
      <label class="field">
        <span>支付账户（转出科目）</span>
        <select v-model="paymentForm.cashAccountName">
          <option value="">请选择科目（默认银行存款）</option>
          <option v-for="a in assetAccounts" :key="a.guid" :value="a.name">
            {{ a.name }}
          </option>
        </select>
      </label>
      <label class="field">
        <span>支付日期</span>
        <input v-model="paymentForm.payDate" type="date" />
      </label>
      <label class="field">
        <span>备注</span>
        <input v-model.trim="paymentForm.description" type="text" placeholder="支付说明" />
      </label>
      <button type="submit" :disabled="loading">提交支付</button>
    </form>

    <div v-else class="form">
      <p class="muted">仅查看订单列表</p>
    </div>

    <p v-if="message" class="message">{{ message }}</p>
    <div class="list" v-if="orderDetails.length">
      <h4>采购订单列表</h4>
      <ul>
        <li v-for="o in orderDetails" :key="o.guid">
          <strong>{{ o.name }}</strong>
          <span class="muted">状态：{{ o.status || '—' }}</span>
          <span class="muted" v-if="o.date">日期：{{ o.date }}</span>
          <span class="muted" v-if="o.amount !== undefined">金额：{{ Number(o.amount || 0).toFixed(2) }}</span>
          <span class="muted" v-if="o.note">备注：{{ o.note }}</span>
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
