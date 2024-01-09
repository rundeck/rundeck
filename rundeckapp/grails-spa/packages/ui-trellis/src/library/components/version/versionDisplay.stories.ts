import type {Meta, StoryFn} from '@storybook/vue3'

import VersionDisplay from './VersionDisplay.vue'

export default {
    title: 'Rundeck Version Atoms/Version Display',
    component: VersionDisplay,
} as Meta<typeof VersionDisplay>

export const rundeckVersionDisplay: StoryFn<typeof VersionDisplay> = (args) => ({
    setup() {
        return {args}
    },
    components: {VersionDisplay},
    template: `
      <VersionDisplay v-bind="args"/>`
})
rundeckVersionDisplay.args = {
    version: '4.0.0',
    date: '2022-03-09',
    appName: 'Rundeck',
}
export const rundeckVersionArray: StoryFn<typeof VersionDisplay> = (args) => ({
    setup() {
        return {...args}
    },
    components: {VersionDisplay},
    template: `
      <table>
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
rundeckVersionArray.args = {
    date: '2022-03-09',
    appName: 'Rundeck',
}