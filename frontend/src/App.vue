<script setup lang="ts">
import { computed, onMounted, reactive, watch } from 'vue'
import AuthPanel from './components/AuthPanel.vue'
import AccountTree from './components/AccountTree.vue'
import PurchasePanel from './components/PurchasePanel.vue'
import SupplierPanel from './components/SupplierPanel.vue'
import CustomerPanel from './components/CustomerPanel.vue'
import PurchaseOrderPanel from './components/PurchaseOrderPanel.vue'
import SalesInvoicePanel from './components/SalesInvoicePanel.vue'
import SalesPanel from './components/SalesPanel.vue'
import EmployeeExpensePanel from './components/EmployeeExpensePanel.vue'
import ReportPanel from './components/ReportPanel.vue'
import TaxPanel from './components/TaxPanel.vue'
import ReconcilePanel from './components/ReconcilePanel.vue'

const apiBase = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'

type ModuleAction = { label: string; action: string; hint?: string }
type ModuleBlock = { name: string; description: string; actions: ModuleAction[] }
type StatCard = { title: string; value: string; change: string; tone: 'positive' | 'warning' | 'neutral'; hint: string }

const session = reactive({
  userId: localStorage.getItem('user_id') ?? '',
  bookGuid: localStorage.getItem('book_guid') ?? '',
  username: ''
})

const selection = reactive({
  module: '',
  action: ''
})

const dashboard = reactive({
  cashTotal: 0,
  receivableOutstanding: 0,
  payableOutstanding: 0,
  receivedTotal: 0,
  paidTotal: 0,
  receivableProgress: 0,
  payableProgress: 0,
  salesInvoiceCount: 0,
  salesReceiptCount: 0,
  purchaseInvoiceCount: 0,
  purchasePaymentCount: 0,
  receivablePendingCount: 0,
  payablePendingCount: 0,
  taxDue: 0,
  loaded: false
})

const modalSelection = reactive({
  module: '',
  action: ''
})

const modalExceptions = new Set<string>([
  '采购-供应商列表',
  '采购-订单列表',
  '销售-客户列表',
  '销售-发票列表',
  '员工费用-员工列表',
  '报表-利润表',
  '报表-资产负债表',
  '报表-现金流量表',
  '其他-项目管理',
  '税务-税率维护'
])

const isModalAction = (module: string, action: string) => !modalExceptions.has(`${module}-${action}`)

const statCards = computed<StatCard[]>(() => [
  {
    title: '现金余额',
    value: `¥${formatAmount(dashboard.cashTotal)}`,
    change: `已收 ${dashboard.salesReceiptCount} 笔`,
    tone: 'positive',
    hint: '含银行/现金账户'
  },
  {
    title: '应收账款',
    value: `¥${formatAmount(dashboard.receivableOutstanding)}`,
    change: `待回款 ${dashboard.receivablePendingCount} 笔`,
    tone: 'warning',
    hint: '催收节奏与账龄'
  },
  {
    title: '应付账款',
    value: `¥${formatAmount(dashboard.payableOutstanding)}`,
    change: `待付款 ${dashboard.payablePendingCount} 笔`,
    tone: 'neutral',
    hint: '付款计划与资金池'
  },
  {
    title: '税费准备',
    value: `¥${formatAmount(dashboard.taxDue ?? 0)}`,
    change: '含应交税费',
    tone: 'neutral',
    hint: '自动计提数据'
  }
])

const modules: ModuleBlock[] = [
  {
    name: '采购',
    description: '供应商管理、采购订单、付款、过账',
    actions: [
      { label: '新增供应商', action: '新增供应商', hint: '录入新供应商档案' },
      { label: '供应商列表', action: '供应商列表', hint: '查看/维护往来' },
      { label: '新增订单', action: '新增订单', hint: '创建采购订单' },
      { label: '订单列表', action: '订单列表', hint: '跟踪采购执行' },
      { label: '过账', action: '过账', hint: '采购入库/发票过账' },
      { label: '支付', action: '支付', hint: '应付付款处理' }
    ]
  },
  {
    name: '销售',
    description: '客户、发票、出库、收款',
    actions: [
      { label: '新增发票', action: '新增发票', hint: '开具销售发票' },
      { label: '新增客户', action: '新增客户', hint: '建立客户档案' },
      { label: '客户列表', action: '客户列表', hint: '客户往来/信用' },
      { label: '发票列表', action: '发票列表', hint: '在途发票明细' },
      { label: '过账', action: '过账', hint: '出库与收入确认' },
      { label: '收款', action: '收款', hint: '销售收款登记' }
    ]
  },
  {
    name: '员工费用',
    description: '员工档案、报销、预支',
    actions: [
      { label: '员工档案', action: '员工档案', hint: '人事信息与往来' },
      { label: '员工列表', action: '员工列表', hint: '全员清单' },
      { label: '报销/差旅', action: '报销/差旅', hint: '费用报销与审批' },
      { label: '支付', action: '支付', hint: '费用付款' }
    ]
  },
  {
    name: '报表',
    description: '利润表、资产负债表、现金流量表',
    actions: [
      { label: '利润表', action: '利润表', hint: '查看损益表现' },
      { label: '资产负债表', action: '资产负债表', hint: '资产结构与负债' },
      { label: '现金流量表', action: '现金流量表', hint: '经营/投资/筹资' }
    ]
  },
  {
    name: '税务',
    description: '税额计算、税率维护与过账',
    actions: [
      { label: '计算/过账', action: '计算/过账', hint: '自动计提及过账' },
      { label: '税率维护', action: '税率维护', hint: '税种/税率维护' }
    ]
  },
  {
    name: '其他',
    description: '项目管理、对账辅助',
    actions: [
      { label: '项目管理', action: '项目管理', hint: '项目维度核算' },
      { label: '对账', action: '对账', hint: '银行与往来对账' }
    ]
  }
]

const welcomeName = computed(() => {
  if (session.username) return session.username
  if (session.userId) return `用户 ${session.userId}`
  return '访客'
})

const handleAuthSuccess = (payload: { id: number; username: string; bookGuid: string }) => {
  session.userId = String(payload.id ?? '')
  session.bookGuid = payload.bookGuid ?? ''
  session.username = payload.username ?? ''
}

const handleLogout = () => {
  session.userId = ''
  session.bookGuid = ''
  session.username = ''
  localStorage.removeItem('user_id')
  localStorage.removeItem('book_guid')
}

const selectAction = (moduleName: string, actionName: string) => {
  if (isModalAction(moduleName, actionName)) {
    modalSelection.module = moduleName
    modalSelection.action = actionName
  } else {
    selection.module = moduleName
    selection.action = actionName
  }
}

const clearSelection = () => {
  selection.module = ''
  selection.action = ''
}

const clearModal = () => {
  modalSelection.module = ''
  modalSelection.action = ''
}

const formatAmount = (value: number) =>
  (value ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const loadDashboard = async () => {
  if (!session.bookGuid) return
  try {
    const res = await fetch(`${apiBase}/api/dashboard/summary?bookGuid=${session.bookGuid}`)
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '加载失败')
    const d = data.data || {}
    dashboard.cashTotal = Number(d.cashTotal || 0)
    dashboard.receivableOutstanding = Number(d.receivableOutstanding || 0)
    dashboard.payableOutstanding = Number(d.payableOutstanding || 0)
    dashboard.receivedTotal = Number(d.receivedTotal || 0)
    dashboard.paidTotal = Number(d.paidTotal || 0)
    dashboard.receivableProgress = Number(d.receivableProgress || 0)
    dashboard.payableProgress = Number(d.payableProgress || 0)
    dashboard.salesInvoiceCount = Number(d.salesInvoiceCount || 0)
    dashboard.salesReceiptCount = Number(d.salesReceiptCount || 0)
    dashboard.purchaseInvoiceCount = Number(d.purchaseInvoiceCount || 0)
    dashboard.purchasePaymentCount = Number(d.purchasePaymentCount || 0)
    dashboard.receivablePendingCount = Number(d.receivablePendingCount || 0)
    dashboard.payablePendingCount = Number(d.payablePendingCount || 0)
    dashboard.taxDue = Number(d.taxDue || 0)
    dashboard.loaded = true
  } catch {
    dashboard.loaded = false
  }
}

watch(
  () => session.bookGuid,
  (val) => {
    if (val) loadDashboard()
  }
)

onMounted(() => {
  if (session.bookGuid) loadDashboard()
})
</script>

<template>
  <main class="page">
    <section v-if="!session.bookGuid" class="auth-shell">
      <div class="auth-hero">
        <p class="eyebrow">财务系统 · 轻量总账</p>
        <h1>打造真实的财务作业闭环</h1>
        <p class="muted">支持登录/注册账本、采购销售闭环、费用报销与税务计提。</p>
        <div class="pill-row">
          <span class="pill pill-strong">财务账本</span>
          <span class="pill">总账/报表</span>
          <span class="pill">采购销售流程</span>
        </div>
      </div>
      <AuthPanel @success="handleAuthSuccess" />
    </section>

    <section v-else class="dashboard">
      <header class="hero">
        <div class="hero__info">
          <p class="eyebrow">财务驾驶舱</p>
          <h1>总账与凭证中心</h1>
          <p class="muted">当前账本 GUID：<span class="mono">{{ session.bookGuid }}</span> · 欢迎，{{ welcomeName }}</p>
          <div class="pill-row">
            <span class="pill pill-strong">实时余额</span>
            <span class="pill">自动过账</span>
            <span class="pill">合规报表</span>
          </div>
          <div class="hero-actions">
            <button class="primary" type="button" @click="selectAction('销售', '新增发票')">快速开票</button>
            <button class="ghost" type="button" @click="selectAction('采购', '新增订单')">录入采购</button>
            <button class="link" type="button" @click="handleLogout">退出</button>
          </div>
        </div>
        <div class="hero__panel">
          <div class="hero__badge">
            <span>资金总额</span>
            <strong v-if="dashboard.loaded">¥ {{ formatAmount(dashboard.cashTotal) }}</strong>
            <strong v-else>加载中...</strong>
            <small>含现金、银行、理财</small>
          </div>
          <div class="hero__split">
            <div>
              <p class="eyebrow">收款进度</p>
              <h3 v-if="dashboard.loaded">{{ dashboard.receivableProgress.toFixed(1) }}%</h3>
              <h3 v-else>--%</h3>
              <small>
                开票 {{ dashboard.salesInvoiceCount }} 单
                · 回款 {{ dashboard.salesReceiptCount }} 单
              </small>
            </div>
            <div>
              <p class="eyebrow">付款计划</p>
              <h3 v-if="dashboard.loaded">{{ dashboard.payableProgress.toFixed(1) }}%</h3>
              <h3 v-else>--%</h3>
              <small>
                支付 {{ dashboard.purchaseInvoiceCount }} 单
                · 已付 {{ dashboard.purchasePaymentCount }} 单
              </small>
            </div>
          </div>
          <div class="hero__footer">凭证、发票、账务串联，随时过账。</div>
        </div>
      </header>

      <section class="stats-grid">
        <div v-for="card in statCards" :key="card.title" class="stat-card">
          <p class="eyebrow">{{ card.title }}</p>
          <h3>{{ card.value }}</h3>
          <div class="stat-meta">
            <span :class="['chip', card.tone]">{{ card.change }}</span>
            <small>{{ card.hint }}</small>
          </div>
        </div>
      </section>

      <section class="action-grid">
        <div v-for="module in modules" :key="module.name" class="action-card">
          <div class="action-header">
            <div>
              <p class="eyebrow">模块</p>
              <h3>{{ module.name }}</h3>
              <p class="muted">{{ module.description }}</p>
            </div>
            <div class="action-accent" aria-hidden="true"></div>
          </div>
          <div class="action-buttons">
            <button v-for="item in module.actions" :key="item.action" type="button" @click="selectAction(module.name, item.action)">
              <div class="action-label">
                <span>{{ item.label }}</span>
                <small class="muted">{{ item.hint }}</small>
              </div>
              <span class="arrow">→</span>
            </button>
          </div>
        </div>
      </section>

      <section class="workspace">
        <div class="workbench">
          <div class="workbench-header">
            <div>
              <p class="eyebrow">工作台</p>
              <h3 v-if="selection.module">{{ selection.module }} · {{ selection.action }}</h3>
              <h3 v-else>请选择左侧模块</h3>
              <p class="muted">当前账本：<span class="mono">{{ session.bookGuid }}</span></p>
            </div>
            <div class="workbench-actions">
              <button v-if="selection.module" class="ghost" type="button" @click="clearSelection">关闭工作区</button>
            </div>
          </div>

          <div class="workbench-body">
            <PurchasePanel v-if="selection.module === '采购' && selection.action === '过账'" :book-guid="session.bookGuid" mode="post" />
            <PurchasePanel v-else-if="selection.module === '采购' && selection.action === '支付'" :book-guid="session.bookGuid" mode="pay" />
            <PurchasePanel v-else-if="selection.module === '采购' && selection.action === '订单列表'" :book-guid="session.bookGuid" mode="list" />
            <SupplierPanel v-else-if="selection.module === '采购' && selection.action === '新增供应商'" :book-guid="session.bookGuid" mode="create" />
            <SupplierPanel v-else-if="selection.module === '采购' && selection.action === '供应商列表'" :book-guid="session.bookGuid" mode="list" />
            <PurchaseOrderPanel v-else-if="selection.module === '采购' && selection.action === '新增订单'" :book-guid="session.bookGuid" />

            <SalesInvoicePanel v-else-if="selection.module === '销售' && selection.action === '新增发票'" :book-guid="session.bookGuid" />
            <CustomerPanel v-else-if="selection.module === '销售' && selection.action === '新增客户'" :book-guid="session.bookGuid" mode="create" />
            <CustomerPanel v-else-if="selection.module === '销售' && selection.action === '客户列表'" :book-guid="session.bookGuid" mode="list" />
            <SalesPanel v-else-if="selection.module === '销售' && selection.action === '发票列表'" :book-guid="session.bookGuid" mode="list" />
            <SalesPanel v-else-if="selection.module === '销售' && selection.action === '过账'" :book-guid="session.bookGuid" mode="invoice" />
            <SalesPanel v-else-if="selection.module === '销售' && selection.action === '收款'" :book-guid="session.bookGuid" mode="receipt" />

            <EmployeeExpensePanel
              v-else-if="selection.module === '员工费用' && selection.action === '员工列表'"
              :book-guid="session.bookGuid"
              action="员工列表"
            />
            <EmployeeExpensePanel v-else-if="selection.module === '员工费用'" :book-guid="session.bookGuid" :action="selection.action" />

            <ReportPanel v-else-if="selection.module === '报表' && selection.action === '利润表'" :book-guid="session.bookGuid" type="pl" />
            <ReportPanel v-else-if="selection.module === '报表' && selection.action === '资产负债表'" :book-guid="session.bookGuid" type="bs" />
            <ReportPanel v-else-if="selection.module === '报表' && selection.action === '现金流量表'" :book-guid="session.bookGuid" type="cf" />

            <TaxPanel v-else-if="selection.module === '税务'" :book-guid="session.bookGuid" :mode="selection.action" />

            <EmployeeExpensePanel v-else-if="selection.module === '其他' && selection.action === '项目管理'" :book-guid="session.bookGuid" action="项目管理" />
            <ReconcilePanel v-else-if="selection.module === '其他' && selection.action === '对账'" :book-guid="session.bookGuid" />

            <div v-else class="placeholder">
              <p>选择一个操作开始处理账务或凭证。</p>
            </div>
          </div>
        </div>

        <aside class="sidebar">
          <div class="sidebar-card">
            <div class="sidebar-header">
              <p class="eyebrow">会计科目</p>
              <h4>科目余额树</h4>
              <p class="muted">展开科目，查看余额与层级。</p>
            </div>
            <AccountTree :book-guid="session.bookGuid" />
          </div>
        </aside>
      </section>

      <div v-if="modalSelection.module" class="modal-mask" @click.self="clearModal">
        <div class="modal-shell">
          <div class="workbench">
            <div class="workbench-header">
              <div>
                <p class="eyebrow">工作台</p>
                <h3>{{ modalSelection.module }} · {{ modalSelection.action }}</h3>
                <p class="muted">当前账本：<span class="mono">{{ session.bookGuid }}</span></p>
              </div>
              <div class="workbench-actions">
                <button class="ghost" type="button" @click="clearModal">关闭</button>
              </div>
            </div>

            <div class="workbench-body">
              <PurchasePanel v-if="modalSelection.module === '采购' && modalSelection.action === '过账'" :book-guid="session.bookGuid" mode="post" />
              <PurchasePanel v-else-if="modalSelection.module === '采购' && modalSelection.action === '支付'" :book-guid="session.bookGuid" mode="pay" />
              <PurchasePanel v-else-if="modalSelection.module === '采购' && modalSelection.action === '订单列表'" :book-guid="session.bookGuid" mode="list" />
              <SupplierPanel v-else-if="modalSelection.module === '采购' && modalSelection.action === '新增供应商'" :book-guid="session.bookGuid" mode="create" />
              <SupplierPanel v-else-if="modalSelection.module === '采购' && modalSelection.action === '供应商列表'" :book-guid="session.bookGuid" mode="list" />
              <PurchaseOrderPanel v-else-if="modalSelection.module === '采购' && modalSelection.action === '新增订单'" :book-guid="session.bookGuid" />

              <SalesInvoicePanel v-else-if="modalSelection.module === '销售' && modalSelection.action === '新增发票'" :book-guid="session.bookGuid" />
              <CustomerPanel v-else-if="modalSelection.module === '销售' && modalSelection.action === '新增客户'" :book-guid="session.bookGuid" mode="create" />
              <CustomerPanel v-else-if="modalSelection.module === '销售' && modalSelection.action === '客户列表'" :book-guid="session.bookGuid" mode="list" />
              <SalesPanel v-else-if="modalSelection.module === '销售' && modalSelection.action === '发票列表'" :book-guid="session.bookGuid" mode="list" />
              <SalesPanel v-else-if="modalSelection.module === '销售' && modalSelection.action === '过账'" :book-guid="session.bookGuid" mode="invoice" />
              <SalesPanel v-else-if="modalSelection.module === '销售' && modalSelection.action === '收款'" :book-guid="session.bookGuid" mode="receipt" />

              <EmployeeExpensePanel
                v-else-if="modalSelection.module === '员工费用' && modalSelection.action === '员工列表'"
                :book-guid="session.bookGuid"
                action="员工列表"
              />
              <EmployeeExpensePanel
                v-else-if="modalSelection.module === '员工费用'"
                :book-guid="session.bookGuid"
                :action="modalSelection.action"
              />

              <ReportPanel v-else-if="modalSelection.module === '报表' && modalSelection.action === '利润表'" :book-guid="session.bookGuid" type="pl" />
              <ReportPanel v-else-if="modalSelection.module === '报表' && modalSelection.action === '资产负债表'" :book-guid="session.bookGuid" type="bs" />
              <ReportPanel v-else-if="modalSelection.module === '报表' && modalSelection.action === '现金流量表'" :book-guid="session.bookGuid" type="cf" />

              <TaxPanel v-else-if="modalSelection.module === '税务'" :book-guid="session.bookGuid" :mode="modalSelection.action" />

              <EmployeeExpensePanel
                v-else-if="modalSelection.module === '其他' && modalSelection.action === '项目管理'"
                :book-guid="session.bookGuid"
                action="项目管理"
              />
              <ReconcilePanel v-else-if="modalSelection.module === '其他' && modalSelection.action === '对账'" :book-guid="session.bookGuid" />

              <div v-else class="placeholder">
                <p>选择一个操作开始处理账务或凭证。</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: radial-gradient(circle at 10% 10%, rgba(59, 130, 246, 0.16), transparent 32%),
    radial-gradient(circle at 80% 0%, rgba(52, 211, 153, 0.14), transparent 32%),
    linear-gradient(140deg, #0b1224, #0f172a 45%, #0b1224);
  padding: 32px 16px 40px;
}

.auth-shell {
  max-width: 1180px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 24px;
  align-items: center;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  padding: 24px;
  box-shadow: 0 16px 60px rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(10px);
}

.auth-hero h1 {
  margin: 4px 0 10px;
  font-size: 28px;
  color: #e2e8f0;
}

.auth-hero .muted {
  color: #cbd5e1;
}

.dashboard {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 16px;
  background: linear-gradient(135deg, #0ea5e9, #2563eb);
  border-radius: 18px;
  padding: 22px;
  color: #f8fafc;
  box-shadow: 0 16px 40px rgba(14, 165, 233, 0.35);
}

.hero__info h1 {
  margin: 6px 0;
  font-size: 28px;
}

.hero .muted {
  color: #e0f2fe;
}

.hero__panel {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 16px;
  padding: 16px;
  display: grid;
  gap: 10px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.hero__badge {
  background: rgba(15, 23, 42, 0.35);
  border-radius: 12px;
  padding: 12px;
  display: grid;
  gap: 4px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.hero__badge strong {
  font-size: 22px;
}

.hero__split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.hero__split div {
  background: rgba(15, 23, 42, 0.45);
  border-radius: 10px;
  padding: 10px;
}

.hero__footer {
  font-size: 14px;
  color: #dbeafe;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.primary {
  background: #fefefe;
  color: #0f172a;
  border: none;
  padding: 11px 16px;
  border-radius: 10px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.18);
}

.primary:hover {
  transform: translateY(-1px);
}

.ghost {
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #f8fafc;
  padding: 10px 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.ghost:hover {
  background: rgba(255, 255, 255, 0.25);
}

.link {
  padding: 10px 12px;
  border-radius: 10px;
  background: transparent;
  border: 1px solid rgba(148, 163, 184, 0.6);
  color: inherit;
  cursor: pointer;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.stat-card {
  background: #0f172a;
  color: #e2e8f0;
  border: 1px solid #1e293b;
  border-radius: 14px;
  padding: 12px;
  box-shadow: 0 14px 40px rgba(15, 23, 42, 0.35);
}

.stat-card h3 {
  margin: 4px 0;
  font-size: 22px;
}

.stat-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.chip {
  padding: 6px 10px;
  border-radius: 999px;
  font-weight: 700;
  font-size: 13px;
}

.chip.positive {
  background: rgba(34, 197, 94, 0.16);
  color: #4ade80;
}

.chip.warning {
  background: rgba(250, 204, 21, 0.16);
  color: #facc15;
}

.chip.neutral {
  background: rgba(226, 232, 240, 0.16);
  color: #e2e8f0;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 12px;
}

.action-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 14px;
  box-shadow: 0 10px 30px rgba(148, 163, 184, 0.15);
  position: relative;
}

.action-header {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
}

.action-header h3 {
  margin: 0;
  color: #0f172a;
}

.action-header .muted {
  margin: 4px 0 0;
}

.action-accent {
  width: 36px;
  height: 36px;
  background: linear-gradient(145deg, #0ea5e9, #22c55e);
  border-radius: 12px;
}

.action-buttons {
  position: absolute;
  left: 14px;
  right: 14px;
  top: 84px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 16px 30px rgba(15, 23, 42, 0.12);
  padding: 8px;
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
  max-height: 0;
  opacity: 0;
  overflow: hidden;
  transform: translateY(-6px);
  transition: max-height 0.2s ease, opacity 0.2s ease, transform 0.2s ease;
  pointer-events: none;
}

.action-buttons button {
  padding: 10px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: #fff;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.action-buttons button:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

.action-card:hover .action-buttons,
.action-card:focus-within .action-buttons {
  max-height: 420px;
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}

.action-label span {
  font-weight: 700;
  color: #0f172a;
}

.arrow {
  color: #94a3b8;
  font-weight: 700;
}

.action-card:hover {
  z-index: 5;
}

.workspace {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 12px;
}

.workbench {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  box-shadow: 0 14px 40px rgba(148, 163, 184, 0.16);
  overflow: hidden;
}

.workbench-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px;
  border-bottom: 1px solid #e2e8f0;
}

.workbench-body {
  padding: 14px;
}

.sidebar {
  min-width: 320px;
}

.sidebar-card {
  background: #0f172a;
  color: #e2e8f0;
  border: 1px solid #1e293b;
  border-radius: 14px;
  padding: 12px;
  box-shadow: 0 14px 40px rgba(15, 23, 42, 0.35);
}

.sidebar-header h4 {
  margin: 2px 0;
}

.placeholder {
  color: #64748b;
  font-size: 14px;
  padding: 10px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
}

.eyebrow {
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
}

.muted {
  margin: 2px 0 0;
  color: #475569;
  font-size: 14px;
}

.pill-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 8px 0;
}

.pill {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.16);
  color: inherit;
  font-size: 13px;
}

.pill-strong {
  background: #0f172a;
  color: #e2e8f0;
}

.mono {
  font-family: 'JetBrains Mono', 'SFMono-Regular', Consolas, monospace;
  letter-spacing: 0.02em;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.65);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  z-index: 2000;
}

.modal-shell {
  width: min(1100px, 95vw);
  max-height: 90vh;
  overflow: auto;
}

@media (max-width: 960px) {
  .workspace {
    grid-template-columns: 1fr;
  }

  .sidebar {
    min-width: auto;
  }

  .modal-shell {
    width: 100%;
  }
}
</style>
