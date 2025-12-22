<script setup lang="ts">
import { ref, watch } from 'vue'

type ReportType = 'pl' | 'bs' | 'cf'

const props = defineProps<{
  bookGuid: string
  type: ReportType
}>()

const loading = ref(false)
const error = ref('')
const data = ref<any>(null)
const dateStart = ref<string>(new Date().getFullYear() + '-01-01')
const dateEnd = ref<string>(new Date().toISOString().slice(0, 10))
const asOf = ref<string>(new Date().toISOString().slice(0, 10))

const fetchReport = async () => {
  if (!props.bookGuid) {
    error.value = '请先选择账本'
    return
  }
  loading.value = true
  error.value = ''
  data.value = null
  try {
    let url = ''
    if (props.type === 'pl') {
      url = `/api/reports/pl?bookGuid=${props.bookGuid}&start=${dateStart.value}&end=${dateEnd.value}`
    } else if (props.type === 'bs') {
      url = `/api/reports/bs?bookGuid=${props.bookGuid}&asOf=${asOf.value}`
    } else {
      url = `/api/reports/cf?bookGuid=${props.bookGuid}&start=${dateStart.value}&end=${dateEnd.value}`
    }
    const resp = await fetch(url)
    const contentType = resp.headers.get('content-type') || ''
    if (!contentType.includes('application/json')) {
      const text = await resp.text()
      throw new Error(`服务返回非JSON响应：${text.slice(0, 120)}`)
    }
    const json = await resp.json()
    if (!json.success) {
      throw new Error(json.message || '查询失败')
    }
    data.value = json.data
  } catch (e: any) {
    error.value = e?.message ?? String(e)
  } finally {
    loading.value = false
  }
}

watch(
  () => props.type,
  () => {
    // reset data when switching report type
    data.value = null
    error.value = ''
  }
)
</script>

<template>
  <div class="report-card">
    <div class="report-controls">
      <template v-if="props.type === 'bs'">
        <label>截止日</label>
        <input v-model="asOf" type="date" />
      </template>
      <template v-else>
        <label>开始日期</label>
        <input v-model="dateStart" type="date" />
        <label>结束日期</label>
        <input v-model="dateEnd" type="date" />
      </template>
      <button type="button" :disabled="loading" @click="fetchReport">{{ loading ? '查询中...' : '生成报表' }}</button>
    </div>

    <p v-if="error" class="error">{{ error }}</p>

    <div v-if="data && props.type === 'pl'" class="table-wrap">
      <h4>利润表</h4>
      <table>
        <thead>
          <tr>
            <th>项目</th>
            <th class="num">金额（元）</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in data.incomeItems" :key="`inc-${item.name}`">
            <td>{{ item.name }}</td>
            <td class="num">{{ item.amount }}</td>
          </tr>
          <tr class="subtotal">
            <td>收入合计</td>
            <td class="num">{{ data.totalIncome }}</td>
          </tr>
          <tr v-for="item in data.expenseItems" :key="`exp-${item.name}`">
            <td>{{ item.name }}</td>
            <td class="num">-{{ item.amount }}</td>
          </tr>
          <tr class="subtotal">
            <td>费用合计</td>
            <td class="num">-{{ data.totalExpense }}</td>
          </tr>
          <tr class="total">
            <td>净利润</td>
            <td class="num">{{ data.netProfit }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else-if="data && props.type === 'bs'" class="table-wrap three-cols">
      <h4>资产负债表</h4>
      <div class="col">
        <h5>资产</h5>
        <ul>
          <li v-for="item in data.assets" :key="`asset-${item.name}`">
            <span>{{ item.name }}</span>
            <span class="num">{{ item.amount }}</span>
          </li>
        </ul>
        <div class="subtotal">
          <span>资产合计</span>
          <span class="num">{{ data.totalAssets }}</span>
        </div>
      </div>
      <div class="col">
        <h5>负债</h5>
        <ul>
          <li v-for="item in data.liabilities" :key="`liab-${item.name}`">
            <span>{{ item.name }}</span>
            <span class="num">{{ item.amount }}</span>
          </li>
        </ul>
        <div class="subtotal">
          <span>负债合计</span>
          <span class="num">{{ data.totalLiabilities }}</span>
        </div>
      </div>
      <div class="col">
        <h5>所有者权益</h5>
        <ul>
          <li v-for="item in data.equity" :key="`eq-${item.name}`">
            <span>{{ item.name }}</span>
            <span class="num">{{ item.amount }}</span>
          </li>
        </ul>
        <div class="subtotal">
          <span>权益合计</span>
          <span class="num">{{ data.totalEquity }}</span>
        </div>
      </div>
    </div>

    <div v-else-if="data && props.type === 'cf'" class="table-wrap">
      <h4>现金流量表（现金及现金等价物净额）</h4>
      <table>
        <tbody>
          <tr>
            <td>期初余额</td>
            <td class="num">{{ data.beginBalance }}</td>
          </tr>
          <tr>
            <td>净增加额</td>
            <td class="num">{{ data.netChange }}</td>
          </tr>
          <tr class="total">
            <td>期末余额</td>
            <td class="num">{{ data.endBalance }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else-if="!loading" class="placeholder">
      <p>请选择日期并点击“生成报表”。</p>
    </div>
  </div>
</template>

<style scoped>
.report-card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
  background: #fff;
}

.report-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}

label {
  font-size: 13px;
  color: #475569;
}

input {
  padding: 6px 8px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
}

button {
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #0ea5e9;
  color: #fff;
  cursor: pointer;
  transition: all 0.2s ease;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  color: #dc2626;
  margin: 4px 0;
}

.table-wrap {
  margin-top: 8px;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 8px;
  border-bottom: 1px solid #e2e8f0;
}

th {
  text-align: left;
  color: #0f172a;
}

.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.subtotal td,
.subtotal span {
  font-weight: 600;
}

.total td,
.total span {
  font-weight: 700;
  color: #0ea5e9;
}

.placeholder {
  color: #94a3b8;
  font-size: 14px;
}

.three-cols {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}

.col {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 8px;
}

.col h5 {
  margin: 0 0 6px;
  color: #0f172a;
}

.col ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.col li {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  border-bottom: 1px dashed #e2e8f0;
  padding-bottom: 4px;
}

.col .subtotal {
  display: flex;
  justify-content: space-between;
  font-weight: 600;
  margin-top: 6px;
}
</style>
