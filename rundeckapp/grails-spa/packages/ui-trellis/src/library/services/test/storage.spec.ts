import {afterEach, beforeEach, describe, it, jest,} from '@jest/globals'
import axios from 'axios'

import KeyType from '../../types/KeyType'
import {api} from '../api'
import {
  storageKeyCreate,
  storageKeyDelete,
  storageKeyExists,
  storageKeyGetMetadata,
  StorageKeyListResponse,
  StorageKeyMetadata,
  storageKeyUpdate
} from '../storage'

jest.mock('@/library/rundeckService', () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    eventBus: {on: jest.fn(), emit: jest.fn()},
    rdBase: 'http://localhost:4440/',
    projectName: 'testProject',
    apiVersion: '44',
  })),
}))
jest.mock('../api')
const axiosMock = api as jest.Mocked<typeof axios>
describe('storage key service', () => {

  beforeEach(() => {
  })
  afterEach(() => {
    jest.clearAllMocks()
    jest.resetAllMocks()
  })

  it.each([
    [KeyType.Public, 'application/pgp-keys'],
    [KeyType.Private, 'application/octet-stream'],
    [KeyType.Password, 'application/x-rundeck-data-password'],
  ])('creates %p keys with correct content type %p', async (type: KeyType, ctype: string) => {

    axiosMock.post.mockResolvedValue({
      status: 201,
      data: {} as StorageKeyMetadata
    })
    let result = await storageKeyCreate('somepath', 'someval', {
      type
    })
    expect(axiosMock.post).toHaveBeenCalledWith('storage/keys/somepath', 'someval', {
      headers: {
        'Content-Type': ctype,
        'Accept': 'application/json',
      }
    })

  })

  it.each([
    [KeyType.Public, 'application/pgp-keys'],
    [KeyType.Private, 'application/octet-stream'],
    [KeyType.Password, 'application/x-rundeck-data-password'],
  ])('updates %p keys with correct content type %p', async (type: KeyType, ctype: string) => {

    axiosMock.put.mockResolvedValue({
      status: 200,
      data: {} as StorageKeyMetadata
    })
    let result = await storageKeyUpdate('somepath', 'someval', {
      type
    })
    expect(axiosMock.put).toHaveBeenCalledWith('storage/keys/somepath', 'someval', {
      headers: {
        'Content-Type': ctype,
        'Accept': 'application/json',
      }
    })

  })
  it('gets key exists', async () => {
    axiosMock.get.mockResolvedValue({
      status: 200,
      data: {} as StorageKeyMetadata
    })
    let result = await storageKeyExists('somepath')
    expect(axiosMock.get).toHaveBeenCalledWith('storage/keys/somepath')
    expect(result).toEqual(true)
  })
  it('gets key exists 404', async () => {
    axiosMock.get.mockResolvedValue({
      status: 404,
      data: {} as StorageKeyMetadata
    })
    let result = await storageKeyExists('somepath')
    expect(axiosMock.get).toHaveBeenCalledWith('storage/keys/somepath')
    expect(result).toEqual(false)
  })

  it('gets key metadata', async () => {
    let data = {
      resources:[
        {
          "path": "keys/dingo.key",
          "type": "file",
          "name": "dingo.key",
          "url": "http://localhost:4440/rundeckpro/api/52/storage/keys/dingo.key",
          "meta": {
            "Rundeck-content-type": "application/octet-stream",
            "Rundeck-content-creation-time": "2024-12-18T00:50:57Z",
            "Rundeck-content-modify-time": "2024-12-18T00:50:57Z",
            "Rundeck-auth-created-username": "admin",
            "Rundeck-auth-modified-username": "admin",
            "Rundeck-key-type": "private",
            "Rundeck-content-mask": "content"
          }
        },
        {
          "path": "keys/arfarf",
          "type": "file",
          "name": "arfarf",
          "url": "http://localhost:4440/rundeckpro/api/52/storage/keys/arfarf",
          "meta": {
            "Rundeck-content-type": "application/x-rundeck-data-password",
            "Rundeck-content-creation-time": "2025-01-09T00:07:59Z",
            "Rundeck-content-modify-time": "2025-01-09T00:07:59Z",
            "Rundeck-auth-created-username": "admin",
            "Rundeck-auth-modified-username": "admin",
            "Rundeck-data-type": "password",
            "Rundeck-content-mask": "content"
          }
        },
        {
          "path": "keys/diso.key",
          "type": "file",
          "name": "diso.key",
          "url": "http://localhost:4440/rundeckpro/api/52/storage/keys/diso.key",
          "meta": {
            "Rundeck-content-type": "application/octet-stream",
            "Rundeck-content-creation-time": "2025-01-09T00:07:32Z",
            "Rundeck-content-modify-time": "2025-01-09T00:07:32Z",
            "Rundeck-auth-created-username": "admin",
            "Rundeck-auth-modified-username": "admin",
            "Rundeck-key-type": "private",
            "Rundeck-content-mask": "content"
          }
        }
      ]
    } as StorageKeyListResponse
    axiosMock.get.mockResolvedValue({status: 200, data: data})
    let result = await storageKeyGetMetadata('somepath')
    expect(axiosMock.get).toHaveBeenCalledWith('storage/keys/somepath')
    expect(result).toEqual(data)
  })
  it('gets key metadata 404 is empty', async () => {
    axiosMock.get.mockResolvedValue({status: 404, data: {}})
    let result = await storageKeyGetMetadata('somepath')
    expect(axiosMock.get).toHaveBeenCalledWith('storage/keys/somepath')
    expect(result).toEqual({})
  })

  it('deletes key with success', async () => {
    axiosMock.delete.mockResolvedValue({status: 204})
    let result = await storageKeyDelete('somepath')
    expect(axiosMock.delete).toHaveBeenCalledWith('storage/keys/somepath')
    expect(result).toEqual(true)
  })

  it('deletes not found key with failure', async () => {
    axiosMock.delete.mockResolvedValue({status: 404})
    let result = await storageKeyDelete('somepath')
    expect(axiosMock.delete).toHaveBeenCalledWith('storage/keys/somepath')
    expect(result).toEqual(false)
  })
})
