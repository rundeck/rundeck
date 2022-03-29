<template>
  <div>
    <h1>Hey im from Enterprise</h1>
  </div>
</template>

<route lang="yaml">
meta:
  layout: HomeLayout
</route>

<script setup lang="ts">
import { useEnterpriseComponentsStore } from '~/enterprise/stores/components'

const props = defineProps<{ name: string }>()
const router = useRouter()
const something = useEnterpriseComponentsStore.something
const { t } = useI18n()

console.log(something)
</script>