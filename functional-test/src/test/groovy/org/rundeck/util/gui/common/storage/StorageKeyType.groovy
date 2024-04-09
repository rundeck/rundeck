package org.rundeck.util.gui.common.storage

enum StorageKeyType {

    PASSWORD ("password", "uploadpasswordfield"),
    PRIVATE_KEY("privateKey", "storageuploadtext"),
    PUBLIC_KEY("publicKey", "storageuploadtext")

    final String type
    final String fieldId

    StorageKeyType(String type, String fieldId) {
        this.type = type
        this.fieldId = fieldId
    }

    String getType() {
        return this.type
    }

    String getFieldId() {
        return this.fieldId
    }

}
