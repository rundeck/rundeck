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
            style="height: 0;width: 0;"/>
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
.switch {
    --animation-duration: calc(250ms * var(--animation-scale));
    background-color: #DBDBDB;
    border: none;
    height: 20px;
    width: 37px;
    position: relative;
    border-radius: 1000px;
    transition: all var(--animation-duration) ease-out;
    border-color: transparent;

    &--contrast {
        border: 2px solid white;
        height: 24px;
        width: 41px;
    }

    &--checked {
        // background-color: paleturquoise;
        background-color: var(--accent-color);
        transition-duration: var(--animation-duration);
        transition-property: all;
        transition-timing-function: ease-out;
    }

    &--disabled {
        cursor: not-allowed;
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
            background-color: white;
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
</style>