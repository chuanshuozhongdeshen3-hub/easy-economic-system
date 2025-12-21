<script setup lang="ts">
import { reactive, ref } from 'vue'

const message = ref('')
const apiBase = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'
const props = defineProps<{ bookGuid: string }>()
const loading = ref(false)
const form = reactive({
  name: '',
  taxId: '',
  email: '',
  phone: '',
  addr: '',
  notes: ''
})

const submit = async () => {
  if (!props.bookGuid) {
    message.value = '缺少账本'
    return
  }
  loading.value = true
  message.value = ''
  try {
    const res = await fetch(`${apiBase}/api/business/customers`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bookGuid: props.bookGuid,
        name: form.name,
        id: null,
        notes: form.notes || null,
        taxId: form.taxId || null,
        email: form.email || null,
        phone: form.phone || null,
        addr: form.addr || null
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error(data.message || '创建失败')
    message.value = '客户创建成功'
  } catch (e) {
    message.value = e instanceof Error ? e.message : '创建失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="panel">
    <h3>新增客户</h3>
    <div class="form">
      <label class="field">
        <span>名称</span>
        <input v-model.trim="form.name" type="text" placeholder="客户名称" />
      </label>
      <label class="field">
        <span>税号</span>
        <input v-model.trim="form.taxId" type="text" placeholder="纳税人识别号" />
      </label>
      <label class="field">
    <span>邮箱</span>
    <input v-model.trim="form.email" type="email" placeholder="email@example.com" />
  </label>
  <label class="field">
    <span>电话</span>
    <input v-model.trim="form.phone" type="text" placeholder="联系电话" />
  </label>
  <label class="field">
    <span>地址</span>
    <input v-model.trim="form.addr" type="text" placeholder="地址" />
  </label>
  <label class="field">
    <span>备注</span>
    <input v-model.trim="form.notes" type="text" placeholder="备注" />
  </label>
      <button type="button" @click="submit" :disabled="loading">保存</button>
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
  border: 1px solid #cbd5e1;
  border-radius: 8px;
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
