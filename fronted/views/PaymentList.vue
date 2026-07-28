<!-- 支付列表页面：支持按状态筛选、关键字搜索、分页展示，并点击行跳转详情页 -->
<template>
  <el-card class="list-page" shadow="never">
    <template #header>
      <div class="card-header">
        <span class="card-title">{{ t('list.title') }}</span>
        <el-button type="primary" :icon="CirclePlus" @click="router.push('/payments/create')">
          {{ t('list.newPayment') }}
        </el-button>
      </div>
    </template>

    <!-- 筛选栏：状态下拉 + 关键字输入 + 查询/重置按钮 -->
    <el-form :inline="true" class="filter-bar">
      <el-form-item :label="t('list.status')">
        <el-select v-model="query.status" :placeholder="t('list.allStatuses')" clearable style="width: 180px">
          <el-option v-for="item in statusOptions" :key="item" :label="item" :value="item" />
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
        <el-button type="primary" :icon="Search" @click="handleSearch">{{ t('list.search') }}</el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">{{ t('list.reset') }}</el-button>
      </el-form-item>
    </el-form>

    <!-- 支付列表表格：v-loading 绑定请求中状态，行点击跳转详情 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      style="width: 100%"
      @row-click="handleRowClick"
      class="clickable-table"
      stripe
    >
      <el-table-column type="index" :label="t('list.columns.index')" width="56" />
      <el-table-column prop="id" :label="t('list.columns.paymentId')" width="110" />
      <el-table-column prop="fromAccount" :label="t('list.columns.fromAccount')" min-width="130" />
      <el-table-column prop="toAccount" :label="t('list.columns.toAccount')" min-width="130" />
      <el-table-column :label="t('list.columns.amount')" width="150" align="right">
        <template #default="{ row }">
          <span class="amount-cell">{{ row.amount }} {{ row.currency }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('list.columns.status')" width="130" align="center">
        <template #default="{ row }">
          <!-- 状态标签颜色映射：COMPLETED 绿色、FAILED 红色、SENT 黄色、CREATED/VALIDATED 灰蓝 -->
          <el-tag :type="statusTagType(row.status)" effect="light">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" :label="t('list.columns.remark')" show-overflow-tooltip min-width="160" />
      <el-table-column prop="createdAt" :label="t('list.columns.createdAt')" width="180" />
      <el-table-column :label="t('list.columns.actions')" width="140" align="center" fixed="right">
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
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { Search, RefreshLeft, CirclePlus } from '@element-plus/icons-vue';
import { listPayments, softDeletePayment } from '../api/payment';

const router = useRouter();
const { t } = useI18n();

// 状态下拉选项：与后端 PaymentStatus 枚举保持一致
const statusOptions = ['CREATED', 'VALIDATED', 'SENT', 'COMPLETED', 'FAILED'];

// 查询条件：page/size 与后端分页参数命名一致，status 为空表示不筛选
const query = reactive({
  status: '',
  keyword: '',
  page: 1,
  size: 10
});

const tableData = ref([]);
const total = ref(0);
const loading = ref(false);

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

/** 请求列表数据：将当前查询条件传给后端分页接口 */
async function fetchList() {
  loading.value = true;
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
  } catch (error) {
    // 错误提示已由 http.js 拦截器统一处理
  } finally {
    loading.value = false;
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
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.filter-bar {
  margin-top: 4px;
  margin-bottom: 8px;
}

.amount-cell {
  font-variant-numeric: tabular-nums;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.clickable-table :deep(.el-table__row) {
  cursor: pointer;
}
</style>

