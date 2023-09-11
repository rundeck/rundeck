<template>
    <div
        class="switch"
        type="button"
        role="switch"
        :aria-checked="modelValue"
        tabindex=0
        :class="{
            'switch--checked': modelValue,
            'switch--disabled': disabled,
            'switch--contrast': contrast,
        }"
        @click="handleSelect"
        @keypress.space="handleSelect">
        <input
            ref="input"
            v-model="modelValue"
            v-on:input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
            type="checkbox"
            style="height: 0;width: 0;appearance: none;"/>
        <span 
            class="switch__slider"
            :class="{'switch__slider--checked': modelValue}"></span>
    </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
export default defineComponent({
    name: 'rd-switch',
    props: {
        modelValue: {
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
    emits: ['update:modelValue'],
    methods: {
        handleSelect() {
            this.$emit('update:modelValue', !this.modelValue)
        }
    }
})
</script>

<style scoped lang="scss">
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