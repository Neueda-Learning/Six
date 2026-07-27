<!-- 支付详情页面：展示单笔支付的基础信息、失败错误详情（如有）以及完整状态变更历史时间线 -->
<template>
  <section class="detail-page" v-loading="loading">
    <div class="header-row">
      <h2>Payment Detail</h2>
      <el-button @click="router.push('/')">Back to List</el-button>
    </div>

    <template v-if="payment">
      <!-- 基础信息卡片：字段命名与后端 PaymentResponse 保持一致 -->
      <el-descriptions :column="2" border class="info-card">
        <el-descriptions-item label="Payment ID">{{ payment.id }}</el-descriptions-item>
        <el-descriptions-item label="Status">
          <el-tag :type="statusTagType(payment.status)">{{ payment.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="From Account">{{ payment.fromAccount }}</el-descriptions-item>
        <el-descriptions-item label="To Account">{{ payment.toAccount }}</el-descriptions-item>
        <el-descriptions-item label="Amount">{{ payment.amount }}</el-descriptions-item>
        <el-descriptions-item label="Currency">{{ payment.currency }}</el-descriptions-item>
        <el-descriptions-item label="Idempotency Key">{{ payment.idempotencyKey }}</el-descriptions-item>
        <el-descriptions-item label="Remark">{{ payment.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Created At">{{ payment.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="Updated At">{{ payment.updatedAt }}</el-descriptions-item>
      </el-descriptions>

      <!-- 失败错误详情：仅当状态为 FAILED 时展示，符合课程需求“查看失败支付错误详情” -->
      <el-alert
        v-if="payment.status === 'FAILED'"
        type="error"
        class="error-alert"
        :title="`Error Code: ${payment.errorCode || 'UNKNOWN'}`"
        :description="payment.errorMessage || 'No error message provided'"
        show-icon
        :closable="false"
      />

      <!-- 状态变更历史时间线（audit trail） -->
      <h3 class="history-title">Status History</h3>
      <el-timeline>
        <el-timeline-item
          v-for="item in history"
          :key="item.id"
          :type="statusTimelineType(item.toStatus)"
          :timestamp="item.createdAt"
        >
          <div>
            <strong>{{ item.fromStatus || 'START' }} → {{ item.toStatus }}</strong>
            <span class="operator-tag">by {{ item.operator }}</span>
          </div>
          <div v-if="item.errorCode" class="history-error">
            {{ item.errorCode }}: {{ item.errorMessage }}
          </div>
          <div v-if="item.remark" class="history-remark">{{ item.remark }}</div>
        </el-timeline-item>
      </el-timeline>
    </template>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getPaymentById, getPaymentHistory } from '../api/payment';

// 路由配置中该页面使用 props: true，因此 id 直接作为组件 prop 传入，无需再手动解析 useRoute().params
const props = defineProps({
  id: {
    type: [String, Number],
    required: true
  }
});

const router = useRouter();

const payment = ref(null);
const history = ref([]);
const loading = ref(false);

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
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 24px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-card {
  margin-top: 16px;
}

.error-alert {
  margin-top: 16px;
}

.history-title {
  margin-top: 24px;
}

.operator-tag {
  margin-left: 8px;
  color: #6b7280;
  font-size: 12px;
}

.history-error {
  color: #dc2626;
  font-size: 13px;
}

.history-remark {
  color: #6b7280;
  font-size: 13px;
}
</style>

