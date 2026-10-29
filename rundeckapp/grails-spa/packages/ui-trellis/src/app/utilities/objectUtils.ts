type UnknownRecord = Record<string, unknown>;

const isObject = (item: unknown): item is UnknownRecord => {
  return Boolean(item) && typeof item === "object" && !Array.isArray(item);
};

const mergeDeep = <T extends UnknownRecord>(
  target: T,
  ...sources: unknown[]
): T => {
  if (!sources.length) return target;
  const source = sources.shift();

  if (isObject(target) && isObject(source)) {
    for (const key in source) {
      if (isObject(source[key])) {
        if (!target[key])
          Object.assign(target, {
            [key]: {},
          });
        mergeDeep(target[key] as UnknownRecord, source[key]);
      } else {
        Object.assign(target, {
          [key]: source[key],
        });
      }
    }
  }

  return mergeDeep(target, ...sources);
};

export { mergeDeep };
