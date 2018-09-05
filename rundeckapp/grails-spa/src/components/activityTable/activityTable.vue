<template>
  <div>HELLO WORLD 2</div>
</template>

<script>
console.log('hello')
export default {
  name: 'ActivityTable',
  data () {
    return {
      test: null
    }
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
