import Vue from 'vue'

export default {
    title: 'Class Components'
}

export const typography = () => (Vue.extend({
    render(h) {
        return (
            <div>
                <div class="h1">H1 Heading</div>
                <div class="h2">H2 Heading</div>
                <div class="h3">H3 Heading</div>
                <div class="h4">H4 Heading</div>
                <div class="h5">H5 Heading</div>
            </div>
        )
    }
}))

export const buttons = () => (Vue.extend({
    render(h) {
        return (
            <div style="max-width: 500px; margin-top: 10px;">
                <div style="display:flex;justify-content: space-evenly;">
                    <a class="btn btn-default" role="button">Link</a>
                    <button class="btn btn-default">Button</button>
                    <input class="btn btn-default" value="Input"/>
                </div>
                <div style="display: flex;justify-content: space-evenly; margin-top: 10px;">
                    <button class="btn btn-default">Default</button>
                    <button class="btn btn-primary">Primary</button>
                    <button class="btn btn-info">Info</button>
                    <button class="btn btn-success">Success</button>
                    <button class="btn btn-warning">Warning</button>
                    <button class="btn btn-danger">Danger</button>
                </div>
            </div>
        )
    }
}))

export const pagination = () => (Vue.extend({
    render(h) {
        return (
            <ul data-v-06f48450="" class="pagination pagination-sm">
                <li data-v-06f48450="" class="disabled">
                    <a data-v-06f48450="" href="#" title="Previous Page" class="page_nav_btn"><i data-v-06f48450="" class="glyphicon glyphicon-arrow-left"></i></a>
                </li>
                <li data-v-06f48450="" class="active">
                    <span data-v-06f48450="" title="Page 1">1</span>
                </li>
                <li data-v-06f48450="" class="">
                    <a data-v-06f48450="" href="#" title="Page 2" class="page_nav_btn">2</a></li><li data-v-06f48450="" class=""><a data-v-06f48450="" href="#" title="Page 3" class="page_nav_btn">3</a></li><li data-v-06f48450="" class=""><a data-v-06f48450="" href="#" title="Page 4" class="page_nav_btn">4</a></li><li data-v-06f48450="" class=""><a data-v-06f48450="" href="#" title="Page 5" class="page_nav_btn">5</a></li><li data-v-06f48450="" class=""><a data-v-06f48450="" href="#" title="Page 6" class="page_nav_btn">6</a></li><li data-v-06f48450="" class=""><a data-v-06f48450="" href="#" title="Page 7" class="page_nav_btn">7</a></li><li data-v-06f48450="" class=""><a data-v-06f48450="" href="#" title="Page 8" class="page_nav_btn">8</a></li><li data-v-06f48450="" class=""><a data-v-06f48450="" href="#" title="Next Page" class="page_nav_btn"><i data-v-06f48450="" class="glyphicon glyphicon-arrow-right"></i></a></li></ul>
        )
    }
}))

export const cards = () => (Vue.extend({
    render(h) {
        return (
            <div style="padding: 20px; max-width: 500px">
                <div class="card">
                    <div class="card-content" style="padding-bottom: 20px;">
                    <span class="h3 text-primary">
                        <span data-bind="messageTemplate: projectNamesTotal, messageTemplatePluralize:true">220 Projects
                        </span>
                    </span>
                    
                        <a href="/resources/createProject" class="btn  btn-success pull-right">
                            New Project
                            <b class="glyphicon glyphicon-plus"></b>
                        </a>
                    
                    </div>
                </div>
            </div>
        )
    }
}))