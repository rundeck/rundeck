import { observable} from 'mobx'
import { VueConstructor } from 'vue'

export class UIStore {
  @observable items: Array<UIItem> = []
  @observable watchers: Array<UIWatcher> = []
  @observable uiMessages: Array<UiMessage> = []

  notifyWatchers() {
    this.watchers.forEach((w) => {
      w.callback(this.itemsForLocation(w.section, w.location))
    })
  }

  addUiMessages(uiMessages: Array<UiMessage>){
    uiMessages.forEach(i => this.uiMessages.push(i))
  }

  getUiMessages():Array<UiMessage>{
    return this.uiMessages
  }

  itemsForLocation(section: string, location: string): Array<UIItem> {
    return this.items.filter((v) => v.section === section && (location ? v.location == location : true)).sort((a, b) => (a.order || 0) - (b.order || 0))
  }

  addItems(items: Array<UIItem>) {
    items.forEach(i => this.items.push(i))
    this.notifyWatchers()
  }

  addWatcher(watch: UIWatcher) {
    this.watchers.push(watch)
  }

  removeWatcher(watch: UIWatcher) {
    let ndx=this.watchers.indexOf(watch)
    if(ndx>=0){
      this.watchers.splice(ndx, 1)
    }
  }
}

export interface UIWatcher {
  section: string
  location: string
  callback: (items: Array<UIItem>) => void
}

export interface UIItem {
  id?: string
  order?: number
  section: string
  location: string
  visible: boolean
  widget?: VueConstructor
  html?: string
  text?: string
}

export interface UiMessage {
  [key: string]: string
}