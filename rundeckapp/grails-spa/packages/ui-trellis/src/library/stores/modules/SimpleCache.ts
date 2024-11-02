import { Cache } from "../../types/stores/Cache";

export class SimpleCache<T> implements Cache<T> {
  private cachedValues: Map<string, T> = new Map();

  get(key: string): T | null {
    return this.cachedValues.get(key) || null;
  }

  set(key: string, value: T): void {
    this.cachedValues.set(key, value);
  }

  clear(): void {
    this.cachedValues.clear();
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
