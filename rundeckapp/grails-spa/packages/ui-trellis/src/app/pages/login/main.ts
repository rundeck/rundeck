import 'vite/modulepreload-polyfill'
import { ThemeStore } from '../../../library/stores/Theme'
import SvgInject from '@iconfu/svg-inject'
// @ts-ignore
window.SVGInject = SvgInject
new ThemeStore()
