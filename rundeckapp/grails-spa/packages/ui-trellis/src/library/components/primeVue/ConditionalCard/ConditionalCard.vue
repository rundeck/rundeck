<template>
  <Card class="conditionalCard">
    <template #header>
      <div class="conditionalCard--header">
        <div>
          <div>
            <plugin-info
                :detail="{
                  iconUrl: 'public/images/icon-condition.png',
                  title: 'For each Linux node',
                }"
              :show-description="false"
              :show-extended="false"
            />
          </div>
          <div class="conditionalCard--header-description">
            <Tag class="tag-node" value="Node Step" />
            <p>Conditional Logic on Node Step</p>
            <i class="fa fa-info-circle" v-tooltip="{ value: 'aaaaaaa' }"></i>
          </div>
        </div>
        <div class="conditionalCard--header-buttons">
          <PtButton icon="glyphicon glyphicon-trash" aria-label="Delete" v-tooltip.top="'Delete this step'" />
          <PtButton icon="fa fa-ellipsis-h" />
          <Menu ref="menu" id="overlay_menu" :model="[]" :popup="true" />
        </div>
      </div>
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
                  <span @click.stop="yolo">Run command to stop crowdstrike agent</span>
                </template>
              </plugin-info>
            </AccordionHeader>
            <AccordionContent>
              <div>
                Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
                eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut
                enim ad minim veniam, quis nostrud exercitation ullamco laboris
                nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor
                in reprehenderit in voluptate velit esse cillum dolore eu fugiat
                nulla pariatur. Excepteur sint occaecat cupidatat non proident,
                sunt in culpa qui officia deserunt mollit anim id est laborum.
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
                              <AccordionPanel value="1">
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
import { defineComponent } from "vue";
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
import PtButton from "../PtButton/PtButton.vue";



import "../Tag/tag.scss";
import "../Tooltip/tooltip.scss";


export default defineComponent({
  name: "ConditionalCard",
  components: {
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
      test: {"iconUrl":"http://localhost:4440/plugin/icon/WorkflowNodeStep/exec-command","title":"Command","builtin":false,"id":"4ca0a4eec11b","providerMetadata":null,"service":"WorkflowNodeStep","highlightedOrder":0,"artifactName":"Script Node Step Plugin","pluginVersion":"5.15.0-SNAPSHOT","description":"Execute a remote command","isHighlighted":true,"name":"exec-command","author":"Rundeck, Inc."}
    }
  },
  methods: {
    yolo() {
      alert('yo')
    }
  }
});
</script>

<style lang="scss">
.conditionalCard {
  box-shadow: none;
  overflow: hidden;
  border-radius: var(--radii-md);
  border: 1px solid var(--colors-gray-300);

  p, a, span:not(.glyphicon, .fa), {
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
      gap: var(--sizes-1);
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
</style>
