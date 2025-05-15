// Type definitions for vue-i18n
import { DefineComponent } from 'vue'

declare module '@vue/runtime-core' {
  interface ComponentCustomProperties {
    $t: (key: string, values?: Record<string, any>) => string
    $tc: (key: string, choice?: number, values?: Record<string, any>) => string
    $te: (key: string, locale?: string) => boolean
    $d: (value: number | Date, key?: string, locale?: string) => string
    $n: (value: number, key?: string, locale?: string) => string
  }

  // We also augment CreateComponentPublicInstanceWithMixins
  interface CreateComponentPublicInstanceWithMixins {
    $t: (key: string, values?: Record<string, any>) => string
    $tc: (key: string, choice?: number, values?: Record<string, any>) => string
    $te: (key: string, locale?: string) => boolean
    $d: (value: number | Date, key?: string, locale?: string) => string
    $n: (value: number, key?: string, locale?: string) => string
  }
}

export {}