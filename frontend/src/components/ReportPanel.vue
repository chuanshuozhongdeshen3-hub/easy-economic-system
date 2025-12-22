<script setup lang="ts">
import { defineComponent, h, ref, watch } from 'vue'

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

const formatMoney = (v: any) => {
  const num = Number(v || 0)
  return num.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const expanded = ref<Set<string>>(new Set())
const plExpanded = ref<Set<string>>(new Set())
const toggleExpand = (id: string) => {
  const next = new Set(expanded.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  expanded.value = next
}
const togglePl = (id: string) => {
  const next = new Set(plExpanded.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  plExpanded.value = next
}
type BsNodeType = { guid: string; name: string; balance: number; children?: BsNodeType[] }
const BsNode = defineComponent({
  name: 'BsNode',
  props: {
    nodes: {
      type: Array as () => BsNodeType[],
      required: true
    },
    level: {
      type: Number,
      required: true
    },
    expanded: {
      type: Object as () => Set<string>,
      required: true
    },
    onToggle: {
      type: Function as unknown as () => (id: string) => void,
      required: true
    }
  },
  setup(props) {
    const render = (nodes: BsNodeType[], level: number): any =>
      h(
        'ul',
        { class: 'bs-tree' },
        nodes.map((node) => {
          const hasChild = !!(node.children && node.children.length)
          const isOpen = props.expanded.has(node.guid)
          return h('li', { key: node.guid }, [
            h(
              'div',
              { class: 'row', style: { paddingLeft: `${level * 12}px` } },
              [
                hasChild
                  ? h(
                      'button',
                      {
                        class: 'toggle',
                        onClick: () => props.onToggle && props.onToggle(node.guid)
                      },
                      isOpen ? '－' : '＋'
                    )
                  : null,
                h('span', { class: 'label' }, node.name),
                h('span', { class: 'num' }, formatMoney(node.balance))
              ].filter(Boolean)
            ),
            hasChild && isOpen ? render(node.children!, level + 1) : null
          ])
        })
      )
    return () => render(props.nodes, props.level)
  }
})
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

    <div v-if="data && props.type === 'pl'" class="table-wrap pl-wrap">
      <h4>利润表</h4>
      <div class="section">
        <h5>收入</h5>
        <BsNode :nodes="data.incomeTree" :level="0" :expanded="plExpanded" :onToggle="togglePl" />
        <div class="subtotal">
          <span>收入合计</span>
          <span class="num">{{ formatMoney(data.totalIncome) }}</span>
        </div>
      </div>
      <div class="section">
        <h5>支出</h5>
        <BsNode :nodes="data.expenseTree" :level="0" :expanded="plExpanded" :onToggle="togglePl" />
        <div class="subtotal">
          <span>支出合计</span>
          <span class="num">-{{ formatMoney(data.totalExpense) }}</span>
        </div>
      </div>
      <div class="total-line">
        <span>净利润</span>
        <span class="num">{{ formatMoney(data.netProfit) }}</span>
      </div>
    </div>

    <div v-else-if="data && props.type === 'bs'" class="table-wrap bs-layout">
      <h4>资产负债表</h4>
      <div class="section">
        <h5>资产</h5>
        <BsNode :nodes="data.assets" :level="0" :expanded="expanded" :onToggle="toggleExpand" />
        <div class="subtotal">
          <span>资产合计</span>
          <span class="num">{{ formatMoney(data.totalAssets) }}</span>
        </div>
      </div>
      <div class="section">
        <h5>负债</h5>
        <BsNode :nodes="data.liabilities" :level="0" :expanded="expanded" :onToggle="toggleExpand" />
        <div class="subtotal">
          <span>负债合计</span>
          <span class="num">{{ formatMoney(data.totalLiabilities) }}</span>
        </div>
      </div>
      <div class="section">
        <h5>所有者权益</h5>
        <BsNode :nodes="data.equity" :level="0" :expanded="expanded" :onToggle="toggleExpand" />
        <div class="subtotal">
          <span>权益合计</span>
          <span class="num">{{ formatMoney(data.totalEquity) }}</span>
        </div>
      </div>
      <div class="total-line">
        <span>负债和所有者权益合计</span>
        <span class="num">{{ formatMoney((data.totalLiabilities || 0) + (data.totalEquity || 0)) }}</span>
      </div>
    </div>

    <div v-else-if="data && props.type === 'cf'" class="table-wrap cf-wrap">
      <h4>现金流量表（简版）</h4>
      <div class="section">
        <h5>资金来源</h5>
        <ul>
          <li v-for="i in data.inflows" :key="`in-${i.name}`">
            <span>{{ i.name }}</span>
            <span class="num">{{ formatMoney(i.amount) }}</span>
          </li>
        </ul>
        <div class="subtotal">
          <span>资金流入</span>
          <span class="num">{{ formatMoney(data.totalInflow) }}</span>
        </div>
      </div>
      <div class="section">
        <h5>资金流出</h5>
        <ul>
          <li v-for="o in data.outflows" :key="`out-${o.name}`">
            <span>{{ o.name }}</span>
            <span class="num">-{{ formatMoney(o.amount) }}</span>
          </li>
        </ul>
        <div class="subtotal">
          <span>资金流出</span>
          <span class="num">-{{ formatMoney(data.totalOutflow) }}</span>
        </div>
      </div>
      <div class="total">
        <span>差额</span>
        <span class="num">{{ formatMoney(data.netChange) }}</span>
      </div>
      <div class="footer">
        <span>期初余额</span>
        <span class="num">{{ formatMoney(data.beginBalance) }}</span>
        <span>期末余额</span>
        <span class="num">{{ formatMoney(data.endBalance) }}</span>
      </div>
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

.bs-layout {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.section {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 8px;
}
.section h5 {
  margin: 0 0 6px;
  color: #0f172a;
}
.bs-tree {
  list-style: none;
  padding-left: 0;
  margin: 0;
}
.bs-tree > li {
  border-bottom: 1px dashed #e2e8f0;
  padding: 4px 0;
}
.bs-tree ul {
  list-style: none;
  padding-left: 14px;
  margin: 4px 0 0;
  border-left: 1px dashed #e2e8f0;
}
.toggle {
  width: 22px;
  height: 22px;
  margin-right: 6px;
  border: 1px solid #cbd5e1;
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
}
.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.label {
  color: #0f172a;
}
.subtotal {
  display: flex;
  justify-content: space-between;
  font-weight: 700;
  margin-top: 6px;
}
.total-line {
  display: flex;
  justify-content: space-between;
  font-weight: 700;
  color: #0ea5e9;
  border-top: 1px solid #e2e8f0;
  padding-top: 6px;
}
.cf-wrap .section {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 8px;
  margin-bottom: 10px;
}
.cf-wrap h5 {
  margin: 0 0 6px;
}
.cf-wrap ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
.cf-wrap li {
  display: flex;
  justify-content: space-between;
  border-bottom: 1px dashed #e2e8f0;
  padding: 4px 0;
}
.cf-wrap .subtotal,
.cf-wrap .total,
.cf-wrap .footer {
  display: flex;
  justify-content: space-between;
  font-weight: 700;
  margin-top: 6px;
}
.cf-wrap .footer {
  gap: 8px;
  border-top: 1px solid #e2e8f0;
  padding-top: 6px;
}
</style>
