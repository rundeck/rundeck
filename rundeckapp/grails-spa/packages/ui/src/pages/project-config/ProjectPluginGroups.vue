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
            v-for="(plugin,index) in pluginConfigs"
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
              <div slot="extra" class="row" v-if="mode==='edit'">
                <div class="col-xs-12 col-sm-12">
                  <span v-if="editFocus===-1">
                    <a
                      class="btn btn-default btn-xs"
                      @click="editFocus=index"
                      :key="'edit'"
                    >{{$t('Edit')}}</a>
                  </span>
                  <span v-if="editFocus===index">
                    <a
                      class="btn btn-cta btn-xs"
                      @click="savePlugin(plugin,index)"
                      :key="'save'"
                    >{{$t('Save')}}</a>
                    <a class="btn btn-default btn-xs" @click="editFocus=-1">{{$t('Cancel')}}</a>
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
            </plugin-config>
            <slot name="item-extra" :plugin="plugin" :editFocus="editFocus===index" :mode="mode"></slot>
          </div>
          <div class="list-group-item" v-if="pluginConfigs.length<1 && showEmpty">
            <slot name="empty-message">
              <span  class="text-muted">
                {{$t("No Plugin Groups Configured",serviceName)}}
              </span>
            </slot>
          </div>
        </div>
        <div class="card-footer" v-if="mode==='edit' && editFocus===-1">
          <btn type="primary" @click="modalAddOpen=true">
            {{serviceName}}
            <i class="fas fa-plus"></i>
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
                v-for="plugin in pluginProviders"
                v-if="!plugin['configSet']"
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
            <div slot="footer">
              <btn @click="modalAddOpen=false">{{$t('Cancel')}}</btn>
            </div>
          </modal>
        </div>
        <div class="card-footer" v-if="mode==='edit' && editFocus===-1">
<!--          <btn type="default" @click="cancelAction" v-if="modeToggle">{{$t('Cancel')}}</btn>-->
<!--          <btn type="default" @click="cancelAction" v-else-if="modified">{{$t('Revert')}}</btn>-->
<!--          <a class="btn btn-cta" @click="savePlugins" v-if="modified" href="#">{{$t('Save')}}</a>-->
          <span class="text-warning" v-if="modified">Changes have not been saved.</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import axios from "axios";
import Vue from "vue";
import { Notification } from "uiv";
import { getRundeckContext, RundeckContext } from "@rundeck/ui-trellis";
import Expandable from "@rundeck/ui-trellis/lib/components/utils/Expandable.vue";
import PluginInfo from "@rundeck/ui-trellis/lib/components/plugins/PluginInfo.vue";
import PluginConfig from "@rundeck/ui-trellis/lib/components/plugins/pluginConfig.vue";
import pluginService from "@rundeck/ui-trellis/lib/modules/pluginService";
import PluginValidation from "@rundeck/ui-trellis/lib/interfaces/PluginValidation";
import {RundeckBrowser} from "@rundeck/client";

const client: RundeckBrowser = getRundeckContext().rundeckClient
const rdBase = getRundeckContext().rdBase
const context = getRundeckContext()

interface PluginConf {
  type: string;
  config: any;
  project: any;
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
export default Vue.extend({
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
      pluginData:{} as {[key:string]:PluginConf},
      configOrig: [] as any[],
      rundeckContext: {} as RundeckContext,
      modalAddOpen: false,
      pluginProviders: [] as any,
      pluginLabels: {},
      projectSettings: {},
      editFocus: -1,
      errors: [] as string[],
      pluginStorageAccess: [] as any[]
    };
  },
  computed:{
    exportedData():any[]{
      let data = []
      let inputData = this.pluginConfigs
      for (let key in inputData) {
        data.push({type: key, config: inputData[key].entry.config})
      }
      return data
    }
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
      this.pluginConfigs.push({
        entry: { type: provider, config: {} },
        extra: { config: {} },
        create: true
      } as ProjectPluginConfigEntry);

      this.setFocus(this.pluginConfigs.length - 1);
    },
    setFocus(focus: number) {
      this.editFocus = focus;
    },
    async savePlugin(plugin: ProjectPluginConfigEntry, index: number) {
      this.$emit("saved", this.pluginConfigs);
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
        Vue.set(plugin, "validation", validation);
        return;
      }
      Vue.delete(plugin, "validation");
      plugin.create = false;
      plugin.modified = true;
      this.setPluginConfigsModified();
      this.setFocus(-1);
    },
    removePlugin(plugin: ProjectPluginConfigEntry, index: string) {
      this.$emit("deleted", this.pluginConfigs)
      const type = plugin.entry.type
      this.pluginProviders.forEach((item: any, index: any)=> {
        if(item.name == type){
          item.configSet=false
        }

      })
      const found = this.pluginConfigs.indexOf(plugin);
      this.pluginConfigs.splice(found, 1);
      this.setPluginConfigsModified()
      this.cleanStorageAccess(plugin);
      this.setFocus(-1);
    },

    movePlugin(index: number, plugin: ProjectPluginConfigEntry, shift: number) {
      const found = this.pluginConfigs.indexOf(plugin);
      const item = this.pluginConfigs.splice(found, 1)[0];
      const newindex = found + shift;
      this.pluginConfigs.splice(newindex, 0, item);
      this.setPluginConfigsModified();
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
    setPluginConfigsModified() {
      this.modified = true;
      this.$emit("modified");
      this.notifyPluginConfigs();
    },
    pluginConfigsModified() {
      if (this.loaded) {
        this.setPluginConfigsModified();
      }
    },
    pluginConfigsModifiedReset() {
      this.modified = false;
      this.$emit("reset");
      this.notifyPluginConfigs();
    },
    notifyPluginConfigs(){
      this.$emit("plugin-configs-data",this.pluginConfigs);
    },
    configUpdated() {
      this.pluginConfigsModified();
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
    getPluginConfigs(){
      const projectPluginConfigList = [] as ProjectPluginConfigEntry[]
      pluginService
        .getPluginProvidersForService(this.serviceName)
        .then(data => {
          if (data.service) {
            this.pluginProviders = data.descriptions;
            this.pluginLabels = data.labels;

              this.contextConfig.forEach((provider2: any, index: any) => {

                    let projectPluginConfig = {} as ProjectPluginConfigEntry
                    let configSet = false
                    Object.values(provider2.config).forEach((entry: any)=> {
                      if(entry!=null){
                        projectPluginConfig.entry=provider2
                        configSet=true
                        projectPluginConfig.configSet=true
                        projectPluginConfig.create=true
                        this.loaded = true;
                        this.notifyPluginConfigs();
                      }
                      else{
                        projectPluginConfig.configSet=false
                      }
                    })
                    if(projectPluginConfig.configSet){
                      projectPluginConfigList.push(projectPluginConfig)
                      this.pluginProviders.forEach((provider: any, index: any)=> {
                        console.log(provider.name)
                        console.log(provider2.type)
                        if(provider.name===provider2.type){
                          console.log("entered in")
                          this.pluginProviders[index]["configSet"]=true
                        }
                      })
                    }
              })
          }
        }).catch(error => console.error(error));
      this.pluginConfigs = projectPluginConfigList
      console.log("pluginconfigs")
      console.log(this.pluginConfigs)
    }
  },
  async mounted() {
    this.project = window._rundeck.projectName;
    console.log("made it here")
    const pluginGroups = window._rundeck.data.pluginGroups as PluginConf
    this.contextConfig = pluginGroups.config
    console.log(this.contextConfig)
    await this.getPluginConfigs()

  }
});
</script>

<style>
</style>
