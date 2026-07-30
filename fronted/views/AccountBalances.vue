<template>
  <el-card class="accounts-page" shadow="never">
    <template #header>
      <div class="card-header">
        <span class="card-title">
          <el-icon :size="18" class="title-icon"><Search /></el-icon>
          {{ t('accounts.title') }}
        </span>
      </div>
    </template>

    <el-alert :title="t('accounts.hint')" type="info" show-icon :closable="false" class="hint-alert" />

    <div class="toolbar">
      <el-form :inline="true" class="filter-bar">
        <el-form-item :label="t('accounts.keyword')">
          <el-input
            v-model="query.keyword"
            :placeholder="t('accounts.keywordPlaceholder')"
            clearable
            :prefix-icon="Search"
            style="width: 280px"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" round :icon="Search" @click="handleSearch">{{ t('accounts.search') }}</el-button>
          <el-button round :icon="RefreshLeft" @click="handleReset">{{ t('accounts.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table v-loading="loading" :data="pagedAccounts" stripe :header-cell-style="{ textAlign: 'center' }">
      <el-table-column prop="accountNo" :label="t('accounts.columns.accountNo')" min-width="140" align="center" />
      <el-table-column prop="ownerName" :label="t('accounts.columns.ownerName')" min-width="180" align="center" />
      <el-table-column prop="currency" :label="t('accounts.columns.currency')" width="120" align="center" />
      <el-table-column :label="t('accounts.columns.status')" width="140" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" effect="light">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('accounts.columns.balance')" min-width="180" align="center">
        <template #default="{ row }">
          <span class="balance-cell">{{ formatBalance(row.balance, row.currency) }}</span>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty :description="t('accounts.empty')" />
      </template>
    </el-table>

    <el-pagination
      class="pagination"
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      :page-sizes="[5, 10, 20]"
      :total="accounts.length"
      background
      layout="total, sizes, prev, pager, next"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { RefreshLeft, Search } from '@element-plus/icons-vue';
import { listAccounts } from '../api/account';

const { t, n } = useI18n();

const query = reactive({
  keyword: '',
  page: 1,
  size: 10
});

const accounts = ref([]);
const loading = ref(false);

const pagedAccounts = computed(() => {
  const start = (query.page - 1) * query.size;
  return accounts.value.slice(start, start + query.size);
});

function formatBalance(balance, currency) {
  return n(balance, { key: 'currency', currency });
}

function statusTagType(status) {
  const map = {
    ACTIVE: 'success',
    INACTIVE: 'info',
    FROZEN: 'warning'
  };
  return map[status] || 'info';
}

async function fetchAccounts() {
  loading.value = true;
  try {
    const res = await listAccounts({
      keyword: query.keyword || undefined
    });
    accounts.value = res.data;
  } catch (error) {
    accounts.value = [];
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.page = 1;
  fetchAccounts();
}

function handleReset() {
  query.keyword = '';
  query.page = 1;
  fetchAccounts();
}

function handlePageChange() {
  return;
}

function handleSizeChange() {
  query.page = 1;
}

onMounted(fetchAccounts);
</script>

<style scoped>
.accounts-page {
  width: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.title-icon {
  color: var(--color-primary);
}

.hint-alert {
  margin-bottom: 16px;
  border-radius: 10px;
}

.toolbar {
  background: #f8f9fc;
  border-radius: 12px;
  padding: 12px 16px 0;
  margin-bottom: 16px;
}

.filter-bar {
  margin: 0;
}

.balance-cell {
  font-variant-numeric: tabular-nums;
}

.pagination {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f1f5;
  justify-content: flex-end;
}
</style>