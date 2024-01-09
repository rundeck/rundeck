import {RootStore} from './RootStore'
import {RundeckClient} from '@rundeck/client'
import axios from "axios";
import {ref} from "vue";

export class NewsStore {
    articles: Array<Article> = []

    constructor(readonly root: RootStore, readonly client: RundeckClient) {}

    loaded = ref<boolean>(false)

    load = async () => {
        if (this.loaded.value)
            return

        const resp = await axios
            .get("https://api.rundeck.com/news/v1/blog/list", {
            params: {
                groupid: 7039074342
            }
            })
        resp.data.objects.forEach( (o: any) => {
           this.articles.push(Article.FromApi(o))
        })
        this.loaded.value = true
    }

}


export class Article {
    url!: string
    description!: string
    imageUrl!: string
    title!: string
    date!: Date

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