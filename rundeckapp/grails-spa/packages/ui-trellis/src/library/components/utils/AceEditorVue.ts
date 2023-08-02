import Vue from 'vue'

import * as ace from 'ace-builds'
import { VNode } from 'vue/types/umd'

/**
 * Ace Vue wrapper
 */
export default Vue.extend({
    render: function(h): VNode {
        const height = this.height ? this.px(this.height) : '100%'
        const width = this.width ? this.px(this.width) : '100%'

        return h('div', {
            attrs: {
                style: `height: ${height}; width: ${width};`,
                id: this.identifier,
            }
        })
    },
    props: {
        identifier: String,
        value: String,
        height: String,
        width: String,
        lang: String,
        softWrap: Boolean,
        theme: String,
        darkTheme: String,
        options: Object,
    },
    data () {
        return {
            editor: undefined as undefined | ace.Ace.Editor,
            contentBackup: "",
            observer: undefined as undefined | MutationObserver
        }
    },
    watch: {
        value: function(val): void {
            if (this.contentBackup !== val) {
                // @ts-ignore
                this.editor!.session.setValue(val,1)
                this.contentBackup = val
            }
        },
        theme: function (newTheme): void {
            this.editor!.setTheme(this.resolveTheme(newTheme))
        },
        lang: function (newLang): void {
            this.editor!.getSession().setMode(this.resolveLang(newLang))
        },
        softWrap: function (newSoftWrap): void {
            this.editor!.getSession().setUseWrapMode(newSoftWrap);
        },
        options: function(newOption): void {
            this.editor!.setOptions(newOption)
        },
        height: function(): void {
            this.$nextTick(function() {
                this.editor!.resize()
            })
        },
        width: function(): void {
            this.$nextTick(function() {
                this.editor!.resize()
            })
        }
    },
    mounted: function() {
        const lang = this.lang || 'text'
        const theme = this.getTheme()

        require('ace-builds/src-noconflict/ext-emmet')

        const editor = this.editor = ace.edit(this.$el)
        // @ts-ignore
        editor.$blockScrolling = Infinity

        this.$emit('init', editor)
        editor.getSession().setUseWorker(false)
        editor.getSession().setMode(this.resolveLang(lang))
        editor.setTheme(this.resolveTheme(theme))

        if (this.value)
            editor.setValue(this.value, 1)
        
        this.contentBackup = this.value
        
        editor.on('change', () => {
            const content = editor.getValue()
            this.$emit('input', content)
            this.contentBackup = content
        })

        if (this.options)
            editor.setOptions(this.options)
        this.observeDarkMode()
    },
    beforeDestroy: function() {
        this.editor!.destroy()
        this.editor!.container.remove()
        this.observer?.disconnect()
    },
    methods: {
        /**
         * Observe the dark mode and update the theme for the editor
         */
        observeDarkMode(){
            const query = matchMedia('(prefers-color-scheme: dark)');

            const changeHandler = () => {
                this.editor!.setTheme(this.resolveTheme(this.getTheme()));
            };

            // Support for Safari <14
            if (typeof query.addEventListener == "function")
                query.addEventListener('change', changeHandler);
            else
                query.addListener(changeHandler);

            this.observer = new MutationObserver(changeHandler)
            this.observer.observe(document.documentElement, {attributes: true})
        },

        /**
         * Get the theme to use based on whether it is dark mode or not
         */
        getTheme(){
            let theme = document.documentElement.dataset.colorTheme || 'light';

            if (theme == 'system')
                theme = matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';

            return theme == 'dark' ? (this.darkTheme||'tomorrow_night_eighties') : (this.theme||'chrome');
        },
        px(value: string): string {
            if (/^\d*$/.test(value))
                return `${value}px`

            return value
        },
        resolveTheme(theme: string): string {
            return `ace/theme/${theme}`
        },
        resolveLang(lang: string): string {
            return typeof lang === 'string' ? `ace/mode/${lang}` : lang
        }
    }
})