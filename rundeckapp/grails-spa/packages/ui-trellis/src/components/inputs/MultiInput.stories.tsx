import Vue from 'vue'
import { addons } from '@storybook/addons'
import { CHANGE, array, object, boolean, withKnobs, select } from '@storybook/addon-knobs'


export default {
    title: 'Inputs/MultiInput',
    decorators: [withKnobs({ disableDebounce: true })]
}

export const MultiInput = () => (Vue.extend({
    render(h) {
        return (
            <div class="subtitle-head-item input-group multiple-control-input-group input-group-lg" style="margin-bottom:0;">
                <div class="subtitle-head-item input-group multiple-control-input-group input-group-lg" style="margin-bottom:0;">

                    <span class="input-group-addon input-group-addon-title">Nodes</span>

                    <div class="input-group-btn">
                        <button type="button" class="btn btn-default dropdown-toggle" data-bind="css: { 'btn-success': filterName(), 'btn-default': !filterName() }" data-toggle="dropdown" aria-expanded="false">
                            <span data-bind="text: filterNameDisplay() || ''"></span> <span class="caret"></span>
                        </button>
                        <ul class="dropdown-menu">
                            <li>
                                <a href="/resources/nodes?filterName=.*" class="nodefilterlink" data-node-filter-name=".*" data-node-filter=".*" data-bind="css: { active: '.*'== filterName() }">
                                    <i class="fas fa-asterisk"></i>
                                    Show all nodes
                                </a>
                            </li>

                            <li class="divider"></li>

                            <li class="dropdown-header" data-bind="visible: filterName()" style="display: none;">
                                Filter:<span data-bind="text: filterNameDisplay()"></span>
                            </li>
                            <li data-bind="visible: canSaveFilter" style="display: none;">
                                <a href="#" data-toggle="modal" data-target="#saveFilterModal">
                                    <i class="glyphicon glyphicon-plus"></i>
                                    Save Filter
                                </a>
                            </li>
                            <li data-bind="visible: canDeleteFilter" style="display: none;">
                                <a href="#" class="" data-bind="click: deleteFilter">
                                    <i class="glyphicon glyphicon-remove"></i>
                                    Delete this Filter …
                                </a>
                            </li>
                            <li data-bind="visible: canSetDefaultFilter" style="display: none;">
                                <a href="#" class="" data-bind="click: setDefaultFilter">
                                    <i class="glyphicon glyphicon-filter"></i>
                                    Set as Default Filter
                                </a>
                            </li>
                            <li data-bind="visible: canRemoveDefaultFilter" style="display: none;">
                                <a href="#" class="" data-bind="click: nodeSummary().removeDefault">
                                    <i class="glyphicon glyphicon-ban-circle"></i>
                                    Remove Default Filter
                                </a>
                            </li>

                        </ul>
                    </div>

                    <input type="search" name="filter" class="schedJobNodeFilter form-control" autofocus="" data-bind="textInput: filterWithoutAll,  executeOnEnter: newFilterText" placeholder="Enter a node filter, or .* for all nodes" value="" id="schedJobNodeFilter" />
                    <div class ="input-group-btn">
                    <a class ="btn btn-default" tabindex="0" role="button" data-toggle="popover" data-popover-content-ref="#queryFilterHelp" data-placement="bottom" data-trigger="focus" data-container="body" data-popover-template-class ="popover-wide" data-original-title="" title="">
                    <i class ="glyphicon glyphicon-question-sign"></i>
                    </a>
                    <a class ="btn btn-primary btn-fill disabled" data-bind="click: $data.newFilterText, css: {disabled: !filter()}" href="#">
                    Search
                    </a>
                    </div>

                </div>

            </div>
        )
    }
}))
