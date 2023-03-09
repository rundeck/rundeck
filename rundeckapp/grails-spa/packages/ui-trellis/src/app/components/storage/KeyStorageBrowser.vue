<template>
  <div class="alert alert-warning" data-bind="visible: errorMsg()">
    <span data-bind="text: errorMsg"></span>
  </div>
  <div class="row text-info ">
    <div class="form-group col-sm-12" data-bind="css: invalid()?'has-error':'' ">
      <div class="input-group">
        <div class="input-group-addon rs-tooltip bottom" data-bind="if: staticRoot()">
        <span data-bind="text: rootBasePath()"
              class="text-ellipsis"
              style="
          max-width: 300px;
          display: inline-block;"
        >
        </span>
          <div class="rs-tooltip--message" data-bind="text: rootBasePath()" ></div>


        </div>
        <input type="text" class="form-control" style="padding-left:18px" data-bind="value: inputPath, valueUpdate: 'input', attr: {disabled: loading() }, executeOnEnter: browseToInputPath" placeholder="Enter a path"/>

        <!-- ko if: jumpLinks().length>0 -->
        <div class="input-group-btn">
          <button type="button" class="btn btn-default dropdown-toggle"
                  data-toggle="dropdown" aria-haspopup="true"
                  aria-expanded="false">
            <span data-bind="text: linksTitle"></span>
            <span class="caret"></span>
          </button>
          <ul class="dropdown-menu dropdown-menu-right">
            <li data-bind="foreach: jumpLinks()">
              <a href="#" data-bind="click: function(){$root.loadDir($data.path)}, text: $data.name"></a>
            </li>
          </ul>
        </div>
        <!-- /ko -->

      </div>
    </div>
  </div>
  <div class="row">
    <div class="col-sm-12">
      <div style="margin-bottom:1em;">
        <button type="button" class="btn btn-sm btn-default" data-bind="click: function(){$root.loadDir(upPath())}, css: {disabled: ( !upPath() || invalid() ) }">
          <i class="glyphicon glyphicon-folder-open"></i>
          <i class="glyphicon glyphicon-arrow-up"></i>
          <span data-bind="text: upPath() ? $root.dirName(upPath()) : '' "></span>
        </button>
        <div class="btn-group" data-bind="if: browseMode()=='browse'">
          <button type="button" class="btn btn-sm dropdown-toggle" data-bind="css: { disabled: !selectedPath() }" data-toggle="dropdown">
            {{ $t('Action')}}
            <span class="caret"></span>
          </button>
          <ul class="dropdown-menu" role="menu">
            <li>
              <a href="#storageconfirmdelete" data-toggle="modal">
                <i class="glyphicon glyphicon-remove"></i>
                {{ $t('Delete Selected Item') }}
              </a>
            </li>

            <li class="" data-bind=" if: selectedIsDownloadable()">
              <a href="#" data-bind="click: download">
                <i class="glyphicon glyphicon-download"></i>
                {{ $t('Download Contents') }}</a>
            </li>
          </ul>
        </div>

        <div class="btn-group" data-bind="if: allowUpload() ">
          <a href="#storageuploadkey" data-bind="click: actionUpload" class="btn btn-sm btn-primary">
            <i class="glyphicon glyphicon-plus"></i>
            {{ $t('Add or Upload a Key') }}
          </a>
        </div>
        <div class="btn-group" data-bind="if: allowUpload() && selectedPath() ">
          <a href="#storageuploadkey" data-bind="click: actionUploadModify" class="btn btn-sm btn-info ">
            <i class="glyphicon glyphicon-pencil"></i>
            {{ $t('Overwrite Key') }}
          </a>
        </div>
      </div>
      <div class="loading-area text-info " data-bind="visible: loading()" style="width: 100%; height: 200px; padding: 50px; background-color: #eee;">
        <i class="glyphicon glyphicon-time"></i>
        {{ $t('Loading') }}
      </div>
      <table class="table table-hover table-condensed" data-bind="if: !invalid() && !loading()">
        <tbody data-bind="if: !notFound()">
        <tr>
          <td colspan="2" class="text-strong">
            <span data-bind="if: filteredFiles().length < 1">
              {{ $t('No Keys') }}
            </span>
            <span data-bind="if: filteredFiles().length > 0">
              <span data-bind="text: filteredFiles().length"></span>
              {{ $t('keys') }}
            </span>
          </td>
        </tr>
        </tbody>
        <tbody data-bind="foreach: filteredFiles()">
        <tr data-bind="click: $root.selectFile, css: $root.selectedPath()==path() ? 'success' : '' " class="action">
          <td >
            <i class="glyphicon " data-bind="css: $root.selectedPath()==path() ? 'glyphicon-ok' : 'glyphicon-unchecked' "></i>

            <span data-bind="if: $data.isPrivateKey()" title="This path contains a private key that can be used for remote node execution.">
              <i class="glyphicon glyphicon-lock"></i>
            </span>
            <span data-bind="if: $data.isPublicKey()">
              <i class="glyphicon glyphicon-eye-open"></i>
            </span>
            <span data-bind="if: $data.isPassword()" title="This path contains a password that can be used for remote node execution.">
              <i class="glyphicon glyphicon-lock"></i>
            </span>

            <span data-bind="text: name"></span>
          </td>
          <td class="text-strong">
            <span class="pull-right">
              <span data-bind="if: $data.isPrivateKey()" title="${g.enc(code: 'storage.private.key.description')}">
                {{ $t('Private Key') }}
              </span>
              <span data-bind="if: $data.isPublicKey()">
                {{ $t('Public Key') }}
              </span>
              <span data-bind="if: $data.isPassword()" title="${g.enc(code: 'storage.password.description')}">
                {{ $t('Password') }}
              </span>
            </span>
          </td>
        </tr>
        </tbody>

        <tbody data-bind="if: notFound()">
        <tr>
          <td colspan="2">
            <span class="text-strong">{{ $t('Nothing found at this path') }}
              <span data-bind="if: allowUpload()">{{ $t('Select "Add or Upload a Key" if you would like to create a new key.') }}</span>
            </span>
          </td>
        </tr>
        </tbody>
        <tbody data-bind="foreach: directories()">
        <tr>
          <td class="action" data-bind="click: $root.loadDir" colspan="2">
            <i class="glyphicon glyphicon-arrow-down"></i>
            <i class="glyphicon glyphicon-folder-close"></i>
            <span data-bind="text: $root.dirName($data)"></span>
          </td>
        </tr>
        </tbody>
      </table>

    </div>
  </div>
  <div class="row" data-bind="if: selectedPath()">
    <div class="col-sm-12">
      <div class="well">
        <div>
          {{ $t('Storage path\:') }}
          <code class="text-success" data-bind="text: selectedPath()"></code>
          <a href="#" data-bind="attr: { href: selectedPathUrl() }">
            <i class="glyphicon glyphicon-link"></i>
          </a>
        </div>

        <div data-bind="if: selectedResource() && selectedResource().createdTime()">
          <div>
            {{ $t('created\:') }}
            <span class="timeabs text-strong" data-bind="text: selectedResource().createdTime(), attr: { title:  selectedResource().meta()['Rundeck-content-creation-time'] }"></span>

            <span data-bind="if: selectedResource().createdUsername()">
            {{ $t('by\:') }}

            <span class="text-strong" data-bind="text: selectedResource().createdUsername()"></span>
          </span>

          </div>
        </div>
        <div data-bind="if: selectedResource() && selectedResource().wasModified()">
          <div>
            {{ $t('Modified\:') }}
            <span class="timeago text-strong" data-bind="text: selectedResource().modifiedTimeAgo('ago'), attr: { title:  selectedResource().meta()['Rundeck-content-modify-time'] }"></span>

            <span data-bind="if: selectedResource().modifiedUsername()">
            {{ $t('by\:') }}
            <span class="text-strong" data-bind="text: selectedResource().modifiedUsername()"></span>
          </span>
          </div>
        </div>

        <div data-bind="if: selectedResource() && selectedResource().isPublicKey() && selectedIsDownloadable()">
          <button data-bind="click: function(){$root.actionLoadContents('publicKeyContents',$element);}, visible: !selectedResource().wasDownloaded()" class="btn btn-sm btn-default">
          {{ $t('View Public Key Contents') }}
          (<span data-bind="text: selectedResource().contentSize()"></span>
            {{ $t('Bytes') }}
          </button>

          <div class="pre-scrollable" data-bind="visible: selectedResource().downloadError()">
            <span data-bind="text:selectedResource().downloadError()" class="text-danger"></span>
          </div>
          <pre id="publicKeyContents"  class="pre-scrollable" data-bind="visible: selectedResource().wasDownloaded()"></pre>
        </div>
      </div>

    </div>
  </div>
</template>

<script lang="ts">
import { getRundeckContext, RundeckContext } from "../../../library";
import {RundeckBrowser} from "@rundeck/client";
import Vue from "vue";

const client: RundeckBrowser = getRundeckContext().rundeckClient
const rdBase = getRundeckContext().rdBase
const context = getRundeckContext()
interface StorageData {
  resourcePath: string;
  project: string;
  downloadEnabled: boolean;
}


export default Vue.extend({
  name: "KeyStorageBrowser.vue",
  data(){
    return{
      storageData: {} as StorageData,
      rootPath: "keys",
      baseUrl: "",
      basePath: "",
      jumpLinks: [],
      linksTitle: '',
      staticRoot: false,
      errorMsg: "",
      path: '',
      inputPath: '',
      selectedPath: '',
      selectedResource: '',
      selectedIsDownloadable: false,
      fileFilter: '',
      fieldTarget: '',
      resources: [],
      loading: false,
      invalid: false,
      browseMode: 'browse',
      allowUpload: false,
      allowSelection: true,
      allowNotFound: false,
      notFound: false,
      downloadenabled: true
    }
  },

  async mounted() {
    this.project = window._rundeck.projectName;
    this.storageData = window._rundeck.data.storageData as StorageData
  }

});
</script>

<style scoped>

</style>