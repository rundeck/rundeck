import Vue from 'vue'
import { addons } from '@storybook/addons'


export default {
    title: 'Layouts/Wizards'
}

export const Wizard = () => (Vue.extend({
    render(h) {
        return (
            <div>
                <section class="breadcrumb-bar">
                    <div class="breadcrumb-bar--navs">
                        <div class="breadcrumb-bar--nav_item">
                            Runner MGMT
                        </div>
                        <div class="breadcrumb-bar--nav_item">
                            Edit Runner
                        </div>
                    </div>
                </section>
                <section class="layout-wizard">
                    <div class="layout-wizard--header">
                        <h1>Create Runner</h1>
                        <p>Create a Runner that can receive work</p>
                    </div>
                    <div class="layout-wizard--body">
                        <div class="input-text--horizontal">
                            <div class="col-span-2">
                                <label class="label">Name</label>
                                <p class="description">Give the runner an easily identifiable name</p>
                            </div>
                            <div class="col-span-3 flex-v-center form-group">
                                <input type="text" class="form-control" />
                            </div>
                        </div>
                        <div class="input-text--horizontal">
                            <div class="col-span-2">
                                <label class="label">Name</label>
                                <p class="description">Give the runner an easily identifiable name</p>
                            </div>
                            <div class="col-span-3 flex-v-center form-group">
                                <input type="text" class="form-control" />
                            </div>
                        </div>
                        <div class="input-text--horizontal">
                            <div class="col-span-2">
                                <label class="label">Name</label>
                                <p class="description">Give the runner an easily identifiable name</p>
                            </div>
                            <div class="col-span-3 flex-v-center form-group">
                                <input type="text" class="form-control" />
                            </div>
                        </div>
                    </div>
                    <div class="layout-wizard--footer">
sd
                    </div>
                </section>
            </div>
        )
    }
}))

export const WizardWithTabs = () => (Vue.extend({
    render(h) {
        return (
            <div>
                <section class="breadcrumb-bar">
                    <div class="breadcrumb-bar--navs">
                        <div class="breadcrumb-bar--nav_item">
                            Runner MGMT
                        </div>
                        <div class="breadcrumb-bar--nav_item">
                            Edit Runner
                        </div>
                    </div>
                </section>
                <section class="layout-wizard">
                    <div class="layout-wizard--header">
                        <h1>Create Runner</h1>
                        <p>Create a Runner that can receive work</p>
                    </div>
                    <div class="layout-wizard--body-tab">
                        <div class="input-text--horizontal">
                            <div class="col-span-2">
                                <label class="label">Name</label>
                                <p class="description">Give the runner an easily identifiable name</p>
                            </div>
                            <div class="col-span-3 flex-v-center form-group">
                                <input type="text" class="form-control" />
                            </div>
                        </div>
                        
                    </div>
                    <div class="layout-wizard--footer">
                        sd
                    </div>
                </section>
            </div>
        )
    }
}))
