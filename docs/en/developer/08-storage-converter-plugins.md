% Storage Converter Plugin
% Greg Schueler
% March 29, 2014

## About 

Storage converters can modify file contents and metadata uploaded to the [Key Storage](../administration/key-storage.html) via the [Key Storage API](../api/index.html#key-storage).

When installed, Storage Converter plugins can be configured to apply to all storage requests for a certain Path, or matching a certain metadata selector.  This lets you apply plugins to only a subset of storage requests.

A typical example is to apply some form of encryption to the [Key Storage](../administration/key-storage.html) stored under the `/keys` path.  In this case you can also have the plugin apply only to Private keys, by using the metadata selector.

## Configuring

See: [Configuring the Storage Converter Plugin](../administration/key-storage.html#configuring-the-storage-converter-plugin).

## Java Plugin Type

* *Note*: Refer to [Java Development](plugin-development.html#java-plugin-development) for information about developing a Java plugin for Rundeck.

The plugin interface is [StorageConverterPlugin](../javadoc/com/dtolabs/rundeck/plugins/storage/StorageConverterPlugin.html).

The service name is [`StorageConverter`](../javadoc/com/dtolabs/rundeck/plugins/ServiceNameConstants.html#StorageConverter).

~~~~~ {.java}
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.storage.ResourceMetaBuilder;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.storage.StorageConverterPlugin;
import org.rundeck.storage.api.HasInputStream;
import org.rundeck.storage.api.Path;

@Plugin(name="myprovider", service=ServiceNameConstants.StorageConverter)
public class MyProvider implements StorageConverterPlugin {
    /** read the stored data, decrypt if necessary */
    HasInputStream readResource(Path path, ResourceMetaBuilder
      resourceMetaBuilder, HasInputStream hasInputStream){
      return null;
  }

    /** encrypt data to be stored if necessary */
    HasInputStream createResource(Path path, ResourceMetaBuilder 
      resourceMetaBuilder, HasInputStream hasInputStream){
      return null;
  }

    /** encrypt data to be stored if necessary */
    HasInputStream updateResource(Path path, ResourceMetaBuilder 
      resourceMetaBuilder, HasInputStream hasInputStream){
      return null;
  }
}
~~~~~ 

The three methods are called when a resource is read, created, or updated, respectively.

Your plugin can do any of the following:

1. Do nothing and return null, this will result that no changes to the stored data or metadata will be made.
2. Modify the content being written or read, by reading the incoming data via the `HasInputStream` object, and returning a new `HasInputStream` object which will provide the modified data.
    - If you are doing the typical "encrypt/decrypt", you will want to *encrypt* during create, update, and *decrypt* during the read operation
3. Modify the metadata being written or read, by calling methods on the `ResourceMetaBuilder` object.  This object will provide the metadata values that would be written/read.  You can change the values by setting new values in the object.

## Example

Example code under the `examples/` directory:

* `example-java-storage-converter-plugin`