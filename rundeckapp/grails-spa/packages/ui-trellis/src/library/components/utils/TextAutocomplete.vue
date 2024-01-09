<template>
  <Dropdown ref="dropdown"
            v-model="open"
            tag="section"
            :position-element="inputEl">
    <template v-slot:dropdown>
      <slot name="item" :items="items"  :select="selectItem" :highlight="highlight">
        <li v-for="(item,index) in items" >
          <a href="#" @click.prevent="selectItem(item)">
            <span v-html="highlight(item)"></span>
          </a>
        </li>
      </slot>
      <slot name="empty" v-if="!items || items.length === 0"/>
    </template>
  </Dropdown>
</template>

<script lang="ts">

import { Dropdown } from 'uiv'

import {defineComponent} from "vue"

export default defineComponent({
  name: "TextAutocomplete",
  extends: Dropdown,
  emits:['update:modelValue'],
  props: {
    modelValue: {
      required: true
    },
    data: Array,
    itemKey: String,
    limit: {
      type: Number,
      default: 10
    },
    target: {
      required: true
    },
    autocompleteKey: {
      type: String
    }
  },
  data () {
    return {
      inputEl: {} as any,
      items: [] as any,
      open: false,
      selectionStart: 0,
      currentSelection: 0,
      selectedValue: ""
    }
  },
  computed: {
    value: {
        get() {
            return this.modelValue
        },
        set(val: any) {
            this.$emit('update:modelValue', val)
        }
    }
  },
  methods: {
    selectItem (item: any) {
      let value = this.inputEl.value.toString()
      let preItem = value.substr(0, this.selectionStart)
      let postItem = value.substr(this.currentSelection+1, value.length)
      let finalValue =  preItem + item + postItem
      this.value = finalValue
      this.cleanItem ()
    },
    cleanItem () {
      this.open = false
      this.selectionStart = 0
      this.currentSelection = 0
      this.selectedValue = ""
    },
    highlight (item: any) {
      let _value = this.itemKey ? item[this.itemKey] : item
      let inputValue = "\\" + this.autocompleteKey + this.selectedValue.substr(1 , this.selectedValue.length)
      let regex = new RegExp(`${inputValue}`)
      return _value.replace(regex, '<b>$&</b>')
    },
    getItems (data: any) {
      this.items = []
      for (let i = 0, l = data.length; i < l; i++) {
        let item = data[i]
        let key = this.itemKey ? item[this.itemKey] : item
        key = key.toString()
        if(key.startsWith(this.selectedValue)){
          this.items.push(item)
        }

      }
    },
    initListeners () {
      if (this.inputEl) {
        this.inputEl.addEventListener('blur', this.inputBlured)
        this.inputEl.addEventListener('keydown', this.inputKeyPressed)
        this.inputEl.addEventListener('paste', this.inputPaste)
      }
    },
    inputPaste (event: any) {
      this.cleanItem()
      let clipboardData = event.clipboardData
      let pastedData = clipboardData.getData('Text');
      this.selectedValue = pastedData

      this.selectionStart = event.target.selectionStart
      this.currentSelection = this.selectionStart  + pastedData.length

      this.getItems(this.data)
      this.open = true
    },

    inputBlured () {
      if (!this.open) {
        this.cleanItem()
      }
    },
    inputKeyPressed (event: any) {
      event.stopPropagation()
      let key = event.key
      let keyCode = event.keyCode

      if(keyCode >= 8 && keyCode <=46){
        return
      }
      if(key == this.autocompleteKey){
        if(this.selectionStart != 0){
          this.cleanItem()
        }
        this.selectionStart = event.target.selectionStart
        this.open = true
      }
      this.currentSelection = event.target.selectionStart

      if(this.open){
        let originalInputEl = this.inputEl.value.toString()
        let newInputEl = originalInputEl.substr(0, this.currentSelection) + key + originalInputEl.substr(this.currentSelection+1, originalInputEl.length)

        this.selectedValue = newInputEl.substr(this.selectionStart,  this.currentSelection-this.selectionStart + 1)
        this.getItems(this.data)
      }
    },
    initInputElByTarget (target: any) {
      if (!target) {
        return
      }
      if (this.isString(target)) { // is selector
        this.inputEl = document.querySelector(target)
      } else if (this.isElement(target)) { // is element
        this.inputEl = target
      } else if (this.isElement(target.$el)) { // is component
        this.inputEl = target.$el
      }
    },
    isString (obj:any) {
      return typeof obj === 'string'
    },
    isElement (el:any) {
      return el && el.nodeType === Node.ELEMENT_NODE
    }
  },
  mounted () {
    this.initInputElByTarget(this.target)
    this.initListeners()
  },
});
</script>

<style scoped>

</style>