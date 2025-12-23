<script setup lang="ts">
import { defineComponent, h, onMounted, reactive, ref, watch } from 'vue'

type AccountNode = {
  guid: string
  name: string
  code?: string
  accountType: string
  description?: string
  balance: number
  children: AccountNode[]
  expanded?: boolean
}

const props = defineProps<{
  bookGuid: string
}>()

const apiBase = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'
const loading = ref(false)
const message = ref('')
const tree = ref<AccountNode[]>([])

const createForm = reactive({
  parentGuid: '',
  accountType: '',
  name: '',
  code: '',
  description: ''
})
const editing = reactive({
  guid: '',
  name: '',
  code: '',
  description: ''
})
const related = reactive({
  accountGuid: '',
  accountName: '',
  loading: false,
  message: '',
  items: [] as { docType: string; docId: string; docDate: string; description: string }[]
})

const loadTree = async () => {
  loading.value = true
  message.value = ''
  try {
    const res = await fetch(`${apiBase}/api/accounts/tree?bookGuid=${props.bookGuid}`)
    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || '加载失败')
    }
    const nodes: AccountNode[] = data.data.map((n: any) => ({
      ...n,
      balance: Number(n.balance ?? 0),
      expanded: false,
      children: markExpanded(n.children || [])
    }))
    tree.value = nodes
  } catch (error) {
    message.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    loading.value = false
  }
}

const markExpanded = (children: AccountNode[] | any[]): AccountNode[] =>
  children.map((c: any) => ({
    ...c,
    balance: Number(c.balance ?? 0),
    expanded: false,
    children: markExpanded(c.children || [])
  }))

const toggle = (node: AccountNode) => {
  node.expanded = !node.expanded
}

const openCreate = (node: AccountNode) => {
  createForm.parentGuid = node.guid
  createForm.accountType = node.accountType
  createForm.name = ''
  createForm.code = ''
  createForm.description = ''
}

const submitCreate = async () => {
  if (!createForm.parentGuid) return
  loading.value = true
  message.value = ''
  try {
    const res = await fetch(`${apiBase}/api/accounts`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bookGuid: props.bookGuid,
        parentGuid: createForm.parentGuid,
        name: createForm.name,
        code: createForm.code || null,
        accountType: createForm.accountType,
        description: createForm.description || null
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || '创建失败')
    }
    await loadTree()
    createForm.parentGuid = ''
    message.value = '创建成功'
  } catch (error) {
    message.value = error instanceof Error ? error.message : '创建失败'
  } finally {
    loading.value = false
  }
}

const openEdit = (node: AccountNode) => {
  editing.guid = node.guid
  editing.name = node.name
  editing.code = node.code || ''
  editing.description = node.description || ''
}

const submitEdit = async () => {
  if (!editing.guid) return
  loading.value = true
  message.value = ''
  try {
    const res = await fetch(`${apiBase}/api/accounts/${editing.guid}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: editing.name,
        code: editing.code || null,
        description: editing.description || null
      })
    })
    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || '更新失败')
    }
    await loadTree()
    editing.guid = ''
    message.value = '更新成功'
  } catch (error) {
    message.value = error instanceof Error ? error.message : '更新失败'
  } finally {
    loading.value = false
  }
}

const remove = async (node: AccountNode) => {
  if (!node.guid) return
  if (!window.confirm(`确定删除科目「${node.name}」吗？该操作不可恢复。`)) return
  loading.value = true
  message.value = ''
  try {
    const res = await fetch(`${apiBase}/api/accounts/${node.guid}`, {
      method: 'DELETE'
    })
    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || '删除失败')
    }
    await loadTree()
    message.value = '删除成功'
  } catch (error) {
    message.value = error instanceof Error ? error.message : '删除失败'
  } finally {
    loading.value = false
  }
}

const loadRelated = async (node: AccountNode) => {
  if (!node.guid) return
  related.accountGuid = node.guid
  related.accountName = node.name
  related.loading = true
  related.message = ''
  related.items = []
  try {
    const res = await fetch(`${apiBase}/api/accounts/${node.guid}/related?bookGuid=${props.bookGuid}&includeChildren=true`)
    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || '查询失败')
    }
    related.items = (data.data || []).map((d: any) => ({
      docType: d.docType,
      docId: d.docId,
      docDate: d.docDate ? String(d.docDate).replace('T', ' ') : '',
      description: d.description || ''
    }))
  } catch (error) {
    related.message = error instanceof Error ? error.message : '查询失败'
  } finally {
    related.loading = false
  }
}

onMounted(loadTree)
watch(
  () => props.bookGuid,
  (val, oldVal) => {
    if (val && val !== oldVal) {
      loadTree()
    }
  }
)

const AccountNodeRow = defineComponent({
  name: 'AccountNodeRow',
  props: {
    node: {
      type: Object as () => AccountNode,
      required: true
    },
    level: {
      type: Number,
      default: 0
    }
  },
  emits: ['toggle', 'create', 'edit', 'remove', 'related'],
  setup(props, { emit }) {
    const format = (value: number) => {
      const num = Number(value ?? 0)
      const abs = Math.abs(num).toLocaleString('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      })
      return num < 0 ? `¥-${abs}` : `¥${abs}`
    }

    const onToggle = () => emit('toggle', props.node)
    const onCreate = () => emit('create', props.node)
    const onEdit = () => emit('edit', props.node)
    const onRemove = () => emit('remove', props.node)
    const onRelated = () => emit('related', props.node)

    const rowClass = ['row', props.level === 0 ? 'root-row' : 'child-row', `level-${Math.min(props.level, 3)}`]
    const indentPx = `${props.level * 16}px`

    return () =>
      h('div', {}, [
        h('div', { class: rowClass }, [
          h('div', { class: 'row-head' }, [
            h('div', { class: 'left', style: { marginLeft: indentPx } }, [
              props.node.children.length
                ? h(
                    'button',
                    { class: 'toggle', onClick: onToggle, 'aria-label': '展开/收起' },
                    props.node.expanded ? 'v' : '>'
                  )
                : h('span', { class: 'toggle placeholder' }, '.'),
              h('span', { class: 'icon' }, props.level === 0 ? '*' : '+'),
              h('span', { class: 'name', title: props.node.description || '' }, props.node.name)
            ]),
            h(
              'span',
              { class: ['balance', props.node.balance < 0 ? 'negative' : 'positive'], style: { marginLeft: indentPx } },
              format(props.node.balance)
            )
          ]),
          h('div', { class: 'row-actions', style: { paddingLeft: `${props.level * 16 + 34}px` } }, [
            h('button', { class: 'ghost', onClick: onCreate }, '新增'),
            h('button', { class: 'ghost', onClick: onEdit }, '编辑'),
            h('button', { class: 'ghost', onClick: onRelated }, '查看'),
            !props.node.children.length ? h('button', { class: 'ghost danger', onClick: onRemove }, '删除') : null
          ])
        ]),
        props.node.children.length && props.node.expanded
          ? h(
              'div',
              { class: 'children' },
              props.node.children.map((child) =>
                h(AccountNodeRow, {
                  node: child,
                  level: props.level + 1,
                  onToggle: (n: AccountNode) => emit('toggle', n),
                  onCreate: (n: AccountNode) => emit('create', n),
                  onEdit: (n: AccountNode) => emit('edit', n),
                  onRemove: (n: AccountNode) => emit('remove', n),
                  onRelated: (n: AccountNode) => emit('related', n)
                })
              )
            )
          : null
      ])
  }
}) as any
</script>

<template>
  <div class="accounts">
    <div class="toolbar">
      <h2>科目列表</h2>
      <span class="muted">默认展开一级科目，点击可查看下级科目</span>
    </div>

    <p v-if="message" class="message">{{ message }}</p>

    <div v-if="loading">加载中...</div>
    <div v-else-if="!tree.length" class="empty">暂无科目数据</div>
    <div v-else class="tree">
      <AccountNodeRow
        v-for="node in tree"
        :key="node.guid"
        :node="node"
        :level="0"
        @toggle="toggle"
        @create="openCreate"
        @edit="openEdit"
        @remove="remove"
        @related="loadRelated"
      />
    </div>

    <div v-if="createForm.parentGuid" class="modal-mask" @click.self="createForm.parentGuid = ''">
      <div class="modal">
        <header class="modal-header">
          <h3>新增下级科目</h3>
          <button class="close" type="button" @click="createForm.parentGuid = ''">×</button>
        </header>
        <p class="muted">父级科目类型：{{ createForm.accountType }}</p>
        <form class="form" @submit.prevent="submitCreate">
          <label class="field">
            <span>科目名称</span>
            <input v-model.trim="createForm.name" type="text" required placeholder="请输入科目名称" />
          </label>
          <label class="field">
            <span>编码（可选）</span>
            <input v-model.trim="createForm.code" type="text" placeholder="例：1001" />
          </label>
          <label class="field">
            <span>描述（可选）</span>
            <textarea v-model.trim="createForm.description" rows="2" placeholder="备注信息" />
          </label>
          <div class="actions">
            <button type="submit">创建</button>
            <button type="button" class="ghost" @click="createForm.parentGuid = ''">取消</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="editing.guid" class="modal-mask" @click.self="editing.guid = ''">
      <div class="modal">
        <header class="modal-header">
          <h3>编辑科目</h3>
          <button class="close" type="button" @click="editing.guid = ''">×</button>
        </header>
        <form class="form" @submit.prevent="submitEdit">
          <label class="field">
            <span>科目名称</span>
            <input v-model.trim="editing.name" type="text" required />
          </label>
          <label class="field">
            <span>编码（可选）</span>
            <input v-model.trim="editing.code" type="text" />
          </label>
          <label class="field">
            <span>描述（可选）</span>
            <textarea v-model.trim="editing.description" rows="2" />
          </label>
          <div class="actions">
            <button type="submit">保存</button>
            <button type="button" class="ghost" @click="editing.guid = ''">取消</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="related.accountGuid" class="modal-mask" @click.self="related.accountGuid = ''">
      <div class="modal">
        <header class="modal-header">
          <h3>科目关联：{{ related.accountName }}</h3>
          <button class="close" type="button" @click="related.accountGuid = ''">×</button>
        </header>
        <p class="muted">展示引用该科目的订单 / 发票 / 交易</p>
        <p v-if="related.message" class="message">{{ related.message }}</p>
        <div v-if="related.loading">加载中...</div>
        <ul v-else class="related-list">
          <li v-for="item in related.items" :key="item.docType + item.docId + item.docDate">
            <span class="badge">{{ item.docType }}</span>
            <div class="info">
              <div class="line">
                <strong>{{ item.docId || '未命名' }}</strong>
                <span class="muted">{{ item.docDate }}</span>
              </div>
              <div class="muted small">{{ item.description || '无描述' }}</div>
            </div>
          </li>
          <li v-if="!related.items.length" class="muted">暂无关联记录</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style scoped>
.accounts {
  margin-top: 16px;
}

.toolbar {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.toolbar h2 {
  margin: 0;
  font-size: 20px;
  color: #0f172a;
}

.muted {
  color: #475569;
  font-size: 13px;
}

.message {
  margin: 10px 0;
  color: #0f172a;
}

.tree {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 8px 10px;
  background: #ffffff;
  color: #0f172a;
  box-shadow: inset 0 1px 0 rgba(0, 0, 0, 0.02);
}

.row {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px 4px;
  border-bottom: 1px solid #e2e8f0;
}

.row:last-child {
  border-bottom: none;
}

.root-row {
  font-size: 15px;
}

.child-row {
  font-size: 14px;
}

.row-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.toggle {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #0f172a;
  cursor: pointer;
}

.toggle.placeholder {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  background: transparent;
  border-color: transparent;
  color: #cbd5e1;
}

.icon {
  color: #475569;
}

.name {
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
}

.root-row .name {
  font-size: 16px;
}

.child-row .name {
  color: #1f2937;
}

.code {
  color: #475569;
  font-size: 12px;
  background: #e2e8f0;
  padding: 2px 6px;
  border-radius: 6px;
}

.desc {
  color: #64748b;
  font-size: 12px;
}

.balance {
  font-weight: 700;
  color: #0f172a;
  min-width: 160px;
  text-align: right;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.02em;
}

.balance.negative {
  color: #b91c1c;
}

.balance.positive {
  color: #0f172a;
}

.ops {
  display: flex;
  gap: 6px;
}

.row-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.ghost {
  padding: 6px 10px;
  border-radius: 8px;
  border: 1px solid #334155;
  background: #0f172a;
  color: #e2e8f0;
  cursor: pointer;
}

.ghost.danger {
  border-color: #f87171;
  color: #fca5a5;
}

.ghost:hover {
  border-color: #38bdf8;
}

.children {
  margin-left: 0;
  border-left: none;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #e2e8f0;
  font-weight: 600;
}

input,
textarea {
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #0f172a;
}

.actions {
  display: flex;
  gap: 10px;
}

button {
  cursor: pointer;
}

button.ghost {
  background: #0f172a;
}

.related-list {
  list-style: none;
  padding: 0;
  margin: 8px 0 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.related-list li {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.badge {
  background: #e0f2fe;
  color: #0f172a;
  border-radius: 6px;
  padding: 4px 6px;
  font-size: 12px;
  min-width: 68px;
  text-align: center;
}

.info .line {
  display: flex;
  gap: 8px;
  align-items: baseline;
}

.small {
  font-size: 12px;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  padding: 16px;
}

.modal {
  width: min(560px, 90vw);
  background: #0c1428;
  border: 1px solid #1f2937;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.4);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.close {
  border: 1px solid #334155;
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 8px;
  width: 32px;
  height: 32px;
  cursor: pointer;
}
</style>
