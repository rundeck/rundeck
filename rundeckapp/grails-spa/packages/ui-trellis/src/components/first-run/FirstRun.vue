<template>
  <div class="card splash-screen">
    <div class="row">
      <div class="col-md-9 mb-6">
        
          <div class="splash-screen--title">
            <RundeckVersion :edition="edition" :number="version.number" :tag="version.tag"/>
          </div>
          <div class="splash-screen--linkitems">
            <a href="https://support.rundeck.com/" target="_blank" class="item"><i class="fas fa-first-aid"></i> Support</a>    
            <a :href="links.help " target="_blank" class="item"><i class="fas fa-book"></i> Docs</a>
          </div>
          <div class="splash-screen--byline">
            <p>
             To get started, create a new project.
            </p>
            <p>
              <a href="/resources/createProject" class="btn  btn-cta btn-lg ">
                  Create Project <b class="glyphicon glyphicon-plus"></b>
              </a>
            </p>
            <span class="text-small text-primary">
                            You can see this message again by clicking the
                            <a href="/menu/welcome">version number</a>
                            in the page footer.
                          </span>
          </div>

      </div>
      <div class="col-md-3 bg-white rounded p-6 shadow">
        <div class="stickers-section">
          <div class="l1">Get Your</div>
          <div class="l2">FREE</div>
          <div class="l3">Rundeck Stickers</div>
        </div>
        <div style="margin-top:12px;text-align:center;">
          <a href="https://www.rundeck.com/free-stuff" target="_blank" class="btn  btn-success btn-lg">Sign Up Now</a>
          <img src="https://www.rundeck.com/hubfs/Assets/website/rundeck-stickers.png" class="mt-6" style="max-width: 70%;">
        </div>
      </div>
    </div>
  </div>
</template>
<script lang="ts">
import Vue from 'vue'
import { Observer } from 'mobx-vue'
import {Component, Prop} from 'vue-property-decorator'

import { VersionInfo, ServerInfo } from '../../stores/System'

import {url} from '../../rundeckService'

import Copyright from '../version/Copyright.vue'
import RundeckVersion from '../version/RundeckVersionDisplay.vue'
import VersionDisplay from '../version/VersionIconNameDisplay.vue'
import ServerDisplay from '../version/ServerDisplay.vue'
import { getAppLinks } from '../../../lib'
import { AppLinks } from '../../../lib/interfaces/AppLinks'

@Observer
@Component({components: {
    ServerDisplay,
    VersionDisplay,
    RundeckVersion,
    Copyright
}})
export default class RundeckInfoWidget extends Vue {
    @Prop({default: 'Community'})
    edition!: String

    @Prop()
    version!: VersionInfo

    @Prop()
    server!: ServerInfo
    links!: AppLinks
    async created() {
      this.links = getAppLinks()
    }
    welcomeUrl() {
        return url('menu/welcome').toString()
    }
}
</script>

<style lang="scss" scoped>
.bg-white{
  background-color: #fff;
}
.rounded{
  border-radius: 1rem;
}
.p-6{
  padding: 1rem;
}
.shadow{
  box-shadow: 0px 0px 34px rgba(0, 0, 0, 0.2);
}
.mt-6{
  margin-top: 1rem;
}
.mb-6{
  margin-bottom: 1rem;
}
.splash-screen{
    padding:25px; 
    background-color: #F9F9F9;
    &--title{
        font-weight: 700;
        font-weight: 700;
        font-size: 2.2rem;
    }
    &--linkitems{
        display: flex;
        justify-content: flex-start;
        align-items: center;
        width: 100%;
        margin: 20px 0;
        .item{
            border: 1px solid #B4B4B4;
            box-shadow: 0px 0px 38px rgba(0, 0, 0, 0.11);
            border-radius: 4px;
            padding: 35px;
            margin-right: 14px;
            font-weight: 700;
            font-size: 1.7rem;
        }
    }
}
.stickers-section{
    padding:12px 24px;
    text-align: center;
    .l1{
        font-size: 20px;
        font-weight: 600;
    }
    .l2{
        font-size: 41px;
        font-weight: 800;
    }
    .l3{
        font-size: 32px;
    }
}

/*.rd-icon {
    display: inline-block;
    background-repeat: no-repeat;
    height: 25px;
    width: 30px;
    vertical-align: text-top;
    background-size: contain;
    background-image: url("data:image/svg+xml,%3Csvg width='59' height='60' viewBox='0 0 59 60' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath fill-rule='evenodd' clip-rule='evenodd' d='M47.8869 12.5094L40.4776 0.747314H0.767578L8.17692 12.5064L47.8869 12.5094ZM0.76992 59.5428H40.4799L47.8893 47.7838H8.17926L0.76992 59.5428ZM15.5869 24.2659H55.2969L59.0001 30.147L55.2969 36.028H15.5869L19.2901 30.147L15.5869 24.2659Z' fill='%23F73F39'/%3E%3C/svg%3E%0A");
}*/
</style>