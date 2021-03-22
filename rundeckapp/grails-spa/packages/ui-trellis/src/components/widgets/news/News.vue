<template>
    <div class="news-widget">
        <div class="news-article" v-for="article in news.articles.slice(0,4)" :key="article.name">
            <div style="margin-right: 10px; flex-basis: 50px; flex-shrink: 0;align-items: center;align-content: center; display: flex;">
                <img :src="article.imageUrl"/>
            </div>
            <div class="news-article__details">
                <p class="news-article__date">{{article.date.toUTCString('MMMM Do YYYY hh:mm')}}</p>
                <p class="news-article__description"><a :href="article.url" target="_blank">{{article.title}}</a></p>
            </div>
        </div>
        <div style="padding: 10px;">
            <span style="cursor: pointer;"><a class="text-info" @click="$emit('news:select-all')">View More Community News</a></span>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from 'vue'
import {Component, Inject} from 'vue-property-decorator'
import {Observer} from 'mobx-vue'

import { RecycleScroller } from 'vue-virtual-scroller'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'

import {RootStore} from '../../../stores/RootStore'
import PerfectScrollbar from 'perfect-scrollbar'
import { NewsStore } from '../../../stores/News'


@Observer
@Component({components: {
    RecycleScroller
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

</style>