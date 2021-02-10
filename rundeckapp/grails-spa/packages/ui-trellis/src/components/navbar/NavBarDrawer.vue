<template>
    <div>
        <slot/>
        <div ref="drawer" class="nav-drawer">
            <slot name="content"/>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
    data: {
        display: false
    },
    mounted() {
        const drawer = this.$refs['drawer'] as HTMLElement
        drawer.style.display = this.display ? 'inherit' : 'none'

        drawer.parentNode!.removeChild(drawer)

        document.body.appendChild(drawer);

        (<HTMLElement>this.$el).addEventListener('click', () => {
            this.display = !this.display;
            (<HTMLElement>this.$refs['drawer']).style.display = this.display ? 'inherit' : 'none'
        })
    },
    beforeDestroy() {
        const drawer = this.$refs['drawer'] as HTMLElement
        drawer.remove()
    }
})
</script>

<style scoped lang="scss">
.nav-drawer {
    position: absolute;
    left: 64px;
    top: 0;
    overflow-x: hidden;
    overflow-y: auto;
    border-radius: 5px 10px 10px 5px;
    border-style: solid;
    border-color: #414141;
    border-width: 1px;
    background-color: #303030;
    height: 100vh;
    width: 250px;
    padding: 10px;

    ul {
        flex-wrap: wrap;
        margin: 0;
        padding: 10px;
        align-items: start;
        align-content: space-between;
    }

    li {
        width: 40px;
        margin: 10px;
    }
}
</style>