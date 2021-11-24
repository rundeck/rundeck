import NodeIcon from './NodeIcon.vue'
import Vue from 'vue'
import {addons} from '@storybook/addons'
import {CHANGE, array ,object, boolean, withKnobs, text} from '@storybook/addon-knobs'


export default {
  title: 'Nodes/UI',
  decorators: [withKnobs({disableDebounce: true})]
}

export const nodeIconDefault = () => {
  return Vue.extend({
    template: `<node-icon v-bind="$props"/>`,
    components: { NodeIcon},
    props: {
      node: {
        default: object('Node', {
          name:'anode',
          attributes:{

          }
        })
      },
      defaultIconCss: {
        default: 'fas fa-hdd'
      }
    },
  })
}
