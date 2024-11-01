import { BaseLocalStorageInterface } from "../../types/stores/BaseLocalStorageInterface";
import { Cache } from "../../types/stores/Cache";
import { CacheFactory } from "../modules/SimpleCache";
import { StorageFactory } from "../BaseLocalStorageStore";

type CollectionType = { [key: string]: any };

export class BaseStore<T extends Record<string, CollectionType>> {
  protected store: T;
  private cache: Cache<any> | null = null;
  private storage: BaseLocalStorageInterface<T> | null = null;

  constructor(
    initialState: T,
    storageKey?: string,
    useCacheAndStorage: boolean = false,
  ) {
    this.store = initialState;
    if (storageKey) {
      if (useCacheAndStorage) {
        this.cache = CacheFactory.getCache<T>(storageKey);
        this.storage = StorageFactory.getStorage<T>(storageKey);
      } else {
        this.cache = CacheFactory.getCache<T>(storageKey);
      }
      if (this.storage) {
        const storedData = this.storage.load();
        if (storedData) {
          this.store = { ...this.store, ...storedData };
        }
      }
    }
  }

  private generateCacheKey<K extends keyof T>(
    collection: K,
    id: string,
  ): string {
    return `${String(collection)}_${id}`;
  }

  getItem<K extends keyof T>(collection: K, id: string): T[K][string] | null {
    try {
      const cacheKey = this.generateCacheKey(collection, id);
      if (this.cache) {
        const cachedItem = this.cache.get(cacheKey);
        if (cachedItem) {
          return cachedItem;
        }
      }
      const item = this.store[collection][id] || null;
      if (this.cache && item) {
        this.cache.set(cacheKey, item);
      }
      return item;
    } catch (error) {
      console.warn(
        `Error getting item from collection "${String(collection)}" with id "${id}":`,
        error,
      );
      return null;
    }
  }

  setItem<K extends keyof T>(
    collection: K,
    item: T[K][string] & { id: string },
  ): void {
    try {
      const id = item.id;
      (this.store[collection] as CollectionType)[id] = item;
      if (this.cache) {
        const cacheKey = this.generateCacheKey(collection, id);
        this.cache.set(cacheKey, item);
      }
      if (this.storage) {
        this.storage.save(this.store);
      }
    } catch (error) {
      console.warn(
        `Error getting item from collection "${String(collection)}":`,
        error,
      );
    }
  }

  removeItem<K extends keyof T>(collection: K, id: string): void {
    delete this.store[collection][id];
    if (this.cache) {
      const cacheKey = this.generateCacheKey(collection, id);
      this.cache.set(cacheKey, null);
    }
    if (this.storage) {
      this.storage.save(this.store);
    }
  }

  updateItem<K extends keyof T>(
    collection: K,
    id: string,
    updatedItem: Partial<T[K][string]>,
  ): void {
    const currentItem = this.store[collection][id];
    if (currentItem) {
      (this.store[collection] as CollectionType)[id] = {
        ...currentItem,
        ...updatedItem,
      };
      if (this.cache) {
        const cacheKey = this.generateCacheKey(collection, id);
        this.cache.set(cacheKey, this.store[collection][id]);
      }
      if (this.storage) {
        this.storage.save(this.store);
      }
    }
  }

  getAllItems<K extends keyof T>(
    collection: K,
  ): { [key: string]: T[K][string] } {
    return JSON.parse(JSON.stringify(this.store[collection]));
  }

  searchItems<K extends keyof T>(
    collection: K,
    predicate: (value: T[K][string]) => boolean,
  ): { [key: string]: T[K][string] } {
    const result: { [key: string]: T[K][string] } = {};
    for (const key in this.store[collection]) {
      if (
        this.store[collection].hasOwnProperty(key) &&
        predicate(this.store[collection][key])
      ) {
        result[key] = this.store[collection][key];
      }
    }
    return result;
  }

  getPaginatedItems<K extends keyof T>(
    collection: K,
    page: number,
    pageSize: number,
  ): T[K][string][] {
    const allItems = Object.values(this.store[collection]);
    const start = (page - 1) * pageSize;
    const end = start + pageSize;
    return allItems.slice(start, end);
  }
}
