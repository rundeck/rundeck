import {mount, VueWrapper} from '@vue/test-utils'
import {Modal} from 'uiv'
import {listProjects} from '../../../services/projects'
import {storageKeyGetMetadata} from '../../../services/storage'
import KeyStorageEdit from '../../storage/KeyStorageEdit.vue'
import KeyStorageView from '../../storage/KeyStorageView.vue'
import KeyStorageSelector from '../KeyStorageSelector.vue'

jest.mock('@/library/rundeckService', () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    eventBus: {on: jest.fn(), emit: jest.fn()},
    rdBase: 'http://localhost:4440/',
    projectName: 'testProject',
    apiVersion: '44',
  })),
}))
jest.mock('../../../services/projects')
jest.mock('../../../services/storage')
const mockedStorageKeyGetMetadata = storageKeyGetMetadata as jest.MockedFunction<typeof storageKeyGetMetadata>
const mockedListProjects = listProjects as jest.MockedFunction<typeof listProjects>
mockedListProjects.mockResolvedValue([])

const mountKeyStorageSelectorStub = async (props = {}) => {
  return mount(KeyStorageSelector, {
    props: {
      readOnly: false,
      allowUpload: true,
      ...props,
    },
    global: {
      components: {
        Modal,
      },
      mocks: {
        $t: (msg: string) => msg,
      },
      stubs: {
        KeyStorageView: {
          name: 'KeyStorageView',
          props: ['storageFilter', 'modelValue', 'readOnly', 'allowUpload', 'rootPath'],
          template: '<div data-testid="mock-key-storage-view">' +
            '<div data-testid="storageFilter">{{storageFilter}}</div>' +
            '<div data-testid="modelValue">{{modelValue}}</div>' +
            '<div data-testid="readOnly">{{readOnly}}</div>' +
            '<div data-testid="allowUpload">{{allowUpload}}</div>' +
            '<div data-testid="rootPath">{{rootPath}}</div>' +
            '</div>',
        },
        KeyStorageEdit: {
          name: 'KeyStorageEdit',
          props: ['uploadSetting', 'rootPath'],
          template: '<div data-testid="mock-key-storage-edit">' +
            '<div data-testid="uploadSetting">{{uploadSetting}}</div>' +
            '<div data-testid="rootPath">{{rootPath}}</div>' +
            '</div>',
        }
      }
    },
    data() {
      return {}
    },
  })
}
const mountKeyStorageSelector = async (props = {}): Promise<VueWrapper<any>> => {
  return mount(KeyStorageSelector, {
    props: {
      readOnly: false,
      allowUpload: true,
      ...props,
    },
    global: {
      components: {
        Modal, KeyStorageView, KeyStorageEdit
      },
      mocks: {
        $t: (msg: string) => msg,
      },
    },
    data() {
      return {}
    },
  })
}

describe('KeyStorageSelector', () => {

  beforeEach(() => {

    jest.clearAllMocks()
  })

  it.each([
    'Rundeck-key-type=private',
    'Rundeck-key-type=public',
    'Rundeck-data-type=password',
  ])('passes storageSelector %p prop to key storage view', async (storageFilter: string) => {
    const wrapper = await mountKeyStorageSelectorStub({storageFilter})
    await wrapper.find('[data-testid="open-selector-btn"]').trigger('click')

    await wrapper.vm.$nextTick()
    const storageView = wrapper.find('[data-testid="storageFilter"]')
    expect(storageView.text()).toBe(storageFilter)
  })
  it.each([
    'keys/some/path',
    'keys/another/path',
  ])('passes modelValue %p prop to key storage view', async (modelValue: string) => {
    const wrapper = await mountKeyStorageSelectorStub({modelValue})
    await wrapper.find('[data-testid="open-selector-btn"]').trigger('click')

    await wrapper.vm.$nextTick()
    const storageView = wrapper.find('[data-testid="modelValue"]')
    expect(storageView.text()).toBe(modelValue)
  })
  it.each([
    true, false
  ])('passes allowUpload %p prop to key storage view', async (allowUpload: boolean) => {
    const wrapper = await mountKeyStorageSelectorStub({allowUpload})
    await wrapper.find('[data-testid="open-selector-btn"]').trigger('click')

    await wrapper.vm.$nextTick()
    const storageView = wrapper.find('[data-testid="allowUpload"]')
    expect(storageView.text()).toBe(allowUpload.toString())
  })
  it.each([
    true, false
  ])('passes readOnly %p prop to key storage view', async (readOnly: boolean) => {
    const wrapper = await mountKeyStorageSelectorStub({readOnly})
    await wrapper.find('[data-testid="open-selector-btn"]').trigger('click')

    await wrapper.vm.$nextTick()
    const storageView = wrapper.find('[data-testid="readOnly"]')
    expect(storageView.text()).toBe(readOnly.toString())
  })
  it.each([
    'keys'
  ])('passes rootPath %p prop to key storage view', async (rootPath: string) => {
    const wrapper = await mountKeyStorageSelectorStub({})
    await wrapper.find('[data-testid="open-selector-btn"]').trigger('click')

    await wrapper.vm.$nextTick()
    const storageView = wrapper.find('[data-testid="rootPath"]')
    expect(storageView.text()).toBe(rootPath.toString())
  })
  it('opens editor when KeyStorageView upload key button clicked', async () => {
    mockedStorageKeyGetMetadata.mockResolvedValue({
      resources: [
        {
          name: '/myKey',
          path: '/keys/myKey',
          type: 'file',
          meta: {'Rundeck-key-type': 'private'},
        },
        {
          name: '/key2',
          path: '/keys/key2',
          type: 'file',
          meta: {'Rundeck-key-type': 'private'},
        },
      ],
    })
    const wrapper = await mountKeyStorageSelector({})
    await wrapper.find('[data-testid="open-selector-btn"]').trigger('click')
    await wrapper.vm.$nextTick()
    await wrapper.find('[data-testid="add-key-btn"]').trigger('click')
    const uploadModal = wrapper.get('#storageuploadkey')
    expect(uploadModal.isVisible()).toBe(true)
    expect(wrapper.vm.modalEdit).toBe(true)
    expect(wrapper.vm.uploadSetting).toEqual(
      {
        'errorMsg': null,
        'file': '',
        'fileContent': '',
        'fileName': null,
        'inputPath': '',
        'inputType': 'text',
        'keyType': 'privateKey',
        'modifyMode': false,
        'password': '',
        'status': 'new',
        'textArea': '',
      })

  })
  it.each([
    'myKey',
    'key2'
  ])('opens editor with key %p when KeyStorageView overwrite key button clicked', async (keyName: string) => {
    mockedStorageKeyGetMetadata.mockResolvedValue({
      resources: [
        {
          name: 'myKey',
          path: '/keys/myKey',
          type: 'file',
          meta: {'Rundeck-key-type': 'private'},
        },
        {
          name: 'key2',
          path: '/keys/key2',
          type: 'file',
          meta: {'Rundeck-key-type': 'private'},
        },
      ],
    })
    const wrapper = await mountKeyStorageSelector({})
    await wrapper.find('[data-testid="open-selector-btn"]').trigger('click')
    await wrapper.vm.$nextTick()
    //find and click "key2" to select it
    let keys = await wrapper.findAll('tr.action span[data-testid="created-key"]')
    expect(keys.length).toBe(2)
    let found = keys.find((e) => e.text() === keyName)
    expect(found).not.toBeNull()
    await found.trigger('click')
    await wrapper.vm.$nextTick()
    await wrapper.find('[data-testid="overwrite-key-btn"]').trigger('click')
    const uploadModal = wrapper.get('#storageuploadkey')
    expect(uploadModal.isVisible()).toBe(true)
    expect(wrapper.vm.modalEdit).toBe(true)
    expect(wrapper.vm.uploadSetting).toEqual(
      {
        'errorMsg': null,
        'file': '',
        'fileContent': '',
        'fileName': keyName,
        'inputPath': '',
        'inputType': 'text',
        'keyType': 'privateKey',
        'modifyMode': true,
        'password': '',
        'status': 'update',
        'textArea': '',
      })

  })
  it.each([
    'myKey',
    'key2'
  ])('close modal and emit with key %p when KeyStorageView save button clicked', async (keyName: string) => {
    mockedStorageKeyGetMetadata.mockResolvedValue({
      resources: [
        {
          name: 'myKey',
          path: '/keys/myKey',
          type: 'file',
          meta: {'Rundeck-key-type': 'private'},
        },
        {
          name: 'key2',
          path: '/keys/key2',
          type: 'file',
          meta: {'Rundeck-key-type': 'private'},
        },
      ],
    })
    const wrapper = await mountKeyStorageSelector({})
    await wrapper.find('[data-testid="open-selector-btn"]').trigger('click')
    await wrapper.vm.$nextTick()
    expect(wrapper.get('#storage-file').isVisible()).toBe(true)
    //find and click "key2" to select it
    let keys = await wrapper.findAll('tr.action span[data-testid="created-key"]')
    expect(keys.length).toBe(2)
    let found = keys.find((e) => e.text() === keyName)
    expect(found).not.toBeNull()
    await found.trigger('click')
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.selectedKey).toBe(`/keys/${keyName}`)
    //click save button
    await wrapper.find('.modal-footer button.btn-primary').trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('update:modelValue')[0][0]).toBe(`/keys/${keyName}`)

  })
})