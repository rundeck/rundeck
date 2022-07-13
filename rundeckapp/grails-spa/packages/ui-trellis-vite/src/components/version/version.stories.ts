import Vue from 'vue'

import RundeckVersionDisplay from './RundeckVersionDisplay.vue'
import ServerDisplay from './ServerDisplay.vue'
import VersionDisplay from './VersionDisplay.vue'

export default {
    title: 'Rundeck Version Atoms'
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
export const rundeckVersionDisplay = () => ({
    components: { VersionDisplay },
    props: {
        version: {default: '4.0.0'},
        date:{default:'2022-03-09'},
        appName:{default: 'Rundeck'}
    },
    template: `<VersionDisplay v-bind="$props"/>`
})
export const rundeckVersionArray = () => ({
    components: { VersionDisplay },
    props: {
        date:{default:'2022-03-09'},
        appName:{default:'Rundeck'}
    },
    template: `
        <table >
        <tr>
            <td>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.0.0"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.0.1"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.0.2"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.0.3"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.0.4"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.0.5"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.0.6"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.0.7"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.0.8"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.0.9"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.0.10"/>
        </div>
            </td>
            <td>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.1.0"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.1.1"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.1.2"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.1.3"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.1.4"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.1.5"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.1.6"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.1.7"/>
        </div>
                <div>
                    <VersionDisplay :name="appName" :date="date" version="4.1.8"/>
                </div>
                <div>
                    <VersionDisplay :name="appName" :date="date" version="4.1.9"/>
                </div>
                <div>
                    <VersionDisplay :name="appName" :date="date" version="4.1.10"/>
                </div>
            </td>
            <td>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.2.0"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.2.1"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.2.2"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.2.3"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.2.4"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.2.5"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.2.6"/>
        </div>
        <div>
            <VersionDisplay :name="appName" :date="date" version="4.2.7"/>
        </div>
                <div>
                    <VersionDisplay :name="appName" :date="date" version="4.2.8"/>
                </div>
                <div>
                    <VersionDisplay :name="appName" :date="date" version="4.2.9"/>
                </div>
                <div>
                    <VersionDisplay :name="appName" :date="date" version="4.2.10"/>
                </div>
            </td>
        </tr>
        </table>
    `,
})