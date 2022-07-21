<template>
    <div
        class="switch"
        type="button"
        role="switch"
        :aria-checked="value"
        tabindex=0
        :class="{
            'switch--checked': value,
            'switch--disabled': disabled,
            'switch--contrast': contrast,
        }"
        @click="handleSelect"
        @keypress.space="handleSelect">
        <input
            ref="input"
            v-model="value"
            v-on:input="$emit('input', $event.target.value)"
            type="checkbox"
            style="height: 0;width: 0;appearance: none;"/>
        <span 
            class="switch__slider"
            :class="{'switch__slider--checked': value}"/>
    </div>
</template>

<script lang="ts">
import Vue from 'vue'
export default Vue.extend({
    name: 'rd-switch',
    props: {
        value: {
            type: Boolean,
            default: false
        },
        disabled: {
            type: Boolean,
            default: false
        },
        contrast: {
            type: Boolean,
            default: false
        }
    },
    methods: {
        handleSelect() {
            this.$emit('input', !this.value)
        }
    }
})
</script>

<style scoped lang="scss">
.caca-switch{
    background-color: caca;
}
.switch {
    --animation-duration: calc(250ms * var(--animation-scale));
    background-color: var(--grey-500);
    border: none;
    height: 20px;
    width: 37px;
    position: relative;
    border-radius: 1000px;
    transition: all var(--animation-duration) ease-out;
    border-color: transparent;
    cursor: pointer;

    &--contrast {
        border: 2px solid var(--default-color);
        height: 24px;
        width: 41px;
    }

    &--checked {
        // background-color: paleturquoise;
        background-color: var(--success-color);
        transition-duration: var(--animation-duration);
        transition-property: all;
        transition-timing-function: ease-out;
    }

    

    &__slider {
        height: 100%;
        width: 100%;
        &::before {
            bottom: 2px;
            left: 2px;
            border-radius: 1000px;
            height: 16px;
            width: 16px;
            background-color: var(--default-color);
            content: '';
            position: absolute;
            box-shadow: 0px 0px 4px rgba(0, 0, 0, 0.25);
            transition-duration: var(--animation-duration);
            transition-property: all;
            transition-timing-function: ease-out;
        }

        &--checked::before {
            left: 19px !important;
            transition-duration: var(--animation-duration);
            transition-property: all;
            transition-timing-function: ease-out;
        }
    }
}

.switch--disabled {
    cursor: not-allowed;
    background-color: var(--grey-300);

    @at-root &.switch--checked {
        background-color: var(--success-bg-color);
    }
}
</style>