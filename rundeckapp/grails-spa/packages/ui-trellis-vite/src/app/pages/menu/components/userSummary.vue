<template>
  <div>
    <!-- -->
    <modal v-model="openModal" ref="modal" append-to-body>
        <span slot="title">Search</span>
        <div class="row">
          <div class="col-xs-12">
            <div class="form-group" style="padding-top:10px">
              <span class="prompt">{{ $t("message.pageUsersSummary")}}</span>
            </div>
          </div>
        </div>
        <div class="base-filters">
          <form @submit.prevent="loadUsersList(0)">
            <div class="row">
              <div
                class="col-xs-12"
                :class="{'col-sm-4': sessionIdEnabled, 'col-sm-6': !sessionIdEnabled}"
              >
                <div class="form-group">
                  <input
                    type="text"
                    class="form-control"
                    :placeholder="$t( 'message.pageFilterLogin')"
                    v-model="loginFilter"
                  />
                </div>
              </div>
              <div class="col-xs-12 col-sm-4" v-if="sessionIdEnabled">
                <div class="form-group">
                  <input
                    type="text"
                    class="form-control"
                    :placeholder="$t( 'message.pageFilterSessionID')"
                    v-model="sessionIdFilter"
                  />
                </div>
              </div>
              <div
                class="col-xs-12"
                :class="{'col-sm-4': sessionIdEnabled, 'col-sm-6': !sessionIdEnabled}"
              >
                <div class="form-group">
                  <input
                    type="text"
                    class="form-control"
                    :placeholder="$t( 'message.pageFilterHostName')"
                    v-model="hostNameFilter"
                  />
                </div>
              </div>
            </div>
            <button style="display:none;" type="submit">Submit</button>
          </form>
        </div>
        <div slot="footer">
          <btn @click="openModal=false">Cancel</btn>
          <btn type="cta" @click="loadUsersList(0)">{{ $t("message.pageFilterBtnSearch")}}</btn>
        </div>
    </modal>

    <p class="help-block">{{ $t('message.userSummary.desc')}}</p>
    <!-- -->
    <section id="userProfilePage" class="section-space">
              <div class="row">
                <div :class="{'col-sm-8':showLoginStatus, 'col-sm-10': !showLoginStatus}">
                  <section class="section-space-bottom">
                    <span>
                      {{ $t("message.pageUsersTotalFounds")}}
                      <span
                        v-if="!loading && this.pagination.total>=0"
                        class="text-info"
                      >{{this.pagination.total}}</span>
                      <b class="fas fa-circle-notch fa-spin text-info" v-if="loading"></b>
                    </span>
                    <span style="margin-left: 10px;">
                      <btn
                              class="btn btn-secondary btn-sm "
                              style="margin-bottom:1em;"
                              @click="openModal=true"
                      >Search...</btn>
                      <btn
                              v-if="search"
                              style="margin-right:1em; margin-bottom:1em;"
                              class="btn btn-secondary btn-sm "
                              @click="clearSearchParams()"
                      >Clear</btn>
                    </span>
                  </section>
                </div>
                <div class="col-sm-2" v-if="showLoginStatus">
                  <span class="checkbox">
                    <input
                      type="checkbox"
                      name="loggedOnly"
                      id="loggedOnly"
                      v-model="loggedOnly"
                      @change="loadUsersList(0)"
                    />
                    <label for="loggedOnly">{{ $t("message.pageUserLoggedOnly")}}</label>
                  </span>
                </div>
                <div class="col-sm-2">
                  <span class="checkbox">
                    <input
                      type="checkbox"
                      name="includeExec"
                      id="includeExec"
                      v-model="includeExec"
                      @change="loadUsersList(0)"
                    />
                    <label for="includeExec">{{ $t("message.paramIncludeExecTitle")}}</label>
                  </span>
                </div>
              </div>

              <div class="row">
                <div class="col-sm-12">
                  <table class="table table-condensed table-striped">
                    <tbody>
                      <tr>
                        <th class="table-header">{{ $t("message.pageUsersLoginLabel")}}</th>
                        <th class="table-header">{{ $t("message.domainUserEmailLabel")}}</th>
                        <th class="table-header">{{ $t("message.domainUserFirstNameLabel")}}</th>
                        <th class="table-header">{{ $t("message.domainUserLastNameLabel")}}</th>
                        <th class="table-header">{{ $t("message.pageUsersCreatedLabel")}}</th>
                        <th class="table-header">{{ $t("message.pageUsersUpdatedLabel")}}</th>
                        <th
                          v-if="includeExec"
                          class="table-header"
                        >{{ $t("message.pageUsersLastjobLabel")}}</th>
                        <th class="table-header">
                          {{ $t("message.pageUsersTokensLabel")}}
                          <span
                            class="has_tooltip text-strong"
                            data-placement="bottom"
                            :data-original-title="$t('message.pageUsersTokensHelp')"
                          >
                            <i class="glyphicon glyphicon-question-sign"></i>
                          </span>
                        </th>
                        <th
                          class="table-header"
                          v-if="sessionIdEnabled"
                        >{{ $t("message.pageUsersSessionIDLabel")}}</th>
                        <th class="table-header">{{ $t("message.pageUsersHostNameLabel")}}</th>
                        <th class="table-header">{{ $t("message.pageUsersLastLoginInTimeLabel")}}</th>
                        <th
                          class="table-header"
                          v-if="showLoginStatus"
                        >{{ $t("message.pageUsersLoggedStatus")}}</th>
                      </tr>

                      <tr v-for="(user, index) in users" :key="index">
                        <td>
                          <login-status
                            :status="user.loggedStatus"
                            :label="false"
                            :showLoginStatus="showLoginStatus"
                          />
                          {{user.login}}
                        </td>
                        <td v-if="user.email">{{user.email}}</td>
                        <td v-else>
                          <span
                            class="text-muted small text-uppercase"
                          >{{ $t("message.pageUserNotSet")}}</span>
                        </td>
                        <td v-if="user.firstName">{{user.firstName}}</td>
                        <td v-else>
                          <span
                            class="text-muted small text-uppercase"
                          >{{ $t("message.pageUserNotSet")}}</span>
                        </td>
                        <td v-if="user.lastName">{{user.lastName}}</td>
                        <td v-else>
                          <span
                            class="text-muted small text-uppercase"
                          >{{ $t("message.pageUserNotSet")}}</span>
                        </td>
                        <td v-if="user.created">{{user.created | moment("MM/DD/YYYY hh:mm a")}}</td>
                        <td v-else>
                          <span
                            class="text-muted small text-uppercase"
                          >{{ $t("message.pageUserNotSet")}}</span>
                        </td>
                        <td
                          v-if="user.updated"
                          :class="user.updated===user.created?'text-muted ':''"
                        >{{user.updated | moment("MM/DD/YYYY hh:mm a")}}</td>
                        <td v-else>
                          <span
                            class="text-muted small text-uppercase"
                          >{{ $t("message.pageUserNotSet")}}</span>
                        </td>
                        <td
                          v-if="includeExec && user.lastJob"
                        >{{user.lastJob | moment("MM/DD/YYYY hh:mm a")}}</td>
                        <td v-else-if="includeExec">
                          <span
                            class="text-muted small text-uppercase"
                          >{{ $t("message.pageUserNone")}}</span>
                        </td>
                        <td v-if="user.tokens > 0">{{user.tokens}}</td>
                        <td v-else>
                          <span
                            class="text-muted small text-uppercase"
                          >{{ $t("message.pageUserNone")}}</span>
                        </td>
                        <td v-if="user.lastSessionId && sessionIdEnabled">{{user.lastSessionId}}</td>
                        <td v-else-if="sessionIdEnabled">
                          <span
                            class="text-muted small text-uppercase"
                          >{{ $t("message.pageUserNotSet")}}</span>
                        </td>
                        <td v-if="user.lastHostName">{{user.lastHostName}}</td>
                        <td v-else>
                          <span
                            class="text-muted small text-uppercase"
                          >{{ $t("message.pageUserNotSet")}}</span>
                        </td>
                        <td v-if="user.loggedInTime">
                          {{user.loggedInTime | moment("MM/DD/YYYY hh:mm a")}}
                          {{user.loggedInTime | moment('from','now')}}
                        </td>
                        <td v-else>
                          <span
                            class="text-muted small text-uppercase"
                          >{{ $t("message.pageUserNotSet")}}</span>
                        </td>
                        <td v-if="showLoginStatus">
                          <login-status
                            :status="user.loggedStatus"
                            :label="true"
                            :showLoginStatus="showLoginStatus"
                          />
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </section>

    <offset-pagination
      :pagination="pagination"
      @change="changePageOffset($event)"
      :disabled="loading"
      :showPrefix="false"
    ></offset-pagination>
  </div>
</template>

<script>
import axios from "axios";
import OffsetPagination from "@/library/components/utils/OffsetPagination.vue";
import LoginStatus from "./LoginStatus.vue";

export default {
  name: "UserSummary",
  components: {
    OffsetPagination,
    LoginStatus
  },
  props: ["menu", "userSummary"],
  data() {
    return {
      openModal: false,
      search: false,
      showLoginStatus: false,
      sessionIdEnabled: false,
      users: [],
      loggedOnly: false,
      includeExec: false,
      sessionIdFilter: "",
      hostNameFilter: "",
      loginFilter: "",
      loading: false,
      rdBase: null,
      pagination: {
        offset: 0,
        max: 100,
        total: -1
      }
    };
  },
  methods: {
    changePageOffset(offset) {
      if (this.loading) {
        return;
      }
      this.loadUsersList(offset);
    },
    setSummaryPageConfig() {
      this.loading = true;
      this.loggedOnly = false;
      axios({
        method: "get",
        headers: { "x-rundeck-ajax": true },
        url: `${this.rdBase}/user/getSummaryPageConfig`,
        withCredentials: true
      }).then(response => {
        this.loggedOnly = response.data.loggedOnly;
        this.showLoginStatus = response.data.showLoginStatus;
        this.loadUsersList(0);
      });
    },

    clearSearchParams() {
      this.loggedOnly = false;
      this.includeExec = false;
      this.sessionIdFilter = "";
      this.hostNameFilter = "";
      this.loginFilter = "";
      this.loadUsersList(0);
      this.search = false;
    },

    async loadUsersList(offset) {
      this.loading = true;
      this.pagination.offset = offset;
      if (this.openModal) {
        this.openModal = false;
        if (this.hostNameFilter || this.loginFilter || this.sessionIdFilter) {
          this.search = true;
        } else {
          this.search = false;
        }
      }
      axios({
        method: "get",
        headers: { "x-rundeck-ajax": true },
        url: `${this.rdBase}/user/loadUsersList`,
        params: {
          loggedOnly: `${this.loggedOnly}`,
          includeExec: `${this.includeExec}`,
          offset: this.pagination.offset,
          sessionFilter: this.sessionIdFilter,
          hostNameFilter: this.hostNameFilter,
          loginFilter: this.loginFilter
        },
        withCredentials: true
      }).then(response => {
        this.pagination.max = response.data.maxRows;
        this.pagination.total = response.data.totalRecords;
        this.users = response.data.users;
        this.sessionIdEnabled = response.data.sessionIdEnabled;
        this.showLoginStatus = response.data.showLoginStatus;
        this.loading = false;
      });
    }
  },
  beforeMount() {
    if (window._rundeck && window._rundeck.rdBase) {
      this.rdBase = window._rundeck.rdBase;
      this.setSummaryPageConfig();
      window._rundeck.eventBus.$on('refresh-user-summary',() => {
        this.loadUsersList(0)
      })
    }
  },
  beforeDestroy() {
    window._rundeck.eventBus.$off('refresh-user-summary')
  }
};
</script>

<style scoped lang="scss">
ul.users {
  display: inline;
  margin: 0;
  padding: 0;
}

ul.users li {
  display: inline;
  list-style: none;
  margin: 0;
  padding: 0;
}

ul.users li:after {
  content: ", ";
  color: #aaa;
}

ul.users li:last-child:after {
  content: "";
}
#userProfilePage {
  .checkbox {
    margin-top: 0;
  }
}
</style>

