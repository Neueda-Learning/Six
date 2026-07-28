<!-- 创建支付页面：提供表单供用户填写付款信息，前端做基础校验后调用后端创建支付接口 -->
<template>
  <el-card class="create-page" shadow="never">
    <template #header>
      <div class="card-header">
        <el-icon :size="18"><CirclePlus /></el-icon>
        <span class="card-title">{{ t('create.title') }}</span>
      </div>
    </template>

    <!-- Element Plus 表单：ref 绑定实例便于调用 validate；rules 绑定校验规则；label-position 使用顶部标签更紧凑 -->
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="160px"
      label-position="top"
      class="create-form"
    >
      <el-row :gutter="24">
        <el-col :span="12">
          <!-- 源账户：必须与目标账户不同，且需在系统种子数据中真实存在（后端校验） -->
          <el-form-item :label="t('create.fromAccount')" prop="fromAccount">
            <el-input v-model="form.fromAccount" :placeholder="t('create.fromAccountPlaceholder')" :prefix-icon="User" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <!-- 目标账户 -->
          <el-form-item :label="t('create.toAccount')" prop="toAccount">
            <el-input v-model="form.toAccount" :placeholder="t('create.toAccountPlaceholder')" :prefix-icon="User" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="24">
        <el-col :span="12">
          <!-- 金额：数字输入框，限制最小值、最大值与两位小数精度 -->
          <el-form-item :label="t('create.amount')" prop="amount">
            <el-input-number
              v-model="form.amount"
              :min="0.01"
              :max="1000000"
              :precision="2"
              :step="0.01"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <!-- 货币：下拉选择，仅提供后端白名单支持的三种货币 -->
          <el-form-item :label="t('create.currency')" prop="currency">
            <el-select v-model="form.currency" :placeholder="t('create.currencyPlaceholder')" style="width: 100%">
              <el-option v-for="item in currencyOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 备注：可选字段 -->
      <el-form-item :label="t('create.remark')" prop="remark">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="2"
          :placeholder="t('create.remarkPlaceholder')"
        />
      </el-form-item>

      <!-- 幂等键仍会在提交时随表单一起发送给后端（见 form.idempotencyKey 生成逻辑），但不在页面上展示给用户，因此此处不再渲染对应的 el-form-item -->

      <el-divider />

      <el-form-item>
        <el-button type="primary" :icon="Check" :loading="submitting" @click="handleSubmit">
          {{ t('create.submit') }}
        </el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">{{ t('create.reset') }}</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { CirclePlus, User, RefreshLeft, Check } from '@element-plus/icons-vue';
import { createPayment } from '../api/payment';

const router = useRouter();
const { t } = useI18n();

// 表单引用，用于触发 Element Plus 内置的字段校验
const formRef = ref(null);
// 提交中状态，控制按钮 loading，避免重复点击导致重复提交
const submitting = ref(false);

// 货币白名单：与后端 ErrorCode.INVALID_CURRENCY 校验规则保持一致
const currencyOptions = ['USD', 'EUR', 'GBP'];

// 表单数据模型，字段命名与后端 CreatePaymentRequest 保持一致，便于直接作为请求体提交
const form = reactive({
  fromAccount: '',
  toAccount: '',
  amount: undefined,
  currency: '',
  remark: '',
  idempotencyKey: generateUuid()
});

/**
 * 生成幂等键。
 * 优先使用浏览器原生 crypto.randomUUID；不支持时退化为时间戳+随机数拼接，保证仍然唯一。
 * 该键仅用于后端幂等校验，不在页面上展示给用户。
 */
function generateUuid() {
  if (window.crypto && typeof window.crypto.randomUUID === 'function') {
    return window.crypto.randomUUID();
  }
  return `uuid-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

/**
 * 校验源账户与目标账户不能相同。
 * 与后端 INVALID_ACCOUNT 规则对应，此处仅做前端提前拦截，最终以后端校验结果为准。
 */
function validateToAccount(rule, value, callback) {
  if (value && form.fromAccount && value === form.fromAccount) {
    callback(new Error(t('create.validation.toDifferent')));
  } else {
    callback();
  }
}

// 表单校验规则：必填项 + 金额范围 + 账户不同校验
// 使用 computed 包裹，确保切换语言后校验提示文案能实时更新
const rules = computed(() => ({
  fromAccount: [{ required: true, message: t('create.validation.fromRequired'), trigger: 'blur' }],
  toAccount: [
    { required: true, message: t('create.validation.toRequired'), trigger: 'blur' },
    { validator: validateToAccount, trigger: 'blur' }
  ],
  amount: [
    { required: true, message: t('create.validation.amountRequired'), trigger: 'blur' },
    { type: 'number', min: 0.01, max: 1000000, message: t('create.validation.amountRange'), trigger: 'blur' }
  ],
  currency: [{ required: true, message: t('create.validation.currencyRequired'), trigger: 'change' }]
}));

/** 提交表单：先做前端校验，通过后调用创建支付接口 */
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) {
    return;
  }

  submitting.value = true;
  try {
    const res = await createPayment({ ...form });
    // 后端幂等命中或新建成功都会返回 success = true，data 中携带支付详情（含 id）
    ElMessage.success(res.message || t('create.submitSuccess'));
    router.push(`/payments/${res.data.id}`);
  } catch (error) {
    // 具体错误提示已由 http.js 拦截器统一弹出，这里无需重复处理
  } finally {
    submitting.value = false;
  }
}

/** 重置表单并生成新的幂等键 */
function handleReset() {
  formRef.value.resetFields();
  form.idempotencyKey = generateUuid();
}
</script>

<style scoped>
.create-page {
  max-width: 760px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.create-form {
  margin-top: 4px;
}
</style>

