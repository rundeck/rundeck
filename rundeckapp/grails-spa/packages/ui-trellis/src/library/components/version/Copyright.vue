<template>
    <span class="copyright">
        {{ copyright }}
    </span>
</template>

<script>
export default {
    data () {
        return {
            copyright: BUILD_COPYRIGHT
        }
    }
}
</script>

<style scoped lang="scss">
.copyright {
    color: var(--font-color);
}
</style>