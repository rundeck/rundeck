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

        const nav = document.getElementById('section-navbar')

        if (nav)
            nav.appendChild(drawer)

        drawer.addEventListener('click', (evt) => {
            evt.stopPropagation()
        });

        (<HTMLElement>this.$el).addEventListener('click', this.handleTargetClick)

        document.body.addEventListener('click', this.handleBodyClick)
    },
    beforeDestroy() {
        const drawer = this.$refs['drawer'] as HTMLElement
        document.body.removeEventListener('click', this.handleBodyClick)
        drawer.remove()
    },
    methods: {
        handleBodyClick(evt: MouseEvent) {
            this.display = false
            this.setDrawerVisibility()
        },
        handleTargetClick(evt: MouseEvent) {
            this.display = !this.display;
             (<HTMLElement>this.$el).parentElement.className = this.display ? 'navbar__item-container active' : 'navbar__item-container';
            (<HTMLElement>this.$refs['drawer']).style.display = this.display ? 'inherit' : 'none'
            evt.stopPropagation()
        },
        setDrawerVisibility() {
            (<HTMLElement>this.$refs['drawer']).style.display = this.display ? 'inherit' : 'none'
        }
    }
})
</script>

<style scoped lang="scss">
.nav-drawer {
   position: absolute;
    left: 62px;
    top: 0;
    bottom: 0;
    overflow-x: hidden;
    overflow-y: auto;
    border-radius: 5px 10px 10px 5px;
    border-style: solid;
    border-color: #414141;
    border-width: 1px;
    background-color: #303030;
    width: 250px;
    padding: 10px;
    z-index: 100;
}
</style>