<template>
  <div>
    <div style="margin-top:3em;">
      <ais-index :app-id="algoliaAppId" :api-key="algoliaApiKey" :index-name="algoliaIndex">
        <ais-search-box>
          <!-- SEARCH BOX -->
          <div class="columns styled-inputs">
            <div class="column is-four-fifths">
              <div class="control has-icons-right">
                <ais-input
                  placeholder="Search by plugin name..."
                  :classNames="{
                  'ais-input': 'input is-large'
                  }"
                />
                <span class="icon is-medium is-right" aria-hidden="true">
                  <i class="fas fa-search"></i>
                </span>
              </div>
            </div>
            <div class="column is-one-fifth has-text-right">
              <label for>Per Page</label>
              <!-- <div class="select-wrapper">
                <ais-results-per-page-selector
                  :options="[12, 24, 48]"
                  :classNames="{'ais-results-per-page-selector': 'form-control'}"
                />
              </div>-->
            </div>
          </div>
          <div class="columns">
            <div class="column">
              <section class="collapse-section">
                <button
                  class="button is-dark is-medium is-rounded is-padded collapse-trigger"
                  slot="trigger"
                  slot-scope="{open}"
                >
                  <span v-show="open">Close</span>
                  <span v-show="!open">Filter Results</span>
                </button>
                <div>
                  <div class="support-tab-row">
                    <ais-refinement-list
                      attribute-name="support"
                      :sort-by="['name:asc']"
                      :classNames="{
                          'ais-refinement-list': 'support-refinement-tabs-wrapper',
                          'ais-refinement-list__count': 'is-hidden',
                          'ais-refinement-list__item': 'support-refinement-tab-item'
                        }"
                    ></ais-refinement-list>
                  </div>

                  <div class="tag-cloud">
                    <span class="side-title is-size-6">Tags</span>
                    <ais-refinement-list
                      attribute-name="tags"
                      :sort-by="['count:desc', 'name:asc']"
                      :classNames="{
                            'ais-refinement-list': 'field is-grouped is-grouped-multiline',
                            'ais-refinement-list__count': '',
                            'ais-refinement-list__item': 'control'
                          }"
                    >
                      <template slot-scope="{ value, active, count }">
                        <div class="tags has-addons">
                          <a
                            class="tag"
                            v-bind:class="{'is-red':active, 'is-dark': !active}"
                          >{{value | capitalSplit}}</a>
                          <a class="tag">{{count}}</a>
                        </div>
                      </template>
                    </ais-refinement-list>
                  </div>
                  <div
                    class="type-cloud"
                    style="margin-top:2em;border-top: 1px solid #dbd;padding-top:1em;"
                  >
                    <span class="side-title is-size-6">Types</span>

                    <ais-refinement-list
                      attribute-name="providesServices"
                      :sort-by="['count:desc', 'name:asc']"
                      :classNames="{
                            'ais-refinement-list': 'field is-grouped is-grouped-multiline',
                            'ais-refinement-list__count': '',
                            'ais-refinement-list__item': 'control'
                          }"
                    >
                      <template slot-scope="{ value, active, count }">
                        <div class="tags has-addons">
                          <a
                            class="tag"
                            v-bind:class="{'is-red':active, 'is-dark': !active}"
                          >{{value | capitalSplit}}</a>
                          <a class="tag">{{count}}</a>
                        </div>
                      </template>
                    </ais-refinement-list>
                  </div>
                </div>
              </section>
            </div>
          </div>
          <div class="columns">
            <div class="column">
              <ais-stats></ais-stats>
            </div>
          </div>
          <div class="columns">
            <div class="column">
              <!-- SEARCH RESULTS -->
              <ais-results class="search-results columns is-multiline">
                <template slot-scope="{ result }">
                  <div class="column is-one-third">
                    <div class="card">
                      <header class="card-header">
                        <p class="card-header-title">{{result.name}}</p>
                        <a href="#" class="card-header-icon" aria-label="more options">
                          <span class="tag">{{result.rundeckCompatibility}}</span>
                        </a>
                      </header>
                      <div class="card-content">
                        <div style="margin-bottom:.7em;">{{result.description}}</div>
                        <div style="margin-bottom: .7em;">
                          <a :href="result.binaryLink" class="has-text-danger">
                            View Plugin Info
                            <span>
                              <i class="fas fa-external-link-alt fa-sm"></i>
                            </span>
                          </a>
                        </div>
                        <div style="margin-bottom: .7em;">
                          <a @click="install(result.id)" class="has-text-danger">INSTALL</a>
                        </div>
                        <!-- <div>{{result.support}}</div> -->
                        <div>
                          <ul class="tags">
                            <li
                              v-for="(tag, index) in result.tags"
                              :key="index"
                              class="tag has-background-grey-lighter"
                            >{{tag | capitalSplit}}</li>
                          </ul>
                        </div>
                      </div>
                      <footer class="card-footer">
                        <span class="card-footer-item">{{result.support}}</span>
                      </footer>
                    </div>
                  </div>
                </template>
              </ais-results>
              <ais-no-results/>
              <nav class="pagination is-centered" role="navigation" aria-label="pagination">
                <ais-pagination
                  :classNames="{
                  'ais-pagination': 'pagination-list',
                  'ais-pagination__item': '',
                  'ais-pagination__item--first': 'is-dark',
                  'ais-pagination__item--previous': '',
                  'ais-pagination__item--next': '',
                  'ais-pagination__item--last': '',
                  'ais-pagination__link': 'pagination-link',
                  'ais-pagination__item--active': 'is-current',
                  'ais-pagination__item--disabled': 'is-disabled',
                }"
                >
                  <span slot="first">
                    <i class="fas fa-step-backward"></i>
                  </span>
                  <span slot="previous">
                    <i class="fas fa-chevron-left"></i>
                  </span>
                  <span slot="next">
                    <i class="fas fa-chevron-right"></i>
                  </span>
                  <span slot="last">
                    <i class="fas fa-step-forward"></i>
                  </span>
                </ais-pagination>
              </nav>

              <!-- // SEARCH RESULTS -->
            </div>
          </div>
        </ais-search-box>
        <div style="text-align:center;margin: 1em 0;">
          <ais-powered-by/>
        </div>
      </ais-index>
    </div>
  </div>
</template>

<script>
import axios from "axios";
import config from "./config";

console.log("config", config);

export default {
  name: "RepositoryBrowser",
  components: {},
  data() {
    return {
      open: false,
      algoliaAppId: config.algolia.AppId,
      algoliaApiKey: config.algolia.ApiKey,
      algoliaIndex: config.algolia.Index,
      repoName: config.repoName,
      searchStore: null,
      errors: null
    };
  },
  methods: {
    install(pluginId) {
      this.errors = null;
      let rdBase = window._rundeck.rdBase;
      console.log("install");
      axios({
        method: "post",
        headers: { "x-rundeck-ajax": true },
        url: `${rdBase}repository/${this.repoName}/install/${pluginId}`,
        withCredentials: true
      })
        .then(response => {
          console.log("response", response);
          let repo = this.repositories.find(r => r.repositoryName === repoName);
          let plugin = repo.results.find(r => r.id === pluginId);
          plugin.installed = true;
        })
        .catch(error => {
          this.errors = error;
          console.log("ERROR", this.errors);
        });
    }
  },
  filters: {
    capitalize: function(value) {
      if (!value) return "";
      value = value.toString();
      return value.charAt(0).toUpperCase() + value.slice(1);
    },
    capitalSplit: function(value) {
      if (!value) return "";
      if (value.length <= 2) return value.toString().toUpperCase();
      return value.split(/(?=[A-Z])/).join(" ");
    }
  }
};
</script>
<style scoped src='bulma/bulma.sass'>
/* global styles */
</style>
<style lang="scss">
.tag-cloud,
.type-cloud {
  display: -webkit-box;
  display: -ms-flexbox;
  display: flex;
  -webkit-box-align: center;
  -ms-flex-align: center;
  align-items: center;
  .side-title {
    display: inline-block;
    color: #f7403a;
    font-size: 1em;
    font-weight: 600;
    text-transform: uppercase;
    position: relative;
    -ms-flex-negative: 0;
    flex-shrink: 0;
    -webkit-transform: scaleX(-1) scaleY(-1);
    -ms-transform: scaleX(-1) scaleY(-1);
    transform: scaleX(-1) scaleY(-1);
    -webkit-writing-mode: tb-rl;
    -ms-writing-mode: tb-rl;
    writing-mode: tb-rl;
    .ais-refinement-list__checkbox {
      display: none;
    }
    .field.is-grouped-multiline {
      display: -webkit-box;
      display: -ms-flexbox;
      display: flex;
      -webkit-box-align: stretch;
      -ms-flex-align: stretch;
      align-items: stretch;
      list-style-type: none;
      padding: 0;
      margin: 0;
      -webkit-box-flex: 1;
      -ms-flex-positive: 1;
      flex-grow: 1;
      border-left: 3px solid #ddd;
      padding-left: 1em;
      margin-left: 0.6em;
    }
    a.tag {
      padding-top: 2px;
      -webkit-transition: 0.25s all ease-in-out;
      -o-transition: 0.25s all ease-in-out;
      transition: 0.25s all ease-in-out;
      &:hover {
        text-decoration: none !important;
      }

      cursor: pointer;
      &.is-red {
        background-color: #f7403a;

        color: #fff;
      }
    }
  }
  .collapse-section {
    .collapse-trigger {
      display: block !important;
      margin: 0 auto;
      text-align: center;
      // color: blue;
    }
    .collapse-content {
      margin: 2em 0;
    }
  }
  .support-tab-row {
    border-bottom: 1px solid #dbdbdb;
    border-top: 1px solid #dbdbdb;
    margin-bottom: 2em;
    text-align: center;
  }
  .support-refinement-tabs-wrapper {
    .support-refinement-tab-item {
      display: inline-block;
      border-bottom: 1px solid #dbdbdb;
      margin-bottom: -1px;
      padding: 0.5em;
      font-weight: bold;
      &.ais-refinement-list__item--active {
        border-bottom-color: #4a4a4a;
        color: #363636;
      }
      .ais-refinement-list__checkbox {
        display: none;
      }
    }
  }

  .support-refinement-wrapper {
    width: 60%;
    margin: 0 auto 0.9375em;
    // -ms-flex-wrap: wrap;
    // flex-wrap: wrap;

    display: flex;
    flex-flow: row wrap;
    justify-content: space-evenly;
  }
  .support-refinement-list-item {
    // display: inline;
    flex: none;
    padding: 0;
    margin: 0 1.5625em 0 0;
    font-weight: bold;
    &.ais-refinement-list__item--active {
      .ais-refinement-list__label {
        .ais-refinement-list__value {
          color: #333;
          border-bottom: 3px solid #333;
        }
      }
    }
    .ais-refinement-list__checkbox {
      display: none;
    }
    .ais-refinement-list__label {
      position: relative;
      display: inline-block;
      padding: 0.44444em 0 0;
      border: 0;
      background: 0;
      .ais-refinement-list__value {
        color: #6f9ad3;
        font-size: 1.125em;
        font-weight: 500;
        border-bottom: 3px solid transparent;
        padding-bottom: 0.44444em;
        -webkit-transition: 0.25s all ease-in-out;
        -o-transition: 0.25s all ease-in-out;
        transition: 0.25s all ease-in-out;
      }
    }
  }
  //
  // Results
  //
  .search-results {
    .card {
      // margin: 0 10px 10px 0;
      padding: 10px 15px 3px;
      background-color: #fff;
      border: 3px solid #ddd;
      // flex: 0 0 275px;
      // min-height: 100px;
      // max-width: calc(33% - 10px);
      font-size: 1.125em;
    }
    .column-card {
      position: relative;
      height: 100%;
    }
    .card-header {
      box-shadow: none;
      border-bottom: 3px;
      border-bottom-color: black;
      border-bottom-style: solid;
    }
    .card-header-title {
      font-weight: bold;
      color: #333;
      padding-left: 0;
    }
    .card-header-icon {
      padding-right: 0;
    }
    .card-content {
      padding: 0.5em 0;
    }
    .card-footer {
      border-top: 3px solid #000;
    }
    .item-hdr {
      font-weight: bold;
      color: #333;
      padding: 0 0 5px 0;
      border-bottom: 3px;
      border-bottom-color: black;
      border-bottom-style: solid;
      margin-bottom: 5px;
    }
    .desc {
      padding: 5px 0;
    }
    .link {
      color: #6f9ad3;
      text-decoration: underline;
    }
    .footer {
      // position: absolute;
      // bottom: 0;
      padding: 0;
      border-top: 3px;
      border-top-color: black;
      border-top-style: solid;
    }
    .tag {
      cursor: pointer;
      &.is-red {
        background-color: #f7403a;
        color: #fff;
      }
    }
  }
  .pagination {
    .ais-pagination__item.is-current > a.pagination-link {
      background-color: #f7403a;
      border-color: #f7403a;
      color: white;
    }
    .pagination-link {
      padding-bottom: calc(0.375em - 3px);
      border-color: #dddddd;
      border-width: 3px;
      font-weight: bold;
      border-radius: 0;
      margin-left: 0.6em;
      margin-right: 0.6em;
    }
    .ais-pagination__item--previous {
      .pagination-link {
        padding-left: 0.4em;
      }
    }
    .ais-pagination__item--next {
      .pagination-link {
        padding-left: 0.6em;
      }
    }
  }
}
</style>
