<script setup lang="ts">
import { reactive } from 'vue'
import AuthPanel from './components/AuthPanel.vue'
import AccountTree from './components/AccountTree.vue'

const session = reactive({
  userId: localStorage.getItem('user_id') ?? '',
  bookGuid: localStorage.getItem('book_guid') ?? '',
  username: ''
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
</style>
