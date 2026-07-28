<!-- 支付详情页面：展示单笔支付的基础信息、失败错误详情（如有）以及完整状态变更历史时间线 -->
<template>
  <div class="detail-page" v-loading="loading">
    <!-- 页头：带返回按钮，点击返回列表页 -->
    <el-page-header class="page-header" @back="router.push('/')">
      <template #content>
        <span class="page-title">{{ t('detail.title') }}</span>
      </template>
    </el-page-header>

    <template v-if="payment">
      <!-- 基础信息卡片：字段命名与后端 PaymentResponse 保持一致 -->
      <el-card class="info-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span class="card-title">{{ t('detail.basicInfo') }}</span>
            <el-tag :type="statusTagType(payment.status)" effect="dark">{{ payment.status }}</el-tag>
          </div>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item :label="t('detail.paymentId')">{{ payment.id }}</el-descriptions-item>
          <el-descriptions-item :label="t('detail.idempotencyKey')">{{ payment.idempotencyKey }}</el-descriptions-item>
          <el-descriptions-item :label="t('detail.fromAccount')">{{ payment.fromAccount }}</el-descriptions-item>
          <el-descriptions-item :label="t('detail.toAccount')">{{ payment.toAccount }}</el-descriptions-item>
          <el-descriptions-item :label="t('detail.amount')">{{ formatAmount(payment.amount, payment.currency) }}</el-descriptions-item>
          <el-descriptions-item :label="t('detail.currency')">{{ payment.currency }}</el-descriptions-item>
          <el-descriptions-item :label="t('detail.remark')" :span="2">{{ payment.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('detail.createdAt')">{{ formatDateTime(payment.createdAt) }}</el-descriptions-item>
          <el-descriptions-item :label="t('detail.updatedAt')">{{ formatDateTime(payment.updatedAt) }}</el-descriptions-item>
        </el-descriptions>

        <!-- 失败错误详情：仅当状态为 FAILED 时展示，符合课程需求“查看失败支付错误详情” -->
        <el-alert
          v-if="payment.status === 'FAILED'"
          type="error"
          class="error-alert"
          :title="`${t('detail.errorCode')}: ${payment.errorCode || t('detail.unknown')}`"
          :description="errorDescription(payment.errorCode, payment.errorMessage)"
          show-icon
          :closable="false"
        />
      </el-card>

      <!-- 状态变更历史时间线（audit trail） -->
      <el-card class="history-card" shadow="never">
        <template #header>
          <span class="card-title">{{ t('detail.statusHistory') }}</span>
        </template>

        <el-timeline>
          <el-timeline-item
            v-for="item in history"
            :key="item.id"
            :type="statusTimelineType(item.toStatus)"
            :timestamp="formatDateTime(item.createdAt)"
            placement="top"
          >
            <div class="timeline-title">
              <strong>{{ item.fromStatus || t('detail.start') }} → {{ item.toStatus }}</strong>
              <el-tag size="small" type="info" effect="plain" class="operator-tag">{{ item.operator }}</el-tag>
            </div>
            <div v-if="item.errorCode" class="history-error">
              {{ item.errorCode }}: {{ errorDescription(item.errorCode, item.errorMessage) }}
            </div>
            <div v-if="item.remark" class="history-remark">{{ item.remark }}</div>
          </el-timeline-item>
        </el-timeline>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { getPaymentById, getPaymentHistory } from '../api/payment';

// 路由配置中该页面使用 props: true，因此 id 直接作为组件 prop 传入，无需再手动解析 useRoute().params
const props = defineProps({
  id: {
    type: [String, Number],
    required: true
  }
});

const router = useRouter();
const { t, te, n, d } = useI18n();

const payment = ref(null);
const history = ref([]);
const loading = ref(false);

// 按支付自己的币种格式化金额，与列表页 formatAmount 逻辑保持一致
function formatAmount(amount, currency) {
  return n(amount, { key: 'currency', currency });
}

// 按当前界面语言的习惯格式化日期时间
function formatDateTime(value) {
  return value ? d(new Date(value), 'short') : '-';
}

/**
 * 错误描述优先使用前端本地化字典（locales/*.js 中的 errors.···），保证同一错误码在三种界面语言下
 * 都能展示对应语言的描述，而不受后端固定语言 message 的影响。
 * 若错误码不在前端字典内（未收录的新错误码），回退展示后端返回的 errorMessage，再无则显示兼底文案。
 */
function errorDescription(code, fallbackMessage) {
  const key = `errors.${code}`;
  if (code && te(key)) {
    return t(key);
  }
  return fallbackMessage || t('detail.noErrorMessage');
}

/** 状态到标签类型的映射：与列表页保持一致，避免同一状态在不同页面呈现不同颜色 */
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

/** 时间线节点颜色：复用与状态标签一致的语义色，FAILED 用 danger，COMPLETED 用 success */
function statusTimelineType(status) {
  const map = {
    COMPLETED: 'success',
    FAILED: 'danger',
    SENT: 'warning'
  };
  return map[status] || 'primary';
}

/** 并发加载支付详情与状态历史，减少等待时间 */
async function fetchDetail() {
  loading.value = true;
  try {
    const [detailRes, historyRes] = await Promise.all([
      getPaymentById(props.id),
      getPaymentHistory(props.id)
    ]);
    payment.value = detailRes.data;
    history.value = historyRes.data;
  } catch (error) {
    // 错误提示（含 PAYMENT_NOT_FOUND 等）已由 http.js 拦截器统一处理
  } finally {
    loading.value = false;
  }
}

onMounted(fetchDetail);
</script>

<style scoped>
.detail-page {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  background: #ffffff;
  border-radius: 12px;
  padding: 12px 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.page-title {
  font-size: 16px;
  font-weight: 600;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.error-alert {
  margin-top: 16px;
}

.timeline-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.operator-tag {
  font-weight: normal;
}

.history-error {
  color: #dc2626;
  font-size: 13px;
  margin-top: 4px;
}

.history-remark {
  color: #6b7280;
  font-size: 13px;
  margin-top: 2px;
}
</style>

