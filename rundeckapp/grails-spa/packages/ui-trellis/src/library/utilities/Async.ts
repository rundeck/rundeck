/** Ensures that an async method only has one oustanding promise
 * effectively serializing the coroutines.
 */
export function Serial(
  target: object,
  key: string | symbol,
  prop: PropertyDescriptor,
) {
  const context = {
    prom: undefined as Promise<void> | undefined,
  };
  const original = prop.value as (...args: unknown[]) => unknown;

  prop.value = function (this: unknown, ...args: unknown[]) {
    if (context.prom) return context.prom;

    const serialFunc = async () => {
      try {
        await original.apply(this, args);
      } finally {
        context.prom = undefined;
      }
    };
    context.prom = serialFunc();
    return context.prom;
  };
}
