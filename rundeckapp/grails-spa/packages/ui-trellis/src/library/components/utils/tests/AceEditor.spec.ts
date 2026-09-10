import { mount, VueWrapper } from "@vue/test-utils";
import AceEditor from "../AceEditor.vue";

interface MountOptions {
  props?: Record<string, unknown>;
}

const createWrapper = async (
  options: MountOptions = {},
): Promise<VueWrapper<InstanceType<typeof AceEditor>>> => {
  const wrapper = mount(AceEditor, {
    props: {
      modelValue: "",
      codeSyntaxSelectable: true,
      softWrapControl: true,
      ...options.props,
    },
    global: {
      stubs: {
        Ace: {
          template: '<div data-testid="ace-editor-stub"></div>',
        },
      },
    },
  });

  await wrapper.vm.$nextTick();

  return wrapper;
};

const findByTestId = (
  wrapper: VueWrapper<InstanceType<typeof AceEditor>>,
  testId: string,
) => wrapper.find(`[data-testid="${testId}"]`);

describe("AceEditor", () => {
  it("uses the identifier prop to link the syntax label and select", async () => {
    const wrapper = await createWrapper({
      props: {
        identifier: "job-editor",
        softWrapControl: false,
      },
    });

    expect(
      findByTestId(wrapper, "ace-editor-syntax-label").attributes("for"),
    ).toBe("job-editor-syntax");
    expect(
      findByTestId(wrapper, "ace-editor-syntax-select").attributes("id"),
    ).toBe("job-editor-syntax");
  });

  it("generates unique syntax and wrap ids when identifier is not provided", async () => {
    const firstWrapper = await createWrapper();
    const secondWrapper = await createWrapper();

    const firstSyntaxId = findByTestId(
      firstWrapper,
      "ace-editor-syntax-select",
    ).attributes("id");
    const secondSyntaxId = findByTestId(
      secondWrapper,
      "ace-editor-syntax-select",
    ).attributes("id");
    const firstWrapId = findByTestId(
      firstWrapper,
      "ace-editor-wrap-checkbox",
    ).attributes("id");
    const secondWrapId = findByTestId(
      secondWrapper,
      "ace-editor-wrap-checkbox",
    ).attributes("id");

    expect(firstSyntaxId).toBe(
      findByTestId(firstWrapper, "ace-editor-syntax-label").attributes("for"),
    );
    expect(secondSyntaxId).toBe(
      findByTestId(secondWrapper, "ace-editor-syntax-label").attributes("for"),
    );
    expect(firstWrapId).toBe(
      findByTestId(firstWrapper, "ace-editor-wrap-label").attributes("for"),
    );
    expect(secondWrapId).toBe(
      findByTestId(secondWrapper, "ace-editor-wrap-label").attributes("for"),
    );
    expect(firstSyntaxId).not.toBe(secondSyntaxId);
    expect(firstWrapId).not.toBe(secondWrapId);
  });

  it("does not render the syntax selector when code syntax selection is disabled", async () => {
    const wrapper = await createWrapper({
      props: {
        codeSyntaxSelectable: false,
      },
    });

    expect(findByTestId(wrapper, "ace-editor-syntax-label").exists()).toBe(
      false,
    );
    expect(findByTestId(wrapper, "ace-editor-syntax-select").exists()).toBe(
      false,
    );
  });

  it("does not render the soft wrap control when soft wrap selection is disabled", async () => {
    const wrapper = await createWrapper({
      props: {
        softWrapControl: false,
      },
    });

    expect(findByTestId(wrapper, "ace-editor-wrap-label").exists()).toBe(false);
    expect(findByTestId(wrapper, "ace-editor-wrap-checkbox").exists()).toBe(
      false,
    );
  });
});
