<script setup lang="ts">
import { computed, reactive, ref } from 'vue'

type Mode = 'login' | 'register'

const mode = ref<Mode>('login')
const loading = ref(false)
const message = ref('')
const apiBase = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'

const form = reactive({
  username: '',
  password: '',
  bookName: '',
  bookSize: '',
  registeredCapitalNum: null as number | null,
  registeredCapitalDenom: 100 as number | null,
  fiscalYearStartMonth: null as number | null,
  fiscalYearStartDay: null as number | null
})

const storedSession = reactive({
  userId: localStorage.getItem('user_id') ?? '',
  bookGuid: localStorage.getItem('book_guid') ?? ''
})

const switchMode = (next: Mode) => {
  mode.value = next
  message.value = ''
}

const titleText = computed(() => (mode.value === 'login' ? '登录到账本' : '注册新账本'))
const actionText = computed(() => (mode.value === 'login' ? '登录' : '注册并创建账本'))

const buildPayload = () => {
  if (mode.value === 'login') {
    return {
      username: form.username,
      password: form.password
    }
  }
  return {
    username: form.username,
    password: form.password,
    bookName: form.bookName,
    bookSize: form.bookSize || null,
    registeredCapitalNum: form.registeredCapitalNum,
    registeredCapitalDenom: form.registeredCapitalDenom,
    fiscalYearStartMonth: form.fiscalYearStartMonth,
    fiscalYearStartDay: form.fiscalYearStartDay
  }
}

const submit = async () => {
  loading.value = true
  message.value = ''
  try {
    const response = await fetch(`${apiBase}/api/auth/${mode.value}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(buildPayload())
    })

    const data = await response.json()
    if (!response.ok || !data.success) {
      throw new Error(data.message || '请求失败')
    }

    const resp = data.data as { id: number; username: string; bookGuid: string }
    storedSession.userId = String(resp.id ?? '')
    storedSession.bookGuid = resp.bookGuid ?? ''
    localStorage.setItem('user_id', storedSession.userId)
    localStorage.setItem('book_guid', storedSession.bookGuid)
    message.value = mode.value === 'login' ? '登录成功' : '注册成功，已为你创建默认账本'
  } catch (error) {
    message.value = error instanceof Error ? error.message : '请求失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="panel">
    <h1>{{ titleText }}</h1>
    <p class="subtitle">简易财务练手项目，注册时将自动初始化你的企业账本。</p>

    <div class="mode-toggle">
      <button :class="{ active: mode === 'login' }" type="button" @click="switchMode('login')">登录</button>
      <button :class="{ active: mode === 'register' }" type="button" @click="switchMode('register')">注册</button>
    </div>

    <form class="form" @submit.prevent="submit">
      <label class="field">
        <span>用户名</span>
        <input v-model.trim="form.username" type="text" name="username" placeholder="请输入用户名" required minlength="3" />
      </label>
      <label class="field">
        <span>密码</span>
        <input
          v-model.trim="form.password"
          type="password"
          name="password"
          placeholder="至少 6 位密码"
          minlength="6"
          required
        />
      </label>

      <template v-if="mode === 'register'">
        <label class="field">
          <span>账本名称（企业名称）</span>
          <input v-model.trim="form.bookName" type="text" name="bookName" placeholder="如：月光科技有限公司" required />
        </label>

        <label class="field">
          <span>企业规模（可选）</span>
          <input v-model.trim="form.bookSize" type="text" name="bookSize" placeholder="如：50人以下" />
        </label>

        <div class="inline">
          <label class="field">
            <span>注册资本（分子，可选）</span>
            <input
              v-model.number="form.registeredCapitalNum"
              type="number"
              min="0"
              step="1"
              name="registeredCapitalNum"
              placeholder="单位：分子，示例 1000000"
            />
          </label>
          <label class="field">
            <span>注册资本（分母，可选）</span>
            <input
              v-model.number="form.registeredCapitalDenom"
              type="number"
              min="1"
              step="1"
              name="registeredCapitalDenom"
              placeholder="默认 100"
            />
          </label>
        </div>

        <div class="inline">
          <label class="field">
            <span>会计年度起始月（1-12，可选）</span>
            <input
              v-model.number="form.fiscalYearStartMonth"
              type="number"
              min="1"
              max="12"
              step="1"
              name="fiscalYearStartMonth"
              placeholder="示例：1"
            />
          </label>
          <label class="field">
            <span>会计年度起始日（1-31，可选）</span>
            <input
              v-model.number="form.fiscalYearStartDay"
              type="number"
              min="1"
              max="31"
              step="1"
              name="fiscalYearStartDay"
              placeholder="示例：1"
            />
          </label>
        </div>
      </template>

      <button class="submit" type="submit" :disabled="loading">
        {{ loading ? '处理中...' : actionText }}
      </button>
    </form>

    <p v-if="message" class="message">{{ message }}</p>
    <div v-if="storedSession.userId || storedSession.bookGuid" class="session">
      <p><strong>当前用户 ID：</strong>{{ storedSession.userId }}</p>
      <p><strong>账本 GUID：</strong>{{ storedSession.bookGuid }}</p>
    </div>
  </div>
</template>

<style scoped>
.panel {
  max-width: 560px;
  margin: 0 auto;
  padding: 32px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.12);
}

h1 {
  margin: 0;
  font-size: 26px;
  color: #0f172a;
}

.subtitle {
  margin: 8px 0 24px;
  color: #475569;
}

.mode-toggle {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 16px;
}

.mode-toggle button {
  padding: 12px;
  border-radius: 10px;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  cursor: pointer;
  transition: all 0.2s ease;
}

.mode-toggle button.active {
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 6px 20px rgba(34, 197, 94, 0.35);
}

.form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #0f172a;
  font-weight: 600;
}

.inline {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

input {
  padding: 12px;
  border-radius: 10px;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

input:focus {
  border-color: #16a34a;
  box-shadow: 0 0 0 3px rgba(22, 163, 74, 0.2);
}

.submit {
  padding: 12px;
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: #fff;
  border: none;
  border-radius: 10px;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.message {
  margin-top: 14px;
  color: #0ea5e9;
  font-weight: 600;
}

.session {
  margin-top: 10px;
  color: #334155;
  font-size: 14px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  padding: 10px 12px;
}
</style>
