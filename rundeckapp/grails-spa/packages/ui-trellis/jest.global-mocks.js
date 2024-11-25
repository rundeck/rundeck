import { config } from "@vue/test-utils";
import { Btn, Dropdown, Modal } from "uiv";

// Globally stub components - to disable, on mount/shallowMount pass global.stubs.[componentName] = false
// it's also possible to pass another stub
// config.global.stubs = {
// };

// To disable global components - to disable, on mount pass global.stubs.[componentName] = true
config.global.components = {
  Btn,
  Dropdown,
  Modal,
};

// same thing with global.mocks, on mount/shallowMount pass global.mocks.$t = new code
config.global.mocks = {
  $t: (msg) => msg,
  $tc: (msg) => msg,
};

// todo: open a separate PR enabling these mocks and clean up tests
// const mockRundeckBrowser = jest.fn().mockImplementation(() => ({
//   eventBus: { on: jest.fn(), emit: jest.fn() },
// }));

// jest.mock("@/library/modules/rundeckClient", () => ({
//   client: jest.fn(),
// }));
//
// jest.mock("@/library", () => {
//   return {
//     getRundeckContext: jest.fn().mockImplementation(() => ({
//       client: new mockRundeckBrowser(),
//       eventBus: { on: jest.fn(), emit: jest.fn() },
//     })),
//   };
// });
//
// // can leverage the mockRundeckBrowser for other mocks
// export { mockRundeckBrowser };
