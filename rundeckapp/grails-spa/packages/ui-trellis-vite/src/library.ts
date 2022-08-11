import CopyBox from './library/components/containers/copybox/CopyBox.vue'
import Drawer from './library/components/containers/drawer/Drawer.vue'
// import logEntry from './components/execution-log/logEntry.vue'
// import logEntryFlex from './components/execution-log/logEntryFlex.vue'
// import logViewer from './components/execution-log/logViewer.vue'
// import ExecutionData from './components/execution-log/interfaces/ExecutionData'
// import { LogBuilder } from './components/execution-log/logBuilder'
// import FilterList from './components/filter-list/FilterList.vue'
//@@import FirstRun from './components/first-run/FirstRun.vue'
import HelpIcon from './library/components/help/HelpIcon.vue'
import InputSwitch from './library/components/inputs/Switch.vue'
// import NavBarItem from './components/navbar/NavBarItem.vue'
// import NavBarDrawer from './components/navbar/NavBarDrawer.vue'
// import NavBarContainer from './components/navbar//NavBarContainer.vue'
// import NavBar from './components/navbar/NavBar.vue'
// 
// import pluginPropView from './components/plugins/pluginPropView.vue'
// import pluginPropVal from './components/plugins/pluginPropVal.vue'
// import pluginPropEdit from './components/plugins/pluginPropEdit.vue'
// import pluginConfig from './components/plugins/pluginConfig.vue'
// 
// import i18n from './components/plugins/i18n'
// import ProjectPicker from './components/plugins/ProjectPicker.vue'
// import PluginInfo from './components/plugins/PluginInfo.vue'
// import KeyStorageSelector from './components/plugins/KeyStorageSelector.vue'
import JobConfigPicker from './library/components/plugins/JobConfigPicker.vue'
// 
// import DynamicFormPluginProp from './components/plugins/DynamicFormPluginProp.vue'
// import RundeckLogo from './components/svg/RundeckLogo.vue'
// import PagerdutyLogo from './components/svg/PagerdutyLogo.vue'
// 
// import Skeleton from './components/skeleton/Skeleton.vue'
// 
// import UtilityWidgetItem from './components/utility-bar/UtilityWidgetItem.vue'
// import UtilityBarItem from './components/utility-bar/UtilityBarItem.vue'
// import UtilityBar from './components/utility-bar/UtilityBar.vue'
// import UtilityActionItem from './components/utility-bar/UtilityActionItem.vue'
// import Popper from './components/utility-bar/Popper.vue'
// 
// import Pagination from './components/utils/Pagination.vue'
// import PageConfirm from './components/utils/PageConfirm.vue'
import OffsetPagination from './library/components/utils/OffsetPagination.vue'
// import ExtendedDescription from './components/utils/ExtendedDescription.vue'
// 
// import Expandable from './components/utils/Expandable.vue'
// import AceEditorVue from './components/utils/AceEditorVue'
// import AceEditor from './components/utils/AceEditor.vue'
// 
// import VersionIconNameDisplay from './components/version/VersionIconNameDisplay.vue'
// import VersionDisplay from './components/version/VersionDisplay.vue'
// import VersionDateDisplay from './components/version/VersionDateDisplay.vue'
// import ServerDisplay from './components/version/ServerDisplay.vue'
import RundeckVersion from './library/components/version/RundeckVersionDisplay.vue'
// import Copyright from './components/version/Copyright.vue'
// 
// import News from './components/widgets/news/News.vue'
// import ProjectSelectButton from './components/widgets/project-select/ProjectSelectButton.vue'
// import ProjectSelect from './components/widgets/project-select/ProjectSelect.vue'
// 
// import RundeckInfoWidget from './components/widgets/rundeck-info/RundeckInfoWidget.vue'
// import RundeckInfo from './components/widgets/rundeck-info/RundeckInfo.vue'
// import ThemeSelect from './components/widgets/theme-select/ThemeSelect.vue'
// import WebhookSelectItem from './components/widgets/webhook-select/WebhookSelectItem.vue'
// import WebhookSelect from './components/widgets/webhook-select/WebhookSelect.vue'
// 

/*
import Tab from './components/containers/tabs/Tab'
import TabBar from './components/containers/tabs/TabBar.vue'
import TabContent from './components/containers/tabs/TabContent.vue'
import Tabs from './components/containers/tabs/Tabs'
*/


import Tabs from './library/components/containers/tabs/Tabs.vue'
import Tab from './library/components/containers/tabs/Tab.vue'

import { URL } from 'url'
import {RundeckToken} from './library/interfaces/rundeckWindow'
import {AppLinks} from './library/interfaces/AppLinks'

export { 
  //@@FirstRun,
  CopyBox,
  Drawer,
  /*
  Tab,
  TabBar,
  TabContent,
  Tabs,
  */
  Tabs,
  Tab,
//   logEntry,
//   logEntryFlex,
//   logViewer,
//   ExecutionData,
//   LogBuilder,
//   FilterList,
  HelpIcon,
  InputSwitch,
//   NavBarItem,
//   NavBarDrawer,
//   NavBarContainer,
//   NavBar,
//   pluginPropView,
//   pluginPropVal,
//   pluginPropEdit,
//   pluginConfig,
//   i18n,
//   ProjectPicker,
//   PluginInfo,
//   KeyStorageSelector,
JobConfigPicker,
//   DynamicFormPluginProp,
//   RundeckLogo,
//   PagerdutyLogo,
//   Skeleton,
//   UtilityWidgetItem,
//   UtilityBarItem,
//   UtilityBar,
//   UtilityActionItem,
//   Popper,
//   Pagination,
//   PageConfirm,
OffsetPagination,
//   ExtendedDescription,
//   Expandable,
//   AceEditorVue,
//   AceEditor,
//   VersionIconNameDisplay,
//   VersionDisplay,
//   VersionDateDisplay,
//   ServerDisplay,
RundeckVersion,
//   Copyright,
//   News,
//   ProjectSelectButton,
//   ProjectSelect,
//   RundeckInfoWidget,
//   RundeckInfo,
//   ThemeSelect,
//   WebhookSelectItem,
//   WebhookSelect
}


export function getRundeckContext() {
  return window._rundeck
}

export function getAppLinks() {
  return window.appLinks
}

export function getSynchronizerToken() {
  const tokenString = document.getElementById('web_ui_token')!.innerText
  return JSON.parse(tokenString) as RundeckToken
}

/**
* Generate link by joining with the Rundeck base path
*/
export function url(path: string) {
  const context = getRundeckContext()
  return new window.URL(path, context.rdBase)
}