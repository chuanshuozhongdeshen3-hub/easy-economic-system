<script setup lang="ts">
import { reactive, ref } from 'vue'

type Mode = 'login' | 'register'

const mode = ref<Mode>('login')
const loading = ref(false)
const message = ref('')

const form = reactive({
  username: '',
  password: ''
})

const switchMode = (next: Mode) => {
  mode.value = next
  message.value = ''
}

const submit = async () => {
  loading.value = true
  message.value = ''
  try {
    const response = await fetch(`http://localhost:8080/api/auth/${mode.value}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(form)
    })

    const data = await response.json()
    if (!response.ok || !data.success) {
      throw new Error(data.message || '请求失败')
    }

    message.value = `${mode.value === 'login' ? '登录' : '注册'}成功，欢迎 ${data.data.username}`
  } catch (error) {
    message.value = error instanceof Error ? error.message : '请求失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="panel">
    <h1>简单财务管理系统</h1>
    <p class="subtitle">先完成登录和注册，确保每个账号拥有自己的账本空间。</p>

    <div class="mode-toggle">
      <button :class="{ active: mode === 'login' }" @click="switchMode('login')">登录</button>
      <button :class="{ active: mode === 'register' }" @click="switchMode('register')">注册</button>
    </div>

    <form class="form" @submit.prevent="submit">
      <label>
        用户名
        <input v-model.trim="form.username" type="text" name="username" required minlength="3" />
      </label>
      <label>
        密码
        <input v-model.trim="form.password" type="password" name="password" required minlength="6" />
      </label>
      <button class="submit" type="submit" :disabled="loading">
        {{ loading ? '处理中...' : mode === 'login' ? '登录' : '注册' }}
      </button>
    </form>

    <p v-if="message" class="message">{{ message }}</p>
    <p class="tip">后台接口：<code>http://localhost:8080/api/auth</code></p>
  </div>
</template>

<style scoped>
.panel {
  max-width: 480px;
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

label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #0f172a;
  font-weight: 600;
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

.tip {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}

code {
  background: #f1f5f9;
  padding: 4px 6px;
  border-radius: 6px;
}
</style>
