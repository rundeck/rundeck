import { BaseLocalStorageInterface } from "../types/stores/BaseLocalStorageInterface";

export class BaseLocalStorageStore<T> implements BaseLocalStorageInterface<T> {
  private key: string;

  constructor(key: string) {
    this.key = key;
  }

  async load(): Promise<T> {
    const rawData = localStorage.getItem(this.key);
    if (rawData) {
      try {
        return JSON.parse(rawData);
      } catch (error) {
        localStorage.removeItem(this.key);
        console.warn(
          `Failed to load data from localStorage for key ${this.key}:`,
          error,
        );
        return null;
      }
    }
    return {} as T;
  }

  async save(data: T) {
    try {
      localStorage.setItem(this.key, JSON.stringify(data));
    } catch (error) {
      console.warn(
        `Error saving data to localStorage for key "${this.key}":`,
        error,
      );
    }
  }
}

export class StorageFactory {
  private static storageInstances: {
    [key: string]: BaseLocalStorageInterface<any>;
  } = {};

  static getStorage<T>(name: string): BaseLocalStorageInterface<T> {
    if (!this.storageInstances[name]) {
      this.storageInstances[name] = new BaseLocalStorageStore<T>(name);
    }
    return this.storageInstances[name];
  }
}
