<template>
    <div>
        <div class="card-header">
            <h3 class="card-title">
                {{ $t('edit.nodes.file') }}
            </h3>
            <p class="category">{{ sourceDesc }}</p>
        </div>

        <div class="card-content">

            <div class="form-group">
                <label class="control-label col-sm-2">
                    {{ $t('project.node.file.source.label') }}
                </label>

                <div class="col-sm-10">
                    <p class="form-control-static">
                        <span v-if="provider">
                            <plugin-config mode="title" service-name="ResourceModelSource" :provider="provider">
                                <template v-slot:titlePrefix>{{ index }}. </template>
                            </plugin-config>
                        </span>
                        <span v-else>
                            {{ index }}.
                        </span>
                    </p>
                </div>
            </div>


            <div class="form-group">
                <label class="control-label  col-sm-2">
                    {{ $t('file.display.format.label') }}
                </label>

                <div class="col-sm-10">
                    <p class="form-control-static"><code>{{ fileFormat }}</code></p>
                </div>
            </div>
            <div v-if="sourceDesc">
                <div class="form-group">
                    <label class="control-label  col-sm-2">
                        {{ $t('project.node.file.source.description.label') }}
                    </label>

                    <div class="col-sm-10">
                        <p class="form-control-static text-info">{{ sourceDesc }}</p>
                    </div>
                </div>
            </div>
            <ui-socket section="edit-project-node-source-file" location="editor">
                <ace-editor :soft-wrap-control="true"
                            :lang="fileFormat"
                            height="500"
                            :code-syntax-selectable="!fileFormat"
                            v-model="valueInternal"/>

            </ui-socket>
            <div v-if="errorMessage">
                <h3>
                    {{ $t('project.nodes.edit.save.error.message') }}
                </h3>
                <div class="text-warning">{{ errorMessage }}</div>
            </div>
            <div v-if="fileEmpty">
                <div class="text-warning">
                    {{ $t('project.nodes.edit.empty.description') }}
                </div>
            </div>
        </div>


        <div class="card-footer">
            <btn name="cancel" class="reset_page_confirm" @click="$emit('cancel')"  :disabled="saving">
                {{ $t('button.action.Cancel') }}
            </btn>
            <btn name="save" class="btn-cta reset_page_confirm" @click="$emit('save',valueInternal)" :disabled="saving">
                {{ $t('button.action.Save') }}
            </btn>

            <page-confirm class="text-warning footer-text" :message="$t('page.unsaved.changes')" :event-bus="eventBus"
                          :display="true"/>
        </div>
    </div>

</template>
<script lang="ts">
import {defineComponent, PropType} from 'vue'
import PluginConfig from '../../../library/components/plugins/pluginConfig.vue'
import AceEditor from '../../../library/components/utils/AceEditor.vue'
import UiSocket from '../../../library/components/utils/UiSocket.vue'
import PageConfirm from '../../../library/components/utils/PageConfirm.vue'
import {EventBus} from "../../../library";

export default defineComponent({
  components: {PluginConfig, AceEditor, UiSocket, PageConfirm},
  props: {
    eventBus: {type: Object as PropType<typeof EventBus>, required: true},
    index: {type: Number, required: true},
    sourceDesc: {type: String, default: '', required: false},
    fileFormat: {type: String, default: '', required: false},
    provider: {type: String, default: '', required: false},
    value: {type: String, required: true},
    errorMessage: {type: String, default: ''},
    saving: {type: Boolean, default: false}
  },
  emits: ['cancel', 'save'],
  data() {
    return {
      valueInternal: '',
    }
  },
  computed: {
    fileEmpty() {
      return !this.value
    }
  },
  watch: {
    value(newVal) {
      this.valueInternal = newVal
    },
    valueInternal(newVal) {
      this.eventBus.emit('node-source-file-set-content', {content: newVal})
    }
  }
})
</script>
<style lang="scss" scoped>
.btn + .footer-text {
  margin-left: 1em;
}
</style>