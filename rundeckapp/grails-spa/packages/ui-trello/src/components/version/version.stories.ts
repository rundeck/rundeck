import Vue from 'vue'
import {array ,object, withKnobs} from '@storybook/addon-knobs'

import UtilityBar from './UtilityBar.vue'
import RundeckInfo from './RundeckInfo.vue'

import {RootStore} from '../../stores/RootStore'

import RundeckVersionDisplay from './RundeckVersionDisplay.vue'
import ServerDisplay from './ServerDisplay.vue'
import { Server } from 'http'
import { ServerInfo } from '../../stores/System'

export default {
    title: 'Rundeck Version Atoms',
    decorators: [withKnobs]
}

export const serverInfo = () => ({
    components: { ServerDisplay },
    data: () => ({
        name: 'xubuntu',
        glyphicon: 'flash',
        uuid: 'f1dbb7ed-c575-4154-8d01-216a59d7cb5e'
    }),
    template: '<ServerDisplay v-bind="$data"/>',
});

export const rundeckVersion = () => ({
    components: { RundeckVersionDisplay },
    data: () => ({
        edition: 'Community',
        number: '3.4.0',
        tag: 'SNAPSHOT'
    }),
    template: `<RundeckVersionDisplay v-bind="$data"/>`
})