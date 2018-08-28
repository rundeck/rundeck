<template>
  <div>HELLO WORLD 2</div>
</template>

<script>
import _ from 'lodash'

export default {
  name: 'ActivityTable',
  data () {
    return {
    }
  },
  methods: {

  },
  mounted () {

  }
}
</script>

<style lang="scss" scoped>
.motd-content {
  max-height: 200px;
  overflow-y: hidden;
  &.full {
    max-height: 100%;
    height: 100%;
    overflow: auto;
  }
}
</style>
