<template>
    <div ref="root" :id="identifier" :style="styleCss"></div>
</template>
<script lang="ts">
import {defineComponent} from 'vue'

import * as ace from 'ace-builds'

/**
 * Ace Vue wrapper
 */
export default defineComponent({
    props: {
        identifier: String,
        modelValue: {
          type: String,
          required: true,
        },
        height: String,
        width: String,
        lang: String,
        softWrap: Boolean,
        theme: String,
        darkTheme: String,
        options: Object,
    },
    emits: ['init', 'update:modelValue'],
    data () {
        return {
            editor: undefined as undefined | ace.Ace.Editor,
            contentBackup: "",
            observer: undefined as undefined | MutationObserver
        }
    },
    watch: {
        modelValue: function(val): void {
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

        if (this.modelValue)
            editor.setValue(this.modelValue, 1)
        
        this.contentBackup = this.modelValue
        
        editor.on('change', () => {
            const content = editor.getValue()
            this.$emit('update:modelValue', content)
            this.contentBackup = content
        })

        if (this.options)
            editor.setOptions(this.options)
        this.observeDarkMode()
    },
    beforeUnmount: function() {
        this.editor!.destroy()
        this.editor!.container.remove()
        this.observer?.disconnect()
    },
    computed: {
        styleCss() {
            let style = {height:"100%",width:"100%"}
            if(this.height) style.height = this.px(this.height)
            if(this.width) style.width = this.px(this.width)
            return style
        }
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
</script>