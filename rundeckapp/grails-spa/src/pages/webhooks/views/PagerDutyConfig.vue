<template>
    <div>
        <div>
            <h2 style="display:inline;">Rules</h2>
            <div style="display:inline;">
                <a
                class="btn btn-md btn-success"
                @click="addNewRule"
                >Add</a>
            </div>
        </div>
       
        <div class="panel panel-default" v-for="(rulesSet, index) in config.rules" :key="index">
            <div class="panel-heading">
                <span class="h3">Rule Set</span>
                <div style="display:inline;"><a
                  @click="handleDeleteRuleSet(rulesSet)"
                  class="btn btn-md btn-danger"
                >Delete</a></div>
            </div>
            <div class="panel-body">
                <div class="form-group">
                    <label class="control-label">JobID</label>
                    <input class="form-control" type="text" v-model="rulesSet.jobId" placeholder="Job ID">

                    <label class="control-label">Job Options</label>
                    <input class="form-control" type="text" v-model="rulesSet.jobOpts" placeholder="Job Options">

                    <label class="control-label">Node Filter</label>
                    <input class="form-control" type="text" v-model="rulesSet.nodeFilter" placeholder="Node Filter">

                </div>
                
                <div>
                    <h4 class="card-title" style="display: inline;">Conditions</h4>
                    <div style="display:inline;">
                        <a
                        class="btn btn-md btn-success"
                        @click="addNewCondition(rulesSet)"
                        >Add</a>
                    </div>
                </div>
                <label class="control-label">Policy</label>
                <select class="select form-control" v-model="rulesSet.policy">
                    <option>all</option>
                    <option>any</option>
                </select>
                <div v-for="(condition,index) in rulesSet.conditions" :key="index">
                    <div class="form-inline"  style="width:100%">
                        <div class="form-group" style="width:100%">
                            <input class="form-control" type="text" v-model="condition.path" placeholder="Path" style="width: 30%">

                            <select class="form-control" v-model="condition.condition">
                                <option>contains</option>
                                <option>matches</option>
                            </select>

                            <input class="form-control" type="text" v-model="condition.value" placeholder="Value">

                            <div class="btn btn-md btn-danger"
                                @click="handleDeleteCondition(rulesSet,condition)">
                                    Delete
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
export default {
    name: "PagerDutyConfig",
    props: {
        selectedPlugin: Object,
        curHook: Object,
    },
    watch: {
        // Ensure the config gets setup if the parent changes the hook
        curHook: function(newVal, oldVal) {
            this.init()
        }
    },
    data() {
        return {
            webhooks: [],
            webhookPlugins: [],
            config: null,
        }
    },
    created: function() {
        this.init()
    },

    methods: {
        /**
         * Construct the config template if not set
         */
        init() {
            console.log(this.curHook)
            if (this.curHook.config == undefined) {
                this.curHook.config = {
                    rules: [{
                        jobId: '',
                        policy: '',
                        jobOpts: '',
                        nodeFilter: '',
                        conditions: []
                    }]
                }
            }
            this.config = this.curHook.config
            console.log(this.config)
        },
        setSelectedPlugin() {
           this.selectedPlugin = this.webhookPlugins.find(p => p.name === this.curHook.eventPlugin)
        },
        addNewRule() {
            if (! this.config.rules instanceof Array)
                this.config.rules = []

            this.config.rules.push({
                jobId: '',
                policy: 'any',
                conditions: []
            })
        },
        addNewCondition(rule) {
            rule.conditions.push({})
            console.log(rule.conditions)
        },
        handleDeleteRuleSet(prop) {
            let index = this.config.rules.indexOf(prop)
            if (index >  -1)
                this.config.rules.splice(index, 1)
        },
        handleDeleteCondition(prop, condition) {
            let index = prop.conditions.indexOf(condition)
            if (index >  -1)
                prop.conditions.splice(index, 1)
        }
    }
}
</script>