export type ComponentStub = boolean | { template: string };
export type Stubs = string[] | Record<string, ComponentStub>;

export interface Directive {
  beforeMount?: (
    el: HTMLElement,
    binding: any,
    vnode: any,
    prevVnode: any,
  ) => void;
  mounted?: (el: HTMLElement, binding: any, vnode: any, prevVnode: any) => void;
  beforeUpdate?: (
    el: HTMLElement,
    binding: any,
    vnode: any,
    prevVnode: any,
  ) => void;
  updated?: (el: HTMLElement, binding: any, vnode: any, prevVnode: any) => void;
  beforeUnmount?: (
    el: HTMLElement,
    binding: any,
    vnode: any,
    prevVnode: any,
  ) => void;
  unmounted?: (
    el: HTMLElement,
    binding: any,
    vnode: any,
    prevVnode: any,
  ) => void;
}

export interface GlobalOptions {
  stubs?: Stubs;
  mocks?: Record<string, unknown>;
  directives?: Record<string, Directive>;
}
