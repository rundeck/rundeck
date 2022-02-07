<template>
    <div class="rd-drawer__wrapper">
        <div ref="drawer" class="rd-drawer" :class="[`rd-drawer--${placement}`, display ? 'rd-drawer--active' : '']">
            <div v-if="displayHeader()" class="rd-drawer__header">
                <div v-if="title" class="rd-drawer__title">{{title}}</div>
                <div v-if="closeable" class="btn btn-default btn-link" style="margin-left: auto;" @click="() => {$emit('close')}">Close</div>
            </div>
            <slot/>
        </div>
        <div v-if="mask" class="rd-drawer__mask" :class="{'rd-drawer__mask--active': display}" @click="() => {$emit('close')}"/>
    </div>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
    name: 'rd-drawer',
    props: {
        title: {default: ''},
        visible: {type: Boolean},
        closeable: {default: true},
        placement: {default: 'left'},
        width: {default: ''},
        height: {default: ''},
        mask: {default: true}
    },
    data() { return {
        display: false
    }},
    mounted() {
        this.display = this.visible;
        (<HTMLElement>this.$el).style.setProperty('--rd-drawer-width', this.drawerWidth);
        (<HTMLElement>this.$el).style.setProperty('--rd-drawer-height', this.drawerHeight);
    },
    methods: {
        displayHeader(): boolean {
            return (this.title?.length > 0 || this.closeable)
        }
    },
    computed: {
        drawerHeight(): string {
            if (['left', 'right'].includes(this.placement))
                return this.height || '100%'
            else
                return this.height || '256px'
        },
        drawerWidth(): string {
            if (['left', 'right'].includes(this.placement))
                return this.width || '256px'
            else
                return this.width || '100%'
        }
    },
    watch: {
        visible(newVal, oldVal) {
            this.display = newVal
        }
    }
})
</script>

<style scoped lang="scss">
.rd-drawer__wrapper {
    --rd-drawer-transition-time: calc(var(--animation-scale) * 200ms);
}


.rd-drawer {
    position: absolute;
    overflow-x: hidden;
    overflow-y: auto;
    background-color: var(--motd-drawer-background-color);
    box-shadow: none;
    width: var(--rd-drawer-width);
    z-index: 5000;

    &__header {
        padding: 10px;
        display: flex;
        align-content: center;
        align-items: center;
    }

    &__title {
        font-weight: 800;
        font-size: 1.5em;
    }
}

.rd-drawer--left {
    top: 0px;
    max-height: 100%;
    height: var(--rd-drawer-height);
    right: 100%;
    transition: right var(--rd-drawer-transition-time) ease-in-out;

    &.rd-drawer--active {
        right: calc(100% - var(--rd-drawer-width));
        box-shadow: rgba(0, 0, 0, 0.15) 2px 0px 8px 0px;
    }
}

.rd-drawer--right {
    top: 0px;
    max-height: 100%;
    height: var(--rd-drawer-height);
    left: 100%;
    transition: left var(--rd-drawer-transition-time) ease-in-out;

    &.rd-drawer--active {
        left: calc(100% - var(--rd-drawer-width));
        box-shadow: rgba(0, 0, 0, 0.2) -2px 2px 8px 0px;
    }
}

.rd-drawer--top {
    left: 0px;
    bottom: 100%;
    width: 100%;
    height: var(--rd-drawer-width);
    transition: bottom var(--rd-drawer-transition-time) ease-in-out;

    &.rd-drawer--active {
        bottom: calc(100% - var(--rd-drawer-width));
        box-shadow: rgba(0, 0, 0, 0.15) 0px 2px 8px 0px;
    }
}

.rd-drawer__mask {
    position: absolute;
    background-color: transparent;
    top: 0px;
    left: 0px;
    z-index: 4999;
    height: 0px;
    width: 100%;
    background-color: transparent;

    &--active {
        height: 100%;
        background-color: rgba(0, 0, 0, 0.7);
        transition: background-color var(--rd-drawer-transition-time) ease-in-out;
    }
}

</style>