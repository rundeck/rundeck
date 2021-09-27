<template>
    <div ref="drawer" class="rd-drawer" :class="[`rd-drawer--${placement}`, display ? 'rd-drawer--active' : '']">
        <div v-if="displayHeader()" class="rd-drawer__header">
            <div v-if="title" class="rd-drawer__title">{{title}}</div>
            <div v-if="closeable" class="btn btn-default btn-link" style="margin-left: auto;" @click="() => {$emit('close')}">Close</div>
        </div>
        <slot/>
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
        width: {default: '256px'}
    },
    data() { return {
        display: false
    }},
    mounted() {
        this.display = this.visible;
        (<HTMLElement>this.$el).style.setProperty('--rd-drawer-width', this.width)
    },
    methods: {
        displayHeader(): boolean {
            return (this.title?.length > 0 || this.closeable)
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
.rd-drawer {
    position: absolute;
    top: 0;
    bottom: 0;
    overflow-x: hidden;
    overflow-y: auto;
    background-color: var(--background-color);
    box-shadow: none;
    width: var(--rd-drawer-width);
    z-index: 100;

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

    &--active {
        box-shadow: rgba(0, 0, 0, 0.15) 2px 0px 8px 0px;
    }
}

.rd-drawer--left {
    left: calc(-1 * var(--rd-drawer-width));
    transition: left .2s ease-in-out;

    &.rd-drawer--active {
        left: 0px;
    }
}

.rd-drawer--right {
    right: calc(-1 * var(--rd-drawer-width));
    transition: right .2s ease-in-out;

    &.rd-drawer--active {
        right: 0px;
    }
}

.rd-drawer--top {
    top: calc(-1 * var(--rd-drawer-width));
    width: 100%;
    height: var(--rd-drawer-width);
    transition: top .2s ease-in-out;

    &.rd-drawer--active {
        top: 0px;
    }
}

</style>