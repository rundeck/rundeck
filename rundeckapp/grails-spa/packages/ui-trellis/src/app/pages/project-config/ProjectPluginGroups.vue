<template>
  <div class>
    <div class v-if="title">
      <h3 class>
        {{title}}
        <a class="btn btn-primary btn-sm" @click="mode='edit'" v-if="mode!=='edit'">
          <i class="glyphicon glyphicon-pencil"></i>
          {{$t('Edit')}}
        </a>
      </h3>
    </div>

    <div class>
      <div v-if="errors.length>0" class="alert alert-danger">
        <ul>
          <li v-for="(error,index) in errors" :key="'error_'+index">{{error}}</li>
        </ul>
      </div>
      <div>
        <a class="btn btn-primary btn-sm" @click="mode='edit'" v-if="mode!=='edit'">
          <i class="glyphicon glyphicon-pencil"></i>
          {{$t('Edit')}}
        </a>
        <div class="help-block" v-if="help">{{help}}</div>

        <div class="list-group">
          <div
            class="list-group-item"
            v-for="(plugin,index) in workingData"
            :key="'pluginStorageAccessplugin_'+index"
          >
            <plugin-config
              :mode="'title'"
              :serviceName="serviceName"
              :provider="plugin.entry.type"
              :key="plugin.entry.type+'title/'+index"
              :show-description="true"
              @hasKeyStorageAccess="hasKeyStorageAccess"
            >
            </plugin-config>
            <plugin-config
              :mode="editFocus===(index) ? plugin.create?'create':'edit':'show'"
              :serviceName="serviceName"
              v-model="plugin.entry"
              :provider="plugin.entry.type"
              :config="plugin.entry.config"
              :key="plugin.entry.type+'_config/'+index"
              :show-title="false"
              :show-description="false"
              :validation="plugin.validation"
              :validation-warning-text="$t('Validation errors')"
              @input="configUpdated"
            >
              <template v-slot:extra v-if="mode==='edit'">
                <div class="row">
                  <div class="col-xs-12 col-sm-12">
                    <span v-if="editFocus===-1">
                      <a
                        class="btn btn-default btn-xs"
                        @click="editPlugin(index,plugin)"
                        :key="'edit'"
                      >{{$t('Edit')}}</a>
                    </span>
                    <span v-if="editFocus===index">
                      <a
                        class="btn btn-cta btn-xs"
                        @click="savePlugin(plugin,index)"
                        :key="'save'"
                      >{{$t('Save')}}</a>
                      <a class="btn btn-default btn-xs" @click="didCancel(plugin, index)">{{$t('Cancel')}}</a>
                    </span>
                    <span class="small text-info" v-if="plugin.modified">
                      <i class="fas fa-pen-square"></i>
                      Modified
                    </span>
                    <div class="btn-group pull-right" v-if="(editFocus===-1||editFocus===index)">
                      <btn
                        class="btn-xs btn-danger"
                        @click="removePlugin(plugin,index)"
                        :disabled="editFocus!==-1&&editFocus!==(index)"
                      >
                        {{$t('Delete')}}
                        <i class="fas fa-minus"></i>
                      </btn>
                    </div>
                  </div>
                </div>
              </template>
            </plugin-config>
            <slot name="item-extra" :plugin="plugin" :editFocus="editFocus===index" :mode="mode"></slot>
          </div>
          <div class="list-group-item" v-if="workingData.length<1 && showEmpty">
            <slot name="empty-message">
              <span  class="text-muted">
                {{$t("No Plugin Groups Configured", {serviceName})}}
              </span>
            </slot>
          </div>
        </div>
        <div class="card-footer" v-if="mode==='edit' && editFocus===-1">
          <btn type="primary" @click="modalAddOpen=true">
            <i class="fas fa-plus"></i>
            {{"Plugin Config"}}
          </btn>

          <modal
            v-model="modalAddOpen"
            :title="serviceName"
            ref="modal"
            size="lg"
            id="add-new-modal"
            append-to-body
          >
            <div class="list-group">
              <a
                v-for="plugin in filteredPluginProviders"
                v-bind:key="plugin.name"
                href="#"
                @click="addPlugin(plugin.name)"
                class="list-group-item"
              >
                <plugin-info
                  :detail="plugin"
                  :show-description="true"
                  :show-extended="false"
                  description-css="help-block"
                >
                  <i class="glyphicon glyphicon-plus text-muted"></i>
                </plugin-info>
              </a>
            </div>
            <template v-slot:footer>
              <div>
                <btn @click="modalAddOpen=false">{{$t('Cancel')}}</btn>
              </div>
            </template>
          </modal>
        </div>
        <div class="card-footer" v-if="mode==='edit' && editFocus===-1">
            <slot name="footer"></slot>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import axios from "axios";
import {defineComponent} from "vue";
import { Notification } from "uiv";
import { getRundeckContext, RundeckContext } from "../../../library";
import Expandable from "../../../library/components/utils/Expandable.vue";
import PluginInfo from "../../../library/components/plugins/PluginInfo.vue";
import PluginConfig from "../../../library/components/plugins/pluginConfig.vue";
import pluginService from "../../../library/modules/pluginService";
import PluginValidation from "../../../library/interfaces/PluginValidation";
import {RundeckBrowser} from "@rundeck/client";
import {cloneDeep} from "lodash"
import _ from 'lodash';
import {useI18n} from "vue-i18n";

const client: RundeckBrowser = getRundeckContext().rundeckClient
const rdBase = getRundeckContext().rdBase
const context = getRundeckContext()

interface PluginConf {
  type: string;
  config: any;
  project: any;
}
interface EditedProjectPluginConfigEntry {
  entry: PluginConf;
}
interface ProjectPluginConfigEntry {
  entry: PluginConf;
  extra: PluginConf;
  validation: PluginValidation;
  create?: boolean;
  origIndex?: number;
  modified: boolean;
  configSet: boolean
}

const array_clone = (arr: any[]): any[] => arr.map(val => Object.assign({}, val))
export default defineComponent({
  name: "App",
  components: {
    PluginInfo,
    PluginConfig,
    Expandable
  },
  data() {
    return {
      loaded: false,
      modified: false,
      mode: this.editMode ? "edit" : "show",
      project: "",
      rdBase: "",
      cancelUrl: "",
      contextConfig: [] as PluginConf[],
      pluginConfigs: [] as ProjectPluginConfigEntry[],
      workingData: [] as ProjectPluginConfigEntry[],
      editedPlugins:{} as {[key:string]:EditedProjectPluginConfigEntry},
      pluginData:{} as {[key:string]:PluginConf},
      configOrig: [] as any[],
      rundeckContext: {} as RundeckContext,
      modalAddOpen: false,
      pluginProviders: [] as any[],
      pluginLabels: {},
      projectSettings: {},
      editFocus: -1,
      errors: [] as string[],
      pluginStorageAccess: [] as any[]
    };
  },
  computed:{
    exportedData():any[]{
      let data = [] as any
      let inputData = this.pluginConfigs
      inputData.forEach((plugin, index) => {
        data.push({type: plugin.entry.type, config: plugin.entry.config})
      });
      return data
    },
    filteredPluginProviders(): any[] {
      return this.pluginProviders.filter((plugin) => !plugin['configSet'])
    },
  },
  props: {
    editMode: {
      type: Boolean,
      default: false
    },
    modeToggle: {
      type: Boolean,
      default: true
    },
    showEmpty: {
      type: Boolean,
      default: true
    },
    serviceName: String,
    title: {
      required: false,
      type: String
    },
    configPrefix: String,
    additionalProps: { required: false, type: Object },
    help: { required: false, type: String },
    editButtonText: { required: false, type: String },
  },
  emits: ['input', 'saved', 'plugin-storage-access'],
  methods: {
    notifyError(msg: string, args: any[]) {
      Notification.notify({
        type: "danger",
        title: "An Error Occurred",
        content: msg,
        duration: 0
      });
    },

    notifySuccess(title: string, msg: string) {
      Notification.notify({
        type: "success",
        title: title,
        content: msg,
        duration: 5000
      });
    },
    createConfigEntry(entry: any, origIndex: number): ProjectPluginConfigEntry {
      return {
        entry: { type: entry.type, config: Object.assign({}, entry.config) }
      } as ProjectPluginConfigEntry;
    },

    serializeConfigEntry(entry: ProjectPluginConfigEntry): any {
      return {
        type: entry.entry.type,
        serviceName: "PluginGroup",
        config: Object.assign({}, entry.entry.config),
      };
    },
    addPlugin(provider: string) {
      this.modalAddOpen = false;
      this.workingData.push({
        entry: { type: provider, config: {} },
        extra: { config: {} },
        create: true
      } as ProjectPluginConfigEntry);

      this.setFocus(this.workingData.length - 1);
    },
    setFocus(focus: number) {
      this.editFocus = focus;
    },
    editPlugin(index:any, plugin: ProjectPluginConfigEntry){
      this.editFocus=index
      this.editedPlugins[plugin.entry.type]= {entry: plugin.entry}
    },
    didCancel(plugin: ProjectPluginConfigEntry, index: any){
      if(this.errors.length >0 && !this.editedPlugins[plugin.entry.type]){
        this.errors=[]
        this.removePlugin(plugin, index)
      }
      else{
        this.editFocus=-1
        this.errors=[]
        const found = this.workingData.indexOf(plugin);
        if(this.editedPlugins[plugin.entry.type]){
          this.workingData[found].entry=this.editedPlugins[plugin.entry.type].entry
          this.pluginConfigs = array_clone(this.workingData)
        }
        else{
          this.removePlugin(plugin, index)
        }
      }

    },
    async savePlugin(plugin: ProjectPluginConfigEntry, index: number) {

      if(this.errors.length>0){
        this.errors=[]
      }

    if(Object.keys(plugin.entry.config).length===0){
        this.removePlugin(plugin,index)
        return
    }
      const type = plugin.entry.type
      this.pluginProviders.forEach((item: any, index: any)=> {
        if(item.name == type){
          item.configSet=true
        }

      })
      //validate
      const validation: PluginValidation = await pluginService.validatePluginConfig(
        this.serviceName,
        plugin.entry.type,
        plugin.entry.config
      );
      if (!validation.valid) {
        plugin.validation = validation;
        return;
      }
      delete plugin.validation;
      this.pluginConfigs = array_clone(this.workingData)
      plugin.create = false;
      plugin.modified = true;
      this.setFocus(-1);
      this.$emit("input", this.exportedData);
    },
    removePlugin(plugin: ProjectPluginConfigEntry, index: number) {
      const type = plugin.entry.type
      this.pluginProviders.forEach((item: any, index: any)=> {
        if(item.name == type){
          item.configSet=false
        }
      })
      const found = this.workingData.indexOf(plugin);
      this.workingData.splice(found, 1);
      this.pluginConfigs = array_clone(this.workingData)
      this.$emit("input", this.exportedData)
      this.cleanStorageAccess(plugin);
      this.setFocus(-1);
    },

    movePlugin(index: number, plugin: ProjectPluginConfigEntry, shift: number) {
      const found = this.workingData.indexOf(plugin);
      const item = this.workingData.splice(found, 1)[0];
      const newindex = found + shift;
      this.workingData.splice(newindex, 0, item);
      this.pluginConfigs = array_clone(this.workingData)
      this.editFocus = -1;
    },
    didSave(success: boolean) {
      if (this.modeToggle) {
        this.mode = "show";
      }
    },
    async savePlugins() {
        this.$emit("saved", this.pluginConfigs);
    },
    configUpdated() {
      this.modified = true;
    },
    hasKeyStorageAccess(provider: any){
      this.pluginStorageAccess.push(provider)
      this.$emit('plugin-storage-access',[provider])
    },
    cleanStorageAccess(plugin: any){
      const pluginStorageAccess = [] as any[];
      this.pluginStorageAccess.forEach((pluginAccess:any)=>{
        if(pluginAccess !== plugin.entry.type){
          pluginStorageAccess.push(pluginAccess);
        }
      });

      this.pluginStorageAccess = pluginStorageAccess;
    },
    async getPluginConfigs(){
      let projectPluginConfigList = [] as ProjectPluginConfigEntry[]
      let data = await pluginService.getPluginProvidersForService(this.serviceName)

      if (data.service) {
        this.pluginProviders = data.descriptions;
        this.pluginLabels = data.labels;

        this.contextConfig.forEach((provider2: any, index: any) => {
          let projectPluginConfig = {entry:provider2,create:true} as ProjectPluginConfigEntry
          projectPluginConfigList.push(projectPluginConfig)
          this.pluginProviders.forEach((provider: any, index: any) => {
            if (provider.name === provider2.type) {
                provider['configSet'] = true
            }
          })
        })
        this.loaded = true
      }

      this.workingData = projectPluginConfigList
      this.pluginConfigs = array_clone(projectPluginConfigList)
      this.$emit('input', this.exportedData)
    }
  },
  async mounted() {
    this.project = window._rundeck.projectName;
    const pluginGroups = window._rundeck.data.pluginGroups as PluginConf
    this.contextConfig = pluginGroups.config
    await this.getPluginConfigs()

  }
});
</script>

<style>
</style>
