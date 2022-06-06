import Vue from 'vue'
import { addons } from '@storybook/addons'


export default {
    title: 'Layouts/Base'
}

export const base = () => (Vue.extend({
    render(h) {
        return (
            <section class="layout-base">
                <div class="layout-base--header">
                    <div class="layout-base--content">
                        <h1>Runners</h1>
                        <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam faucibus auctor nunc, vel rhoncus nulla consectetur et. Aliquam ac laoreet turpis, eu pharetra mauris. Suspendisse imperdiet feugiat elementum.</p>
                    </div>
                </div>
                <div class="layout-base--body">
                    <div class="layout-base--content">
                        <div class="card">
                            Sample
                        </div>
                    </div>
                </div>
            </section>
        )
    }
}))

export const baseWithAction = () => (Vue.extend({
    render(h) {
        return (
            <section class="layout-base">
                <div class="layout-base--header">
                    <div class="layout-base--content container--flex">
                        <div class="flex--grow">
                            <h1>Runners</h1>
                            <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam faucibus auctor nunc, vel rhoncus nulla consectetur et. Aliquam ac laoreet turpis, eu pharetra mauris. Suspendisse imperdiet feugiat elementum.</p>
                        </div>
                        <div class="layout-base--action flex--none">
                            <a href="" class="btn btn-primary">button</a>
                        </div>
                    </div>
                </div>
                <div class="layout-base--body">
                    <div class="layout-base--content">
                        <div class="card">
                            Sample
                        </div>
                    </div>
                </div>
            </section>
        )
    }
}))

export const baseWithMulitpleAction = () => (Vue.extend({
    render(h) {
        return (
            <section class="layout-base">
                <div class="layout-base--header">
                    <div class="layout-base--content container--flex">
                        <div class="flex--grow">
                            <h1>Runners</h1>
                            <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam faucibus auctor nunc, vel rhoncus nulla consectetur et. Aliquam ac laoreet turpis, eu pharetra mauris. Suspendisse imperdiet feugiat elementum.</p>
                        </div>
                        <div class="layout-base--action flex--none">
                            <a href="" class="btn btn-primary">button</a>
                            <a href="" class="btn btn-cta">button</a>
                        </div>
                    </div>
                </div>
                <div class="layout-base--body">
                    <div class="layout-base--content">
                        <div class="card">
                            Sample
                        </div>
                    </div>
                </div>
            </section>
        )
    }
}))

export const baseWithBreadcrumbBar = () => (Vue.extend({
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
                <section class="layout-base">
                    <div class="layout-base--header">
                        <div class="layout-base--content container--flex">
                            <div class="flex--grow">
                                <h1>Runners</h1>
                                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam faucibus auctor nunc, vel rhoncus nulla consectetur et. Aliquam ac laoreet turpis, eu pharetra mauris. Suspendisse imperdiet feugiat elementum.</p>
                            </div>
                            <div class="layout-base--action flex--none">
                                <a href="" class="btn btn-primary">button</a>
                                <a href="" class="btn btn-cta">button</a>
                            </div>
                        </div>
                    </div>
                    <div class="layout-base--body">
                        <div class="layout-base--content">
                            <div class="card">
                                Sample
                            </div>
                        </div>
                    </div>
                </section>
            </div>
        )
    }
}))

export const baseBodyGrid = () => (Vue.extend({
    render(h) {
        return (
            <div>
                <section class="layout-base">
                    <div class="layout-base--header">
                        <div class="layout-base--content container--flex">
                            <div class="flex--grow">
                                <h1>Runners</h1>
                                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam faucibus auctor nunc, vel rhoncus nulla consectetur et. Aliquam ac laoreet turpis, eu pharetra mauris. Suspendisse imperdiet feugiat elementum.</p>
                            </div>
                            <div class="layout-base--action flex--none">
                                <a href="" class="btn btn-primary">button</a>
                                <a href="" class="btn btn-cta">button</a>
                            </div>
                        </div>
                    </div>
                    <div class="layout-base--body">
                        <div class="layout-base--content">
                            <div class="card">
                                Sample
                            </div>
                            <div class="grid-2">
                                <div class="card">
                                    Sample
                                </div>
                                <div class="card">
                                    Sample
                                </div>
                            </div>
                            <div class="grid-3">
                                <div class="card">
                                    Sample
                                </div>
                                <div class="card">
                                    Sample
                                </div>
                                <div class="card">
                                    Sample
                                </div>
                            </div>
                            <div class="grid-4">
                                <div class="card">
                                    Sample
                                </div>
                                <div class="card">
                                    Sample
                                </div>
                                <div class="card">
                                    Sample
                                </div>
                                <div class="card">
                                    Sample
                                </div>
                            </div>
                            <div class="grid-4">
                                <div class="card col-span-2">
                                    Sample
                                </div>
                                <div class="card">
                                    Sample
                                </div>
                                <div class="card">
                                    Sample
                                </div>
                            </div>
                            <div class="grid-3">
                                <div class="card">
                                    Sample
                                </div>
                                <div class="card col-span-2">
                                    Sample
                                </div>
                            </div>
                        </div>
                    </div>
                </section>
            </div>
        )
    }
}))