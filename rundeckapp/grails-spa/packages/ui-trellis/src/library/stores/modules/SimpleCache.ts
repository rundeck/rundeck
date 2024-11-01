import { Cache } from "../../types/stores/Cache";

export class SimpleCache<T> implements Cache<T> {
  private cache: Map<string, T> = new Map();

  get(key: string): T | null {
    return this.cache.get(key) || null;
  }

  set(key: string, value: T): void {
    this.cache.set(key, value);
  }

  clear(): void {
    this.cache.clear();
  }
}

export class CacheFactory {
  private static cacheInstances: { [key: string]: Cache<any> } = {};

  static getCache<T>(name: string): Cache<T> {
    if (!this.cacheInstances[name]) {
      this.cacheInstances[name] = new SimpleCache<T>();
    }
    return this.cacheInstances[name];
  }
}
