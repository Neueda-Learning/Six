<template>
  <el-card class="trash-page" shadow="never">
    <template #header>
      <div class="card-header">
        <span class="card-title">{{ t('trash.title') }}</span>
      </div>
    </template>

    <el-alert :title="t('trash.retentionHint')" type="info" show-icon :closable="false" class="retention-alert" />

    <el-form :inline="true" class="filter-bar">
      <el-form-item :label="t('trash.keyword')">
        <el-input
          v-model="query.keyword"
          :placeholder="t('trash.keywordPlaceholder')"
          clearable
          :prefix-icon="Search"
          style="width: 260px"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">{{ t('trash.search') }}</el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">{{ t('trash.reset') }}</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" stripe>
      <el-table-column prop="id" :label="t('trash.columns.paymentId')" width="110" />
      <el-table-column prop="fromAccount" label="From" min-width="130" />
      <el-table-column prop="toAccount" label="To" min-width="130" />
      <el-table-column :label="t('trash.columns.amount')" width="150" align="right">
        <template #default="{ row }">
          <span class="amount-cell">{{ row.amount }} {{ row.currency }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('trash.columns.status')" width="130" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" effect="light">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" :label="t('trash.columns.remark')" show-overflow-tooltip min-width="180" />
      <el-table-column :label="t('trash.columns.deletedAt')" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.deletedAt) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('trash.columns.recoverableUntil')" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.recoverableUntil) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('trash.columns.actions')" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <div class="action-group">
            <el-button link type="primary" @click="handleRestore(row)">{{ t('trash.restore') }}</el-button>
            <el-popconfirm
              :title="t('trash.confirmDeletePrompt')"
              :confirm-button-text="t('trash.confirmDelete')"
              :cancel-button-text="t('trash.cancel')"
              @confirm="handlePermanentDelete(row)"
            >
              <template #reference>
                <el-button link type="danger">{{ t('trash.confirmDelete') }}</el-button>
              </template>
            </el-popconfirm>
          </div>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty :description="t('trash.empty')" />
      </template>
    </el-table>

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
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { Search, RefreshLeft } from '@element-plus/icons-vue';
import { listDeletedPayments, permanentlyDeletePayment, restorePayment } from '../api/payment';
import { formatDateTime } from '../utils/datetime';

const { t } = useI18n();

const query = reactive({
  keyword: '',
  page: 1,
  size: 10
});

const tableData = ref([]);
const total = ref(0);
const loading = ref(false);

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

async function fetchList() {
  loading.value = true;
  try {
    const res = await listDeletedPayments({
      keyword: query.keyword || undefined,
      page: query.page,
      size: query.size
    });
    tableData.value = res.data.list;
    total.value = res.data.total;
  } catch (error) {
    // 错误提示已由 http.js 拦截器统一处理
  } finally {
    loading.value = false;
  }
}

async function handleRestore(row) {
  try {
    await restorePayment(row.id);
    ElMessage.success(t('trash.restoreSuccess'));
    if (tableData.value.length === 1 && query.page > 1) {
      query.page -= 1;
    }
    fetchList();
  } catch (error) {
    // 错误提示已由 http.js 拦截器统一处理
  }
}

async function handlePermanentDelete(row) {
  try {
    await permanentlyDeletePayment(row.id);
    ElMessage.success(t('trash.confirmDeleteSuccess'));
    if (tableData.value.length === 1 && query.page > 1) {
      query.page -= 1;
    }
    fetchList();
  } catch (error) {
    // 错误提示已由 http.js 拦截器统一处理
  }
}

function handleSearch() {
  query.page = 1;
  fetchList();
}

function handleReset() {
  query.keyword = '';
  query.page = 1;
  fetchList();
}

function handleSizeChange() {
  query.page = 1;
  fetchList();
}

onMounted(fetchList);
</script>

<style scoped>
.trash-page {
  width: 100%;
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

.retention-alert {
  margin-bottom: 16px;
}

.filter-bar {
  margin-bottom: 8px;
}

.amount-cell {
  font-variant-numeric: tabular-nums;
}

.action-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>