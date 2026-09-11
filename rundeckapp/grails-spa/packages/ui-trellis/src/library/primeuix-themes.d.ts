// @primeuix/themes 1.2.x moved preset entry points under dist/, reachable only via the
// package's "exports" map. tsc's "node" moduleResolution ignores that map and looks for a
// literal `lara/index.d.ts` at the package root, which no longer exists. This shim restores
// the subpath for type-checking without changing the project's moduleResolution setting.
declare module "@primeuix/themes/lara" {
  import type { Preset } from "@primeuix/themes/types";
  const preset: Preset;
  export default preset;
}
