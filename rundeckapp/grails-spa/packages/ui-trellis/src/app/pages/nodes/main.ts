import {defineComponent, markRaw} from 'vue'
import {getRundeckContext} from '../../../library'
import {NodeFilterStore} from '../../../library/stores/NodeFilterStore'
import NodeFilterInput from '../../components/job/resources/NodeFilterInput.vue'
import NodeCard from "../../components/job/resources/NodeCard.vue";

let rundeckContext = getRundeckContext();
const FilterInputComp = defineComponent(
    {
        name: 'NodeFilter',
        data() {
            return {
                project: rundeckContext.projectName,
                filterValue: this.itemData['filter'],
                showInputTitle: this.itemData['showInputTitle'],
                autofocus: this.itemData['autofocus'],
                filterFieldName: this.itemData['filterFieldName'] || 'filter',
                filterFieldId: this.itemData['filterFieldId'],
                queryFieldPlaceholderText: this.itemData['queryFieldPlaceholderText'],
                koFieldName: this.itemData['koFieldName'],
                koParam: this.itemData['koParam'],
                subs: []
            }
        },
        props: ['itemData', 'extraAttrs'],
        inject: ['addUiMessages'],
        components: {NodeFilterInput},
        template: `
          <node-filter-input :project="project"
                             v-model="filterValue"
                             :show-title="showInputTitle"
                             :autofocus="autofocus"
                             :filterFieldName="filterFieldName"
                             :filter-field-id="filterFieldId"
                             :query-field-placeholder-text="queryFieldPlaceholderText"
                             search-btn-type="cta"
                             @update:model-value="updatedValue"
                             @filter="filterClicked"
                             v-bind="extraAttrs"
          />
        `,
        computed: {
            isNodeStoreAvailable() {
                return !!this.extraAttrs.nodeFilterStore
            }
        },
        methods: {
            updatedValue(val: string) {
                this.nodeFilterKo()?.selectNodeFilter({filter: val}, false);
                if(this.isNodeStoreAvailable) {
                    this.extraAttrs.nodeFilterStore.setSelectedFilter(val)
                }
            },
            filterClicked(filter: any) {
                this.nodeFilterKo()?.selectNodeFilter(filter, false)
                if(this.isNodeStoreAvailable) {
                    this.extraAttrs.nodeFilterStore.setSelectedFilter(filter.filter)
                }
            },
            nodeFilterKo() {
                //@ts-ignore
                if(this.koFieldName && this.koParam && window[this.koFieldName] && window[this.koFieldName][this.koParam]){
                    return window[this.koFieldName][this.koParam]
                }else if(this.koFieldName && window[this.koFieldName]){
                    return window[this.koFieldName]
                }else if(!this.koFieldName){
                    //@ts-ignore
                    return window.nodeFilter
                }
            },
            attachKnockout(retry: number) {
                //set up reactive connection to existing Knockout
                //@ts-ignore
                if (this.nodeFilterKo()) {
                    this.subs.push(this.nodeFilterKo().filter.subscribe((val: string) => this.filterValue = val))
                    this.filterValue = this.nodeFilterKo().filter()
                } else if (retry > 0) {
                    setTimeout(() => this.attachKnockout(retry - 1), 1000)
                }else{
                    console.log('Did not find ko component: ', this.koFieldName, this.koParam)
                }
            }
        },
        beforeDestroy() {
            //note: this removes subscriptions from knockout observable
            //@ts-ignore
            this.subs.forEach(s => s.dispose())
        },
        mounted() {

            this.attachKnockout(5)
            if(this.isNodeStoreAvailable){
                this.filterValue = this.extraAttrs.nodeFilterStore.selectedFilter
            }

        }
    }
)
function init() {
    rundeckContext.rootStore.ui.addItems([
        {
            section: 'nodes-page',
            location: 'node-filter-input',
            visible: true,
            widget: markRaw(defineComponent(
                {
                    data() {
                        return {
                            project: rundeckContext.projectName,
                        }
                    },
                    props: ['itemData'],
                    components: {FilterInputComp},
                    template: `
                      <filter-input-comp :project="project"
                                         :item-data="itemData"
                                         :extra-attrs="{'class':'subtitle-head-item','style':'margin-bottom:0;'}"
                      />
                    `,
                }
            ))
        },
        {
            section: 'nodes-page',
            location: 'main',
            visible: true,
            widget: markRaw(defineComponent(
                {
                    data() {
                        return {
                            project: rundeckContext.projectName,
                            nodeFilterStore: new NodeFilterStore(),
                        }
                    },
                    props: ['itemData'],
                    components: { FilterInputComp, NodeCard },
                    methods: {
                        updateNodeFilter(val: any) {
                            const filterName = val.filter || val
                            this.nodeFilterStore.setSelectedFilter(filterName)
                        }
                    },
                    template: `
                        <div class="title">
                          <span class="text-h3"><i class="fas fa-sitemap"></i> {{ $t('gui.menu.Nodes') }}</span>
                        </div>
                        <div style="margin-bottom:20px">
                          <filter-input-comp
                              v-model="nodeFilterStore.selectedFilter"
                              :project="project"
                              :item-data="itemData"
                              :extra-attrs="{'class':'subtitle-head-item','style':'margin-bottom:0;', 'nodeFilterStore': nodeFilterStore}"
                          />
                        </div>
                        <div id="nodesContent">
                          <div class="container-fluid">
                            <div class="row">
                              <div class="col-xs-12">
                                <node-card
                                    :node-filter-store="nodeFilterStore"
                                    :job-create-authorized="itemData.jobCreateAuthorized"
                                    :run-authorized="itemData.runAuthorized"
                                    @filter="updateNodeFilter"
                                >
                                </node-card>
                              </div>
                            </div>
                          </div>
                        </div>
                    `,
                }
            ))
        },
        {
            section: 'adhoc-command-page',
            location: 'node-filter-input',
            visible: true,
            widget: markRaw(defineComponent(
                {
                    data() {
                        return {
                            project: rundeckContext.projectName,
                        }
                    },
                    props: ['itemData'],
                    components: {FilterInputComp},
                    template: `
                      <filter-input-comp :project="project"
                                         :item-data="itemData"
                                         :extra-attrs="{'class':'input-group-lg tight'}"
                      />
                    `,
                }
            ))
        },
        {
            section: 'job-show-page',
            location: 'exec-options-node-filter-input',
            visible: true,
            widget: markRaw(defineComponent(
                {
                    data() {
                        return {
                            project: rundeckContext.projectName,
                        }
                    },
                    props: ['itemData'],
                    components: {FilterInputComp},
                    template: `
                      <filter-input-comp :project="project" :item-data="itemData"/>
                    `,
                }
            ))
        },
        {
            section: 'job-wfitem-edit',
            location: 'jobref-node-filter-input',
            visible: true,
            widget: markRaw(defineComponent(
                {
                    data() {
                        return {
                            project: rundeckContext.projectName,
                        }
                    },
                    props: ['itemData'],
                    components: {FilterInputComp},
                    template: `
                      <filter-input-comp :project="project" :item-data="itemData"/>
                    `,
                }
            ))
        }
    ])
}
window.addEventListener('DOMContentLoaded', init)