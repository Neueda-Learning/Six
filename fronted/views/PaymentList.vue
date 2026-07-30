<!-- 支付列表页面：支持按状态筛选、关键字搜索、分页展示，并点击行跳转详情页 -->
<template>
  <el-card class="list-page" shadow="never">
    <template #header>
      <div class="card-header">
        <span class="card-title">
          <el-icon :size="18" class="title-icon"><List /></el-icon>
          {{ t('list.title') }}
        </span>
        <div class="header-actions">
          <span v-if="isPolling" class="polling-hint"><i class="polling-dot"></i>{{ t('list.autoRefreshing') }}</span>
          <el-button type="primary" round :icon="CirclePlus" @click="router.push('/payments/create')">
            {{ t('list.newPayment') }}
          </el-button>
        </div>
      </div>
    </template>

    <!-- 筛选栏：状态下拉 + 关键字输入 + 查询/重置按钮，用浅色工具条包裹，与下方表格形成视觉分区 -->
    <div class="toolbar">
      <el-form :inline="true" class="filter-bar">
        <el-form-item :label="t('list.status')">
          <el-select v-model="query.status" :placeholder="t('list.allStatuses')" clearable style="width: 180px">
            <el-option
              v-for="item in statusOptions"
              :key="item"
              :label="paymentStatusLabel(item)"
              :value="item"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('list.keyword')">
          <!--
            关键字输入框较窄，长语言（如德语）的完整 placeholder 会被截断显示不全。
            用 el-tooltip 包裹输入框，在鼠标悬停/点击/聚焦时于输入框下方展示完整提示文案。
            trigger 传数组同时启用三种触发方式：hover（悬停）、click（点击）、focus（聚焦，输入过程中保持显示）。
          -->
          <el-tooltip
            :content="t('list.keywordPlaceholder')"
            placement="bottom-start"
            effect="dark"
            :trigger="['hover', 'click', 'focus']"
          >
            <el-input
              v-model="query.keyword"
              :placeholder="t('list.keywordPlaceholder')"
              clearable
              :prefix-icon="Search"
              style="width: 240px"
            />
          </el-tooltip>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" round :icon="Search" @click="handleSearch">{{ t('list.search') }}</el-button>
          <el-button round :icon="RefreshLeft" @click="handleReset">{{ t('list.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 支付列表表格：v-loading 绑定请求中状态，行点击跳转详情；header-cell-style 让表头文字统一居中 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      style="width: 100%"
      @row-click="handleRowClick"
      class="clickable-table"
      stripe
      border
      table-layout="auto"
      :allow-drag-last-column="true"
      :header-cell-style="{ textAlign: 'center' }"
    >
      <el-table-column type="index" :label="t('list.columns.index')" min-width="56" align="center" />
      <el-table-column prop="id" :label="t('list.columns.paymentId')" min-width="110" align="center" />
      <el-table-column prop="fromAccount" :label="t('list.columns.fromAccount')" min-width="130" align="center" />
      <el-table-column prop="toAccount" :label="t('list.columns.toAccount')" min-width="130" align="center" />
      <el-table-column :label="t('list.columns.amount')" min-width="150" align="center">
        <template #default="{ row }">
          <span class="amount-cell">{{ formatAmount(row.amount, row.currency) }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('list.columns.status')" min-width="130" align="center">
        <template #default="{ row }">
          <!-- 状态标签颜色映射：COMPLETED 绿色、FAILED 红色、SENT 黄色、CREATED/VALIDATED 灰蓝 -->
          <el-tag :type="statusTagType(row.status)" effect="light">{{ paymentStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" :label="t('list.columns.remark')" show-overflow-tooltip min-width="160" />
      <el-table-column :label="t('list.columns.createdAt')" min-width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('list.columns.actions')" min-width="140" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" @click.stop="handleMoveToTrash(row)">
            {{ t('list.moveToTrash') }}
          </el-button>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty :description="t('list.empty')" />
      </template>
    </el-table>

    <!-- 分页组件：页码/每页大小变化时重新请求列表 -->
    <el-pagination
      class="pagination"
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      :page-sizes="[10, 20, 50]"
      :total="total"
      background
      layout="total, sizes, prev, pager, next"
      @current-change="fetchList"
      @size-change="handleSizeChange"
    />
  </el-card>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { Search, RefreshLeft, CirclePlus, List } from '@element-plus/icons-vue';
import { listPayments, softDeletePayment } from '../api/payment';
import { formatDateTime } from '../utils/datetime';
import { getPaymentStatusLabel, PAYMENT_STATUS_OPTIONS } from '../utils/paymentStatus';

// 后端自动推进调度（PaymentAutoTransitionScheduler）每 5 秒推进一步，前端轮询间隔与之对齐，
// 保证列表页能在后端状态变化后尽快看到最新结果，而不需要手动点“查询”。
const POLLING_INTERVAL_MS = 5000;

const router = useRouter();
const route = useRoute();
const { t, n, te } = useI18n();

// 按支付自己的币种格式化金额：千分位分隔符/小数点符号随当前界面语言自动切换，
// 货币符号则个据每笔支付自己的 currency 字段决定（不受界面语言影响）。
function formatAmount(amount, currency) {
  return n(amount, { key: 'currency', currency });
}

// 状态下拉选项：与后端 PaymentStatus 枚举保持一致
const statusOptions = PAYMENT_STATUS_OPTIONS;

function paymentStatusLabel(status) {
  return getPaymentStatusLabel(status, t, te);
}

// 查询条件：page/size 与后端分页参数命名一致，status 为空表示不筛选。
// 初始值优先从当前路由 query 参数读取，这样从详情页返回列表页时（地址栏已带有之前的筛选条件）
// 能直接恢复上次的状态/关键字/页码，而不是重新回到第一页。
function parsePositiveInt(value, fallback) {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

const query = reactive({
  status: typeof route.query.status === 'string' ? route.query.status : '',
  keyword: typeof route.query.keyword === 'string' ? route.query.keyword : '',
  page: parsePositiveInt(route.query.page, 1),
  size: parsePositiveInt(route.query.size, 10)
});

const tableData = ref([]);
const total = ref(0);
const loading = ref(false);
const refreshing = ref(false);
const isPolling = ref(false);

let pollingTimer = null;

/** 非终态状态（仍可能被后端自动推进）列表，与后端 PaymentStateMachine 的终态定义保持一致 */
const PENDING_STATUSES = ['CREATED', 'VALIDATED', 'SENT'];

/** 只要当前页表格中还存在非终态支付，就需要继续轮询，直到它们都轮转到 COMPLETED/FAILED 为止 */
function shouldPoll(list) {
  return list.some((item) => PENDING_STATUSES.includes(item.status));
}

function stopPolling() {
  if (pollingTimer !== null) {
    clearInterval(pollingTimer);
    pollingTimer = null;
  }
  isPolling.value = false;
}

function syncPolling() {
  if (!shouldPoll(tableData.value)) {
    stopPolling();
    return;
  }

  if (pollingTimer !== null) {
    isPolling.value = true;
    return;
  }

  pollingTimer = setInterval(() => {
    fetchList({ silent: true });
  }, POLLING_INTERVAL_MS);
  isPolling.value = true;
}

/** 状态到 Element Plus 标签类型的映射，用于列表和详情页统一展示颜色 */
function statusTagType(status) {
  const map = {
    COMPLETED: 'success',
    FAILED: 'danger',
    SENT: 'warning',
    VALIDATED: 'info',
    CREATED: 'info'
  };
  return map[status] || 'info';
}

/**
 * 把当前筛选/分页状态同步到地址栏 query 参数（使用 replace 而非 push，避免每次查询都产生新的浏览器历史记录）。
 * 因为列表页自己的 URL 已经反映了当前筛选条件，从详情页点击“返回”（router.back()）时，
 * 浏览器会自然回到这个带有完整筛选条件的 URL，无需额外传递状态。
 * 默认值（空筛选、第 1 页、每页 10 条）不写入 query，保持地址栏干净。
 */
function syncQueryToRoute() {
  router
    .replace({
      query: {
        status: query.status || undefined,
        keyword: query.keyword || undefined,
        page: query.page !== 1 ? query.page : undefined,
        size: query.size !== 10 ? query.size : undefined
      }
    })
    .catch(() => {
      // 目标 URL 与当前相同时 vue-router 会报 NavigationDuplicated/冗余导航，忽略即可
    });
}

/**
 * 请求列表数据：将当前查询条件传给后端分页接口。
 * options.silent 为 true 时为轮询触发的静默刷新，不展示全表格 loading 遮罩，避免页面閃烁，
 * 也不需要同步 URL query（后台自动刷新不算用户主动发起的查询）。
 */
async function fetchList(options = {}) {
  const silent = options.silent === true;
  if (silent) {
    refreshing.value = true;
  } else {
    loading.value = true;
    syncQueryToRoute();
  }
  try {
    const res = await listPayments({
      status: query.status || undefined,
      keyword: query.keyword || undefined,
      page: query.page,
      size: query.size
    });
    // 后端分页数据结构：{ list, total, page, size }
    tableData.value = res.data.list;
    total.value = res.data.total;
    syncPolling();
  } catch (error) {
    // 错误提示已由 http.js 拦截器统一处理
  } finally {
    if (silent) {
      refreshing.value = false;
    } else {
      loading.value = false;
    }
  }
}

async function handleMoveToTrash(row) {
  try {
    await softDeletePayment(row.id);
    ElMessage.success(t('list.moveToTrashSuccess'));
    if (tableData.value.length === 1 && query.page > 1) {
      query.page -= 1;
    }
    fetchList();
  } catch (error) {
    // 错误提示已由 http.js 拦截器统一处理
  }
}

/** 点击“查询”按钮：重置为第一页后重新加载 */
function handleSearch() {
  query.page = 1;
  fetchList();
}

/** 点击“重置”按钮：清空筛选条件并重新加载 */
function handleReset() {
  query.status = '';
  query.keyword = '';
  query.page = 1;
  fetchList();
}

/** 每页条数变化时，回到第一页重新加载，避免出现空页 */
function handleSizeChange() {
  query.page = 1;
  fetchList();
}

/** 点击表格行：跳转到对应支付的详情页 */
function handleRowClick(row) {
  router.push(`/payments/${row.id}`);
}

// 页面挂载时执行首次查询
onMounted(fetchList);
// 页面卸载时清理轮询定时器，避免离开列表页后仍在后台悄悄发请求
onUnmounted(stopPolling);
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 500;
}

.title-icon {
  color: var(--color-primary);
  opacity: 0.88;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.polling-hint {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--color-primary-dark);
  background: var(--color-primary-light);
  font-size: 12px;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: 999px;
  white-space: nowrap;
}

.polling-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-primary);
  animation: polling-pulse 1.4s ease-in-out infinite;
}

@keyframes polling-pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(0.7);
  }
}

/* 工具条：把筛选表单包裹在浅色圆角容器里，与下方表格拉开视觉层次 */
.toolbar {
  background: #f8f9fc;
  border-radius: 12px;
  padding: 12px 16px 0;
  margin-top: 4px;
  margin-bottom: 16px;
}

.filter-bar {
  margin: 0;
}

.amount-cell {
  font-variant-numeric: tabular-nums;
}

.pagination {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f1f5;
  justify-content: flex-end;
}

.clickable-table :deep(.el-table__row) {
  cursor: pointer;
}
</style>

