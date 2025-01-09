import KeyType from '../types/KeyType'
import {api} from './api'

/**
 * An interface representing StorageKeyMetadata.
 */
export interface StorageKeyMetadata {
  meta?: any;
  url?: string;
  name?: string;
  type?: string;
  path?: string;
}

/**
 * An interface representing StorageKeyListResponse.
 */
export interface StorageKeyListResponse extends StorageKeyMetadata{
  resources?: StorageKeyMetadata[];
}

export interface KeyUploadContent {
  type: KeyType;
}

export async function storageKeyGetMetadata(
  path: string,
): Promise<StorageKeyListResponse> {
  let resp = await api.get(`storage/keys/${path}`)
  if (resp.status !== 200) {
    throw {message: resp.data.message, response: resp}
  } else {
    return resp.data
  }
}

export async function storageKeyExists(path: string): Promise<Boolean> {
  let resp = await api.get(`storage/keys/${path}`)
  if (resp.status === 404) {
    return false
  }
  if (resp.status === 200) {
    return true
  }
  throw {message: resp.data.message, response: resp}
}

const KeyStorageContentTypes = {
  privateKey: 'application/octet-stream',
  publicKey: 'application/pgp-keys',
  password: 'application/x-rundeck-data-password',
}

export async function storageKeyUpdate(path: string, value: string, content: KeyUploadContent): Promise<StorageKeyMetadata> {
  let resp = await api.put(`storage/keys/${path}`, value, {
    headers: {
      'Content-Type': KeyStorageContentTypes[content.type],
      'Accept': 'application/json',
    }
  })
  if (resp.status === 200) {
    return resp.data
  }
  throw {message: resp.data.message, response: resp}
}

export async function storageKeyCreate(path: string, value: string, content: KeyUploadContent): Promise<StorageKeyMetadata> {
  let resp = await api.post(`storage/keys/${path}`, value, {
    headers: {
      'Content-Type': KeyStorageContentTypes[content.type],
      'Accept': 'application/json',
    }
  })
  if (resp.status == 201) {
    return resp.data
  }
  throw {message: resp.data.message, response: resp}
}

export async function storageKeyDelete(path: string): Promise<Boolean> {
  let resp = await api.delete(`storage/keys/${path}`)
  if (resp.status === 404) {
    return false
  }
  if (resp.status === 204) {
    return true
  }
  throw {message: resp.data.message, response: resp}
}
