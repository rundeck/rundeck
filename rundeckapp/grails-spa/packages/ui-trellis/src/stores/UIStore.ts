import { observable} from 'mobx'
import { VueConstructor } from 'vue'

export class UIStore {
  @observable items: Array<UIItem> = []

  itemsForLocation(section: string, location: string): Array<UIItem> {
    return this.items.filter((v) => v.section === section && (location ? v.location == location : true)).sort((a, b) => a.order - b.order)
  }

  addItems(items: Array<UIItem>) {
    items.forEach(i => this.items.push(i))
  }
}

export interface UIItem {
  id: string
  order: number
  section: string
  location: string
  visible: boolean
  widget?: VueConstructor
  html?:string
  text?:string
}