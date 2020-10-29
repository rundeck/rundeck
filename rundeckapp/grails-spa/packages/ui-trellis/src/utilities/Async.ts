

/** Ensures that an async method only has one oustanding promise
 * effectively serializing the coroutines.
 */
export function Serial(target: Object, key: string | symbol, prop: PropertyDescriptor) {
    const context = {
        prom: undefined as Promise<any> | undefined
    }
    const original = prop.value

    prop.value = function(...args: any) {
        if (context.prom)
            return context.prom

        const serialFunc = async () => {
            try {
                const res = await original.apply(this, args)
            } finally {
                context.prom = undefined
            }
        }
        context.prom = serialFunc()
        return context.prom
    }
}