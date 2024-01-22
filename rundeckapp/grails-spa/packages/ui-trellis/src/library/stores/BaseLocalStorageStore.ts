import {BaseLocalStorageInterface} from "../types/stores/BaseLocalStorageInterface";

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
                console.warn("Failed to load data from localStorage");
            }
        }
        return {} as T;
    }

    async store(data: T) {
        localStorage.setItem(this.key, JSON.stringify(data));
    }
}
