import {defineComponent, markRaw} from 'vue'
import {getRundeckContext} from '../../../library'
import NodeFilterInput from '../../components/job/resources/NodeFilterInput.vue'
import NodeFilterSummaryList from '../../components/job/resources/NodeFilterSummaryList.vue'

let rundeckContext = getRundeckContext()
let eventBus = rundeckContext.eventBus
const FilterInputComp = defineComponent(
    {
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
        methods: {
            updatedValue(val: string) {
              if(val) {
                this.nodeFilterKo()?.selectNodeFilter({filter: val}, false)
              }
            },
            filterClicked(filter: any) {
                this.nodeFilterKo()?.selectNodeFilter(filter, false)
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
            location: 'node-filter-summary-list',
            visible: true,
            widget: markRaw(defineComponent(
              {
                data() {
                  return {
                    project: rundeckContext.projectName,
                    nodesBaseUrl: rundeckContext.rdBase + '/project/' + rundeckContext.projectName + '/nodes'
                  }
                },
                components: {NodeFilterSummaryList},
                template: `
                  <node-filter-summary-list
                    :project="project"
                    :nodesBaseUrl="nodesBaseUrl"
                  />
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