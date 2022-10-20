<template>
  <div class="form-group">
    <div :class="`${labelColClass} text-form-label`">
      {{ $t('scheduledExecution.property.orchestrator.label') }}
    </div>
    <div :class="fieldColSize">
      <dropdown ref="dropdown" id="orchestrator-edit-type-dropdown">
        <btn type="simple"  class="btn-simple btn-hover  btn-secondary dropdown-toggle">
          <span class="caret"></span>
          &nbsp;
          <span v-if="updatedValue.type && getProviderFor(updatedValue.type)">
                    <plugin-info
                        :detail="getProviderFor(updatedValue.type)"
                        :show-description="false"
                        :show-extended="false"
                        description-css="help-block"
                    >
                      </plugin-info>
                  </span>
          <span v-else>
                  Select an Orchestrator
                  </span>
        </btn>
        <template slot="dropdown">
          <li v-for="plugin in pluginProviders" :key="plugin.name">
            <a role="button"
               @click="setOrchestratorType(plugin.name)"
               :data-plugin-type="plugin.name">
              <plugin-info
                  :detail="plugin"
                  :show-description="true"
                  :show-extended="false"
                  description-css="help-block"
              >
              </plugin-info>
            </a>
          </li>
        </template>
      </dropdown>
      <btn size="xs" type="danger" @click="remove" v-if="updatedValue.type">
        <i class="fas fa-times"></i>
        Remove Orchestrator
      </btn>
      <input type="hidden" name="orchestratorId" :value="updatedValue.type"/>

      <span class="help-block">
          {{ $t('scheduledExecution.property.orchestrator.description') }}
      </span>

      <span class="orchestratorPlugin" v-if="getProviderFor(updatedValue.type)">
                    <span class="text-info">
                       <plugin-info
                           :detail="getProviderFor(updatedValue.type)"
                           :show-title="false"
                           :show-icon="false"
                           :show-description="true"
                           :show-extended="true"
                           description-css="help-block"
                       >
                      </plugin-info>
                    </span>
                <div>
                  <template v-for="(val,key) in updatedValue.config">
                    <input type="hidden" :name="`orchestratorPlugin.${updatedValue.type}.config.${key}`" :value="val"/>
                  </template>

                  <plugin-config
                      id="orchestrator-edit-config"
                      mode="edit"
                      :serviceName="'Orchestrator'"
                      v-model="updatedValue"
                      :key="'edit_config'+updatedValue.type"
                      :show-title="false"
                      :show-description="false"
                      :context-autocomplete="false"
                      :validation="editValidation"
                      scope="Instance"
                      default-scope="Instance"
                  ></plugin-config>

                </div>
              </span>

    </div>
  </div>
</template>
<script lang="ts">
import InlineValidationErrors from '@/app/components/form/InlineValidationErrors.vue'
import Vue from 'vue'
import Component from 'vue-class-component'
import {Prop, Watch} from 'vue-property-decorator'

import PluginInfo from '@/library/components/plugins/PluginInfo.vue'
import PluginConfig from '@/library/components/plugins/pluginConfig.vue'
import pluginService from '@/library/modules/pluginService'
import ExtendedDescription from '@/library/components/utils/ExtendedDescription.vue'

@Component({components: {InlineValidationErrors, PluginInfo, PluginConfig, ExtendedDescription}})
export default class OrchestratorEditor extends Vue {
  /**
   * Orchestrator type and config value
   */
  @Prop({required: true})
  value: any

  @Prop({required: false, default: 'col-sm-2 control-label'})
  labelColClass!: string

  @Prop({required: false, default: 'col-sm-10'})
  fieldColSize!: string

  @Prop({required: false, default:()=>{}})
  editValidation!: any

  pluginProviders: Array<any> = []
  pluginLabels: { [name: string]: string } = {}

  updatedValue: any = {}

  @Watch('updatedValue')
  async valueUpdated() {
    this.$emit('input', this.updatedValue)
  }
  remove(){
    Vue.set(this.updatedValue, 'type', null)
    Vue.set(this.updatedValue, 'config', {})
  }

  setOrchestratorType(name:string){
    Vue.set(this.updatedValue, 'type', name)
  }
  getProviderFor(name:string) {
    return this.pluginProviders.find(p => p.name === name)
  }

  async mounted() {
    this.updatedValue = Object.assign({type: null, config: {}}, this.value)
    let data = await pluginService.getPluginProvidersForService('Orchestrator')
    if (data.service) {
      this.pluginProviders = data.descriptions
      this.pluginLabels = data.labels
    }
  }
}
</script>