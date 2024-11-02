# State Management with BaseStore

## Overview
The `BaseStore` is a foundational class designed to manage state based on the Flux pattern. It is not intended to be used
directly in Vue components or views. Instead, it serves as a base class for creating specific stores that encapsulate
application logic and state management. This document provides guidelines on how to extend `BaseStore` to create
custom stores, initialize them in the `RootStore`, and configure caching and local storage.

## Creating a Custom Store
To create a custom store, extend the `BaseStore` and add specific methods to manage the state. Here is an example of
how to create a `UserStore`:

### Example: UserStore
```typescript
import { BaseStore } from './BaseStore';

interface User {
    id: string;
    name: string;
    age: number;
}

interface UserStoreState {
    users: { [key: string]: User };
}

class UserStore extends BaseStore<UserStoreState> {
    constructor(initialState: UserStoreState, storageKey?: string, useCacheAndStorage: boolean = false) {
    super(initialState, storageKey, useCacheAndStorage);
    }

    getUser(id: string): User | null {
    return this.getItem('users', id);
    }

    setUser(user: User): void {
    this.setItem('users', user);
    }

    removeUser(id: string): void {
    this.removeItem('users', id);
    }

    updateUser(id: string, updatedUser: Partial<User>): void {
    this.updateItem('users', id, updatedUser);
    }

    getAllUsers(): { [key: string]: User } {
    return this.getAllItems('users');
    }

    searchUsers(predicate: (user: User) => boolean): { [key: string]: User } {
    return this.searchItems('users', predicate);
    }

    getPaginatedUsers(page: number, pageSize: number): User[] {
    return this.getPaginatedItems('users', page, pageSize);
    }
}
```

## Why a Factory is being used for cache and localStorage functionalities?
The factory pattern is used to manage the creation and configuration of cache and storage modules. This approach
provides several benefits:
1. **Centralized Configuration**: Ensures consistent configuration and usage of caching and storage mechanisms
   across different stores.
2. **Lazy Initialization**: Cache and storage instances are created only when needed, reducing unnecessary
   initialization.
3. **Flexibility**: Allows for easy updates and changes to the caching and storage strategies without modifying the
   store logic.

## Initializing the Store in RootStore, with cache and localStorage functionalities
The `RootStore` is a global place to initialize and manage all stores. This ensures that stores are only created once and can be easily accessed throughout the application. 
Initializing the store in the `RootStore` is important because it makes the store available throughout the entire project and allows you to pass the necessary flags to enable caching and local storage if required by the new store.

### Example: RootStore
```typescript
import { reactive, UnwrapNestedRefs } from 'vue';
import { RundeckClient } from '@rundeck/client';
import { UserStore } from './UserStore';

export class RootStore {
    userStore: UnwrapNestedRefs<UserStore>;
    // Other stores...

    constructor(readonly client: RundeckClient, appMeta: any = {}) {
    this.userStore = reactive(new UserStore({ users: {} }, 'userStore', true));
    // Initialize other stores...
    }
}
```

### Note:
In the example above, 2 extra parameters are being passed along with the initial state of the UserStore:

- second parameter is the storageKey, in this case, `userStore`: This is the storage key used to identify the store's data being cached. 
- third parameter is a flag useCacheAndStorage: This flag enables both caching and local storage for the UserStore. When set to true, the store will use caching mechanisms and persist its state in local storage, under the storageKey passed as the second parameter. 
