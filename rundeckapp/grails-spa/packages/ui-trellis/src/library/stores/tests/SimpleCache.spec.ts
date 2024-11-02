import { SimpleCache, CacheFactory } from "../modules/SimpleCache";

describe("SimpleCache", () => {
  let cache: SimpleCache<any>;

  beforeEach(() => {
    cache = new SimpleCache();
  });

  it("should set and get a value", () => {
    const key = "testKey";
    const value = { foo: "bar" };

    cache.set(key, value);
    const retrievedValue = cache.get(key);

    expect(retrievedValue).toEqual(value);
  });

  it("should return null for non-existent key", () => {
    const nonExistentKey = "nonExistentKey";
    const retrievedValue = cache.get(nonExistentKey);

    expect(retrievedValue).toBeNull();
  });

  it("should overwrite existing value", () => {
    const key = "testKey";
    const initialValue = { foo: "bar" };
    const newValue = { foo: "baz" };

    cache.set(key, initialValue);
    cache.set(key, newValue);
    const retrievedValue = cache.get(key);

    expect(retrievedValue).toEqual(newValue);
  });

  it("should clear all cached values", () => {
    cache.set("key1", "value1");
    cache.set("key2", "value2");

    cache.clear();

    expect(cache.get("key1")).toBeNull();
    expect(cache.get("key2")).toBeNull();
  });
});

describe("CacheFactory", () => {
  beforeEach(() => {
    (CacheFactory as any).cacheInstances = {};
  });

  it("should create a new cache instance if it doesn't exist", () => {
    const cache1 = CacheFactory.getCache("cache1");
    expect(cache1).toBeInstanceOf(SimpleCache);
  });

  it("should return the same cache instance for the same name", () => {
    const cache1 = CacheFactory.getCache("cache1");
    const cache1Again = CacheFactory.getCache("cache1");
    expect(cache1).toBe(cache1Again);
  });

  it("should create different cache instances for different names", () => {
    const cache1 = CacheFactory.getCache("cache1");
    const cache2 = CacheFactory.getCache("cache2");
    expect(cache1).not.toBe(cache2);
  });

  it("should maintain separate data for different cache instances", () => {
    const cache1 = CacheFactory.getCache<string>("cache1");
    const cache2 = CacheFactory.getCache<number>("cache2");

    cache1.set("key1", "value1");
    cache2.set("key2", 42);

    expect(cache1.get("key1")).toBe("value1");
    expect(cache2.get("key2")).toBe(42);
    expect(cache1.get("key2")).toBeNull();
    expect(cache2.get("key1")).toBeNull();
  });
});
