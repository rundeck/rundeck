<template>
    <div class="btn project-select-btn" @click="handleClick">
        <i class="fas fa-box-open"/>
        <span>{{projectLabel}}</span>
        <i class="fas" :class="[`fa-chevron-${open ? 'up' : 'down'}`]"/>
        <Popper v-if="open">
            <ProjectSelect @project:selected="handleSelect"/>
        </Popper>
    </div>
</template>

<script lang="ts">
import Vue from 'vue'

import Popper from '../../utility-bar/Popper.vue'

import ProjectSelect from './ProjectSelect.vue'

export default Vue.extend({
    components: {
        Popper,
        ProjectSelect
    },
    props: {
        projectLabel: {type: String}
    },
    data: () => ({
        open: false
    }),
    methods: {
        handleClick() {
            this.open = !this.open
        },
        handleSelect(project: any) {
            this.$emit('project:selected', project)
        }
    }
})
</script>

<style scoped lang="scss">
.project-select-btn {
    span {
        margin: 0 5px;
    }
}
</style>