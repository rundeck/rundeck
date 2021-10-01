<template>
    <button class="btn btn-default btn-simple btn-transparent project-select-btn"
        aria-describedby="projectPicker"
        aria-controls="projectPicker"
        :aria-expanded="open"
        @click="handleClick"
        @mousedown="e => e.preventDefault()">
        <i 
            class="fas project-select-btn__left-icon"
            :class="{'fa-box-open': projectLabel, 'fa-box': !projectLabel}"/>
        <span class="project-select-btn__label">{{projectLabel || 'Projects'}}</span>
        <i class="fas project-select-btn__right-icon" :class="[`fa-chevron-${open ? 'up' : 'down'}`]"/>
        <Popper v-if="open" @close="close">
            <div id="projectPicker" class="card card--popover project-select-btn__popper">
                <ProjectSelect @project:selected="handleSelect" @project:select-all="handleSelectAll"/>
            </div>
        </Popper>
    </button>
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
        close() {
            this.open = false
        },
        handleClick() {
            this.open = !this.open
        },
        handleSelect(project: any) {
            this.$emit('project:selected', project)
        },
        handleSelectAll() {
            this.$emit('project:select-all')
        }
    }
})
</script>

<style scoped lang="scss">
.project-select-btn {
    display: flex;
    align-content: center;
    align-items: center;
    width: 300px;
    span {
        margin: 0 5px;
    }

    .project-select-btn__right-icon {
        margin-left: auto;
    }

    .project-select-btn__label {
        overflow: hidden;
        text-overflow: ellipsis;
    }
}

.project-select-btn__popper {
    width: 300px;
    height: 400px;
    overflow: hidden;
}
</style>