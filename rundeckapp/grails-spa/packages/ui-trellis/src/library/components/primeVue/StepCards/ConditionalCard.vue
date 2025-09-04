<template>
  <Card class="conditionalCard">
    <template #header>
      <StepCardHeader :handle-more-actions="handleMoreActions"/>
    </template>
    <template #content>
      <Conditional>
        <Accordion multiple>
          <AccordionPanel value="0">
            <AccordionHeader>
              <plugin-info
                class="conditionalCard--step-description"
                :detail="test"
                :show-description="false"
                :show-title="false"
                :show-extended="false"
              >
                <template #titleprefix>
                  <span @click.stop="yolo">
                    Run command to stop crowdstrike agent
                  </span>
                </template>
              </plugin-info>
            </AccordionHeader>
            <AccordionContent>
              <div>
                <plugin-config
                  serviceName="WorkflowStep"
                  provider="jira-create-issue"
                  :config="jiraStep.config"
                  :readOnly="true"
                  :showTitle="false"
                  :showIcon="false"
                  :showDescription="false"
                  mode="show"
                />

<!--                <log-filters-->
<!--                  :model-value="[]"-->
<!--                  subtitle=""-->
<!--                  mode="inline"-->
<!--                  title=""-->
<!--                />-->
                <error-handler-step :step="jiraStep" />
              </div>
            </AccordionContent>
          </AccordionPanel>
          <AccordionPanel value="1">
            <AccordionHeader>
              <span class="conditionalCard--step-description"
                >Stop the existing crowdstrike agent</span
              >
            </AccordionHeader>
            <AccordionContent>
              <p class="m-0">
                Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
                eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut
                enim ad minim veniam, quis nostrud exercitation ullamco laboris
                nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor
                in reprehenderit in voluptate velit esse cillum dolore eu fugiat
                nulla pariatur. Excepteur sint occaecat cupidatat non proident,
                sunt in culpa qui officia deserunt mollit anim id est laborum.
              </p>
            </AccordionContent>
          </AccordionPanel>
          <AccordionPanel value="2" class="align-start">
            <AccordionHeader asChild>
              <div class="p-accordionheader nested">
                <Conditional>
                  <Accordion multiple>
                    <AccordionPanel value="0">
                      <AccordionHeader>
                        <span class="conditionalCard--step-description">
                          Stop the existing crowdstrike agent
                        </span>
                      </AccordionHeader>
                      <AccordionContent>
                        <div>
                          Lorem ipsum dolor sit amet, consectetur adipiscing
                          elit, sed do eiusmod tempor incididunt ut labore et
                          dolore magna aliqua. Ut enim ad minim veniam, quis
                          nostrud exercitation ullamco laboris nisi ut aliquip
                          ex ea commodo consequat. Duis aute irure dolor in
                          reprehenderit in voluptate velit esse cillum dolore eu
                          fugiat nulla pariatur. Excepteur sint occaecat
                          cupidatat non proident, sunt in culpa qui officia
                          deserunt mollit anim id est laborum.
                        </div>
                      </AccordionContent>
                    </AccordionPanel>
                    <AccordionPanel value="1">
                      <AccordionHeader asChild>
                        <div class="p-accordionheader nested inner-child">
                          <Conditional>
                            <Accordion multiple>
                              <AccordionPanel value="0">
                                <AccordionHeader
                                  ><span
                                    class="conditionalCard--step-description"
                                    >Stop the existing crowdstrike agent</span
                                  ></AccordionHeader
                                >
                                <AccordionContent>
                                  <div>
                                    Lorem ipsum dolor sit amet, consectetur
                                    adipiscing elit, sed do eiusmod tempor
                                    incididunt ut labore et dolore magna aliqua.
                                    Ut enim ad minim veniam, quis nostrud
                                    exercitation ullamco laboris nisi ut aliquip
                                    ex ea commodo consequat. Duis aute irure
                                    dolor in reprehenderit in voluptate velit
                                    esse cillum dolore eu fugiat nulla pariatur.
                                    Excepteur sint occaecat cupidatat non
                                    proident, sunt in culpa qui officia deserunt
                                    mollit anim id est laborum.
                                  </div>
                                </AccordionContent>
                              </AccordionPanel>
                              <AccordionPanel value="1">
                                <AccordionHeader
                                  ><span
                                    class="conditionalCard--step-description"
                                    >Stop the existing crowdstrike agent</span
                                  ></AccordionHeader
                                >
                                <AccordionContent>
                                  <div>
                                    Lorem ipsum dolor sit amet, consectetur
                                    adipiscing elit, sed do eiusmod tempor
                                    incididunt ut labore et dolore magna aliqua.
                                    Ut enim ad minim veniam, quis nostrud
                                    exercitation ullamco laboris nisi ut aliquip
                                    ex ea commodo consequat. Duis aute irure
                                    dolor in reprehenderit in voluptate velit
                                    esse cillum dolore eu fugiat nulla pariatur.
                                    Excepteur sint occaecat cupidatat non
                                    proident, sunt in culpa qui officia deserunt
                                    mollit anim id est laborum.
                                  </div>
                                </AccordionContent>
                              </AccordionPanel>
                            </Accordion>
                          </Conditional>
                        </div>
                      </AccordionHeader>
                    </AccordionPanel>
                  </Accordion>
                </Conditional>
              </div>
            </AccordionHeader>
          </AccordionPanel>
          <AccordionPanel value="3" class="align-start">
            <AccordionHeader asChild>
              <div class="p-accordionheader nested">
                <Conditional>
                  <Accordion multiple>
                    <AccordionPanel value="0">
                      <AccordionHeader>
                        <span class="conditionalCard--step-description">
                          Stop the existing crowdstrike agent
                        </span>
                      </AccordionHeader>
                      <AccordionContent>
                        <div>
                          Lorem ipsum dolor sit amet, consectetur adipiscing
                          elit, sed do eiusmod tempor incididunt ut labore et
                          dolore magna aliqua. Ut enim ad minim veniam, quis
                          nostrud exercitation ullamco laboris nisi ut aliquip
                          ex ea commodo consequat. Duis aute irure dolor in
                          reprehenderit in voluptate velit esse cillum dolore eu
                          fugiat nulla pariatur. Excepteur sint occaecat
                          cupidatat non proident, sunt in culpa qui officia
                          deserunt mollit anim id est laborum.
                        </div>
                      </AccordionContent>
                    </AccordionPanel>
                    <AccordionPanel value="1">
                      <AccordionHeader>
                        <span class="conditionalCard--step-description">
                          Stop the existing crowdstrike agent
                        </span>
                      </AccordionHeader>
                      <AccordionContent>
                        <div>
                          Lorem ipsum dolor sit amet, consectetur adipiscing
                          elit, sed do eiusmod tempor incididunt ut labore et
                          dolore magna aliqua. Ut enim ad minim veniam, quis
                          nostrud exercitation ullamco laboris nisi ut aliquip
                          ex ea commodo consequat. Duis aute irure dolor in
                          reprehenderit in voluptate velit esse cillum dolore eu
                          fugiat nulla pariatur. Excepteur sint occaecat
                          cupidatat non proident, sunt in culpa qui officia
                          deserunt mollit anim id est laborum.
                        </div>
                      </AccordionContent>
                    </AccordionPanel>
                  </Accordion>
                </Conditional>
              </div>
            </AccordionHeader>
          </AccordionPanel>
          <AccordionPanel value="4" class="align-start">
            <AccordionHeader asChild>
              <div class="p-accordionheader nested">
                <Conditional>
                  <Accordion multiple>
                    <AccordionPanel value="0">
                      <AccordionHeader
                        ><span class="conditionalCard--step-description"
                          >Stop the existing crowdstrike agent</span
                        ></AccordionHeader
                      >
                      <AccordionContent>
                        <div>
                          Lorem ipsum dolor sit amet, consectetur adipiscing
                          elit, sed do eiusmod tempor incididunt ut labore et
                          dolore magna aliqua. Ut enim ad minim veniam, quis
                          nostrud exercitation ullamco laboris nisi ut aliquip
                          ex ea commodo consequat. Duis aute irure dolor in
                          reprehenderit in voluptate velit esse cillum dolore eu
                          fugiat nulla pariatur. Excepteur sint occaecat
                          cupidatat non proident, sunt in culpa qui officia
                          deserunt mollit anim id est laborum.
                        </div>
                      </AccordionContent>
                    </AccordionPanel>
                  </Accordion>
                </Conditional>
              </div>
            </AccordionHeader>
          </AccordionPanel>
        </Accordion>
      </Conditional>
    </template>
  </Card>
</template>

<script lang="ts">
import {defineComponent} from "vue";
import Card from "primevue/card";
import Accordion from "primevue/accordion";
import AccordionPanel from "primevue/accordionpanel";
import AccordionHeader from "primevue/accordionheader";
import AccordionContent from "primevue/accordioncontent";
import Tag from "primevue/tag";
import Menu from "primevue/menu";
import Button from "primevue/button";
import Conditional from "./Conditional.vue";
import PluginInfo from "../../plugins/PluginInfo.vue";
import PluginConfig from "../../plugins/pluginConfig.vue";
import PtButton from "../PtButton/PtButton.vue";

import "../Tag/tag.scss";
import "../Tooltip/tooltip.scss";
import LogFilters from "@/app/components/job/workflow/LogFilters.vue";
import ErrorHandlerStep from "@/app/components/job/workflow/ErrorHandlerStep.vue";
import StepCardHeader from "@/library/components/primeVue/StepCards/StepCardHeader.vue";

export default defineComponent({
  name: "ConditionalCard",
  components: {
    StepCardHeader,
    ErrorHandlerStep,
    LogFilters,
    PluginConfig,
    PtButton,
    PluginInfo,
    Conditional,
    Accordion,
    AccordionPanel,
    AccordionHeader,
    AccordionContent,
    Button,
    Card,
    Menu,
    Tag,
  },
  props: {},
  data() {
    return {
      test: {
        iconUrl:
          "http://localhost:4440/plugin/icon/WorkflowNodeStep/exec-command",
        title: "Command",
        builtin: false,
        id: "4ca0a4eec11b",
        providerMetadata: null,
        service: "WorkflowNodeStep",
        highlightedOrder: 0,
        artifactName: "Script Node Step Plugin",
        pluginVersion: "5.15.0-SNAPSHOT",
        description: "Execute a remote command",
        isHighlighted: true,
        name: "exec-command",
        author: "Rundeck, Inc.",
      },
      jiraStep: {
        description: null,
        id: "4wy1dg",
        filters: [],
        type: "jira-create-issue",
        config: {
          customFieldsUserInput: "[]",
          description:
              "${option.jira-description}  GitHub Link: ${option.github-url}",
          project: "${option.jira-project}",
          summary: "${option.jira-summary}",
          type: "Task",
        },
        nodeStep: true,
        errorhandler: {
          description: null,
          jobref: null,
          id:"s6jvwa",
          filters:[],
          type:"jira-create-issue",
          nodeStep:false,
          keepgoingOnSuccess:true,
          config: {
            customFieldsUserInput: "[]",
            description:
                "${option.jira-description}  GitHub Link: ${option.github-url}",
            project: "${option.jira-project}",
            summary: "${option.jira-summary}",
            type: "Task",
          },
        }
      }
    };
  },
  methods: {
    yolo() {
      alert("edit mode");
    },
    handleMoreActions(event) {
      this.$refs.menu.toggle(event);
    },
  },
});
</script>

<style lang="scss">
.conditionalCard {
  box-shadow: none;
  overflow: hidden;
  border-radius: var(--radii-md);
  border: 1px solid var(--colors-gray-300);

  p,
  a,
  span:not(.glyphicon, .fa) {
    font-family: Inter, var(--fonts-body) !important;
  }

  &--header {
    background-color: var(--colors-secondaryBackgroundOnLight);
    border-bottom: 2px solid var(--colors-gray-300);
    display: flex;
    justify-content: space-between;
    padding: var(--sizes-4);

    &-description {
      align-items: baseline;
      display: flex;
      flex-direction: row;
      gap: var(--sizes-1);
      margin-top: var(--sizes-3);

      p {
        font-weight: var(--fontWeights-medium);
        font-size: var(--fontSizes-md);
        margin: 0;
      }
    }

    &-buttons {
      align-items: flex-start;
      display: flex;
      gap: var(--sizes-2);
    }
  }

  &--step {
    &-description {
      flex: 1 1 auto;
      display: flex;
      gap: var(--sizes-2);
    }
  }

  .p-card-body {
    padding: var(--sizes-4);
  }
}

.p-menu-list {
  margin: 0;
  padding: 0.25rem 0.25rem;
}
</style>
