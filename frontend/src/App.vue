<script setup lang="ts">
import { reactive } from 'vue'
import AuthPanel from './components/AuthPanel.vue'
import AccountTree from './components/AccountTree.vue'
import PurchasePanel from './components/PurchasePanel.vue'
import SupplierPanel from './components/SupplierPanel.vue'
import CustomerPanel from './components/CustomerPanel.vue'
import PurchaseOrderPanel from './components/PurchaseOrderPanel.vue'
import SalesInvoicePanel from './components/SalesInvoicePanel.vue'
import SalesPanel from './components/SalesPanel.vue'
import ReconcilePanel from './components/ReconcilePanel.vue'
import EmployeeExpensePanel from './components/EmployeeExpensePanel.vue'
import ReportPanel from './components/ReportPanel.vue'

const session = reactive({
  userId: localStorage.getItem('user_id') ?? '',
  bookGuid: localStorage.getItem('book_guid') ?? '',
  username: ''
})
const selection = reactive({
  module: '',
  action: ''
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

const selectAction = (module: string, action: string) => {
  selection.module = module
  selection.action = action
}
</script>

<template>
  <main class="page">
    <section v-if="!session.bookGuid" class="panel">
      <AuthPanel @success="handleAuthSuccess" />
    </section>

    <section v-else class="layout">
      <header class="topbar">
        <div>
          <h1>会计科目表</h1>
          <p class="subtitle">账本 GUID：{{ session.bookGuid }}</p>
        </div>
        <div class="user">
          <span>用户 ID：{{ session.userId }}</span>
          <button class="link" type="button" @click="handleLogout">退出</button>
        </div>
      </header>

      <section class="actions">
        <div class="action-card">
          <div class="action-header">
            <h3>采购</h3>
            <p>供应商、订单、支付、过账</p>
          </div>
          <div class="action-buttons">
            <button type="button" @click="selectAction('采购', '新增供应商')">新增供应商</button>
            <button type="button" @click="selectAction('采购', '新增订单')">新增订单</button>
            <button type="button" @click="selectAction('采购', '过账')">过账</button>
            <button type="button" @click="selectAction('采购', '支付')">支付</button>
          </div>
        </div>
        <div class="action-card">
          <div class="action-header">
            <h3>销售</h3>
            <p>客户、销售单/发票、过账、收款</p>
          </div>
          <div class="action-buttons">
            <button type="button" @click="selectAction('销售', '新增发票')">新增发票</button>
            <button type="button" @click="selectAction('销售', '新增客户')">新增客户</button>
            <button type="button" @click="selectAction('销售', '过账')">过账</button>
            <button type="button" @click="selectAction('销售', '收款')">收款</button>
          </div>
        </div>
        <div class="action-card">
          <div class="action-header">
            <h3>员工费用</h3>
            <p>档案/项目、报销审批、应付支付</p>
          </div>
          <div class="action-buttons">
            <button type="button" @click="selectAction('员工费用', '员工档案')">员工档案</button>
            <button type="button" @click="selectAction('员工费用', '报销/差旅')">报销/差旅</button>
            <button type="button" @click="selectAction('员工费用', '支付')">支付</button>
          </div>
        </div>
        <div class="action-card">
          <div class="action-header">
            <h3>报表</h3>
            <p>利润表、资产负债表、现金流量表</p>
          </div>
          <div class="action-buttons">
            <button type="button" @click="selectAction('报表', '利润表')">利润表</button>
            <button type="button" @click="selectAction('报表', '资产负债表')">资产负债表</button>
            <button type="button" @click="selectAction('报表', '现金流量表')">现金流量表</button>
          </div>
        </div>
      </section>

      <section v-if="selection.module" class="workbench">
        <div class="workbench-header">
          <div>
            <h3>{{ selection.module }} - {{ selection.action }}</h3>
            <p class="subtitle">当前账本：{{ session.bookGuid }}</p>
          </div>
          <button class="link" type="button" @click="selection.module = selection.action = ''">关闭</button>
        </div>
        <PurchasePanel v-if="selection.module === '采购' && selection.action === '过账'" :book-guid="session.bookGuid" mode="post" />
        <PurchasePanel v-else-if="selection.module === '采购' && selection.action === '支付'" :book-guid="session.bookGuid" mode="pay" />
        <SupplierPanel v-else-if="selection.module === '采购' && selection.action === '新增供应商'" :book-guid="session.bookGuid" />
        <PurchaseOrderPanel v-else-if="selection.module === '采购' && selection.action === '新增订单'" :book-guid="session.bookGuid" />
        <SalesInvoicePanel v-else-if="selection.module === '销售' && selection.action === '新增发票'" :book-guid="session.bookGuid" />
        <CustomerPanel v-else-if="selection.module === '销售' && selection.action === '新增客户'" :book-guid="session.bookGuid" />
        <SalesPanel v-else-if="selection.module === '销售' && selection.action === '过账'" :book-guid="session.bookGuid" mode="invoice" />
        <SalesPanel v-else-if="selection.module === '销售' && selection.action === '收款'" :book-guid="session.bookGuid" mode="receipt" />
        <EmployeeExpensePanel
          v-else-if="selection.module === '员工费用'"
          :book-guid="session.bookGuid"
          :action="selection.action"
        />
        <ReportPanel v-else-if="selection.module === '报表' && selection.action === '利润表'" :book-guid="session.bookGuid" type="pl" />
        <ReportPanel v-else-if="selection.module === '报表' && selection.action === '资产负债表'" :book-guid="session.bookGuid" type="bs" />
        <ReportPanel v-else-if="selection.module === '报表' && selection.action === '现金流量表'" :book-guid="session.bookGuid" type="cf" />
        <div v-else class="placeholder">
          <p>功能即将接入：{{ selection.module }} - {{ selection.action }}</p>
        </div>
      </section>

      <AccountTree :book-guid="session.bookGuid" />
    </section>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: radial-gradient(circle at 20% 20%, #e0f2fe, transparent 30%),
    radial-gradient(circle at 80% 0%, #dcfce7, transparent 32%),
    linear-gradient(145deg, #eef2ff, #fff);
  padding: 32px 16px;
}

.panel {
  max-width: 720px;
  margin: 0 auto;
}

.layout {
  max-width: 1080px;
  margin: 0 auto;
  background: #ffffff;
  border-radius: 18px;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.12);
  padding: 20px 20px 24px;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 12px;
}

.topbar h1 {
  margin: 0;
  font-size: 24px;
  color: #0f172a;
}

.subtitle {
  margin: 4px 0 0;
  color: #475569;
  font-size: 13px;
}

.user {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #0f172a;
}

.link {
  padding: 8px 12px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  cursor: pointer;
  transition: all 0.2s ease;
}

.link:hover {
  background: #e2e8f0;
}

.actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
  margin: 16px 0;
}

.action-card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
  background: #f8fafc;
}

.action-header h3 {
  margin: 0;
  color: #0f172a;
}

.action-header p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.action-buttons {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.action-buttons button {
  padding: 10px;
  border-radius: 10px;
  border: 1px solid #cbd5e1;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-buttons button:hover {
  background: #e2e8f0;
}

.workbench {
  margin: 16px 0;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
}

.workbench-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 8px;
  margin-bottom: 8px;
}

.placeholder {
  color: #64748b;
  font-size: 14px;
}
</style>
