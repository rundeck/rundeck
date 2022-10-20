import {RootStore} from './RootStore'
import {RundeckClient} from '@rundeck/client'
import { action, computed, flow, observable } from 'mobx'
import Axios from 'axios'

export class NewsStore {
    @observable articles: Array<Article> = []

    constructor(readonly root: RootStore, readonly client: RundeckClient) {}

    @observable loaded = false

    load = flow(function* (this: NewsStore) {
        if (this.loaded)
            return

        const resp = yield Axios
            .get("https://api.rundeck.com/news/v1/blog/list", {
            params: {
                groupid: 7039074342
            }
            })
        resp.data.objects.forEach( (o: any) => {
           this.articles.push(Article.FromApi(o))
        })
        this.loaded = true
    })

}


export class Article {
    @observable url!: string
    @observable description!: string
    @observable imageUrl!: string
    @observable title!: string
    @observable date!: Date

    static FromApi(post: any): Article {
        const article = new Article
        return article.fromApi(post)
    }

    fromApi(post: any) {
        this.title = post.title
        this.url = post.absolute_url
        this.description = post.meta_description
        this.imageUrl = post.featured_image
        this.date = new Date(post.publish_date)
        return this
    }
}