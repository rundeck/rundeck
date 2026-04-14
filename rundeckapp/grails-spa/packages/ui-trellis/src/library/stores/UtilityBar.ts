import { NavItem } from "./NavBar";
import { RootStore } from "./RootStore";
import { RundeckClient } from "@rundeck/client";
import { reactive } from "vue";

export class UtilityBar {
  items: Array<UtilityItem> = reactive([]);

  overflow: Array<UtilityItem> = [];

  constructor(
    readonly root: RootStore,
    readonly client: RundeckClient,
  ) {
    if (window._rundeck.navbar) {
      window._rundeck.navbar.items.forEach((i: NavItem) => {
        this.items.push({
          ...i,
          visible: true,
          container: i.container || "root",
        });
      });
    }
  }

  getItem(id: string) {
    return this.items.find((i) => i.id == id) as UtilityBarItem;
  }

  addOrReplaceItem(item: UtilityBarItem) {
    const existing = this.items.find((i) => i.id == item.id);
    if (existing) {
      const index = this.items.indexOf(existing);
      this.items.splice(index, 1, item);
    } else {
      this.items.push(item);
    }
  }

  addItems(items: Array<UtilityBarItem>) {
    items.forEach((i) => this.items.push(i));
  }

  containerGroupItems(container: string, group: string) {
    const items = this.items.filter(
      (i) => i.group == group && i.container == container,
    );
    // Sort by order property if present, otherwise maintain insertion order
    const sorted = items.sort((a, b) => {
      const orderA = a.order ?? 999;
      const orderB = b.order ?? 999;
      return orderA - orderB;
    });

    return sorted;
  }

  containerItems(container: string) {
    const items = this.items.filter((i) => i.container == container);

    return items;
  }

  groupItems(group: string) {
    const items = this.items.filter((i) => i.group == group);
    return items;
  }
}

export type UtilityBarItem = UtilityWidgetItem | UtilityActionItem;

export interface UtilityItem {
  id: string;
  class?: string;
  label?: string;
  container?: string;
  group?: string;
  visible: boolean;
  count?: number;
  type: string;
  order?: number; // Optional order for sorting items
}

export interface UtilityActionItem extends UtilityItem {
  type: "action";
  action: () => void;
}

export interface UtilityWidgetItem extends UtilityItem {
  type: "widget";
  widget: Object;
  inline?: boolean; // If true, render widget directly without icon/popover
}
