<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

const message = ref('')
const apiBase = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'
const props = defineProps<{ bookGuid: string; mode?: 'create' | 'list' }>()
const loading = ref(false)
const form = reactive({
  name: '',
  taxId: '',
  email: '',
  phone: '',
  addr: '',
  notes: ''
})
const customers = ref<{ guid: string; name: string; status?: string; note?: string }[]>([])
const panelMode = computed(() => props.mode ?? 'create')

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
    await loadCustomers()
    window.dispatchEvent(new Event('customers-updated'))
  } catch (e) {
    message.value = e instanceof Error ? e.message : '创建失败'
  } finally {
    loading.value = false
  }
}

const loadCustomers = async () => {
  if (!props.bookGuid) return
  try {
    const res = await fetch(`${apiBase}/api/business/customers/detail?bookGuid=${props.bookGuid}`)
    const data = await res.json()
    if (!res.ok || !data.success) throw new Error()
    customers.value = data.data || []
  } catch {
    customers.value = []
  }
}

onMounted(loadCustomers)
watch(
  () => props.bookGuid,
  (v) => v && loadCustomers()
)
</script>

<template>
  <div class="panel">
    <h3 v-if="panelMode === 'create'">新增客户</h3>
    <h3 v-else>客户列表</h3>
    <div v-if="panelMode === 'create'" class="form">
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
    <div v-else class="form">
      <p class="muted">仅查看客户列表</p>
    </div>
    <p v-if="message" class="message">{{ message }}</p>
    <div class="list" v-if="customers.length">
      <p class="title">已有客户：</p>
      <ul>
        <li v-for="c in customers" :key="c.guid">
          <strong>{{ c.name }}</strong>
          <span class="muted">ID: {{ c.guid }}</span>
          <span class="muted" v-if="c.note">备注: {{ c.note }}</span>
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
.list {
  margin-top: 10px;
  color: #334155;
  font-size: 14px;
}
.muted {
  color: #64748b;
  margin-left: 6px;
}
.title {
  margin: 0 0 4px;
}
</style>
