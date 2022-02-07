<template>
    <div class="news-widget">
        <Skeleton :loading="!news.loaded" type="community-news">
            <div class="news-article" v-for="article in news.articles.slice(0,4)" :key="article.name">
                <div style="margin-right: 10px; flex-basis: 50px; flex-shrink: 0;align-items: center;align-content: center; display: flex;">
                    <img :src="article.imageUrl"/>
                </div>
                <div class="news-article__details">
                    <p class="news-article__date">{{article.date.toUTCString('MMMM Do YYYY hh:mm')}}</p>
                    <p class="news-article__description"><a :href="article.url" target="_blank">{{article.title}}</a></p>
                </div>
            </div>
        </Skeleton>
        <div style="padding: 10px;">
            <span style="cursor: pointer;"><a class="text-info" @click="$emit('news:select-all')">View More Community News</a></span>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from 'vue'
import {Component, Inject} from 'vue-property-decorator'
import {Observer} from 'mobx-vue'
import PerfectScrollbar from 'perfect-scrollbar'

import { RecycleScroller } from 'vue-virtual-scroller'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'

import {RootStore} from '../../../stores/RootStore'
import { NewsStore } from '../../../stores/News'

import Skeleton from '../../skeleton/Skeleton.vue'


@Observer
@Component({components: {
    RecycleScroller,
    Skeleton
}})
export default class CommunityNews extends Vue {
    @Inject()
    private readonly rootStore!: RootStore

    news!: NewsStore

    created() {
        this.news = this.rootStore.news
    }

    mounted() {
        this.news.load()
    }
}
</script>

<style scoped lang="scss">
.news-widget {
    > div {
        margin-top: 10px;
    }

    > *:not(:first-child) {
        border-top: solid 1px whitesmoke;
    }
}
.news-article {
    display: flex;
    width: 400px;
    overflow: hidden;
    padding: 10px;

    img {
        height: auto;
        // float: right;
        max-width: 100%;
    }

    span {
        font-weight: 700;
    }
}

.news-article__details {
    flex-grow: 1;
    > p {
        margin: 0;
    }
}


.news-article__date {
    font-size: 12px;
}

.news-article__description {
    font-weight: 700;
}

.skeleton {
    height: 300px;
    width: 400px;
    margin: 10px;
}

.skeleton--community-news {
    background-image:
            linear-gradient( 100deg, rgba(255, 255, 255, 0), rgba(255, 255, 255, 0.5) 50%, rgba(255, 255, 255, 0) 80% ),
            linear-gradient( lightgray 26px, transparent 0 ),
            linear-gradient( lightgray 1em, transparent 0 ),
            linear-gradient( lightgray 1em, transparent 0 ),
            linear-gradient( lightgray 1em, transparent 0 );

        background-repeat: repeat-y;

        background-size:
            50px 75px, /* highlight */
            50px 75px, /* circle */
            150px 75px,
            350px 75px,
            300px 75px;

        background-position:
            -50px 0, /* highlight */
            0 1em, /* circle */
            60px 0,
            60px 1.5em,
            60px 3em,
            60px 4.5em;

        animation: communityNewsSkel 1s infinite;

    @keyframes communityNewsSkel {
        to {
            background-position:
                120% 0, /* move highlight to right */
                0 1em,
                60px 0,
                60px 1.5em,
                60px 3em,
                60px 4.5em;
        }
    }
}

</style>