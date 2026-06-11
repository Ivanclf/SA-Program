import type { FormItemRule } from 'element-plus'

/**
 * 公共表单验证规则
 *
 * 使用方式:
 *   import { nameRule, pricePositiveRule, etimeAfterStimeRule } from '@/utils/validators'
 *   const rules = { skuName: [nameRule('SKU 名称')], originalPrice: [pricePositiveRule] }
 */

/** 必填名称：1-100 字符 */
export function nameRule(label: string = '名称'): FormItemRule {
  return {
    required: true,
    message: `请输入${label}`,
    trigger: 'blur',
  }
}

/** 名称长度限制 */
export function nameLengthRule(min: number = 1, max: number = 100): FormItemRule {
  return {
    min,
    max,
    message: `长度 ${min}-${max} 字符`,
    trigger: 'blur',
  }
}

/** 必填 + 长度限制（组合返回数组） */
export function nameRules(label: string = '名称'): FormItemRule[] {
  return [nameRule(label), nameLengthRule()]
}

/** 必填数字，必须 > 0 */
export function pricePositiveRule(label: string = '价格'): FormItemRule {
  return {
    required: true,
    message: `请输入${label}`,
    trigger: 'blur',
  }
}

/** 价格 > 0 自定义验证 */
export function pricePositiveValidator(label: string = '价格'): FormItemRule {
  return {
    validator: (_rule, value, callback) => {
      if (value === undefined || value === null || Number(value) <= 0) {
        callback(new Error(`${label}必须大于 0`))
      } else {
        callback()
      }
    },
    trigger: 'blur',
  }
}

/** 价格验证规则组合 */
export function priceRules(label: string = '价格'): FormItemRule[] {
  return [pricePositiveRule(label), pricePositiveValidator(label)]
}

/**
 * 结束时间必须晚于开始时间
 * @param getStime 获取开始时间值的函数（支持 reactive/ref 动态值）
 */
export function etimeAfterStimeRule(getStime: () => string | undefined): FormItemRule {
  return {
    required: true,
    message: '请选择结束时间',
    trigger: 'change',
  }
}

/** 结束时间 > 开始时间自定义验证器 */
export function etimeAfterStimeValidator(getStime: () => string | undefined): FormItemRule {
  return {
    validator: (_rule, value, callback) => {
      const stime = getStime()
      if (value && stime && new Date(value) <= new Date(stime)) {
        callback(new Error('结束时间必须晚于开始时间'))
      } else {
        callback()
      }
    },
    trigger: 'change',
  }
}

/** 折扣范围验证器 */
export function discountRangeValidator: FormItemRule = {
  validator: (_rule, value, callback) => {
    if (value === undefined || value === null || value < 0.01 || value > 1.0) {
      callback(new Error('折扣须在 0.01 ~ 1.00 之间'))
    } else {
      callback()
    }
  },
  trigger: 'blur',
}
