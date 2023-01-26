package org.rundeck.app.data.providers.v1.storage;

import org.rundeck.app.data.model.v1.page.Pageable;
import org.rundeck.app.data.model.v1.storage.RundeckStorage;
import org.rundeck.app.data.providers.v1.DataProvider;
import org.rundeck.spi.data.DataAccessException;
import org.rundeck.storage.api.Path;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * TokenDataProvider defines a base set of AuthenticationToken datastore methods.
 *
 */
public interface StorageDataProvider extends DataProvider {
    /**
     * Retrieves a Storage based on the id provided.
     *
     * @param id of the Storage, format Serializable
     * @return Storage if found, otherwise null
     */
    RundeckStorage getData (final Serializable id);

    /**
     * Creates a Storage
     *
     * @param data RundeckStorage attributes
     *
     * @return id of the created RundeckStorage
     * @throws DataAccessException on error
     */
    Long create(final RundeckStorage data, Map<String, String> metadata) throws DataAccessException;

    /**
     * Updates a RundeckStorage with the supplied attributes
     *
     * @param metadata RundeckStorage metadata
     * @param data RundeckStorage attributes
     * @param id id
     * @throws DataAccessException on error
     */
    void update(final Serializable id, final RundeckStorage data, Map<String, String> metadata) throws DataAccessException;

    /**
     * Removes a RundeckStorage
     *
     * @param id of the RundeckStorage
     * @throws DataAccessException on error
     */
    void delete(final Serializable id) throws DataAccessException;

    /**
     * Finds a RundeckStorage based on namespace, directory and name
     *
     * @param namespace of the RundeckStorage
     * @param dir of the RundeckStorage
     * @param name of the RundeckStorage
     * @return RundeckStorage instance if found, otherwise null
     */
    RundeckStorage findResource(final String namespace, String dir, String name);

    /**
     * Finds all RundeckStorages based on namespace and path
     *
     * @param namespace of the RundeckStorage
     * @param path of the RundeckStorage
     * @return List of RundeckStorages
     */
    List<RundeckStorage> findAllByNamespaceAndDir(final String namespace, final String path);

    /**
     * Checks if a Rundeck Storage exists with a given path, namespace, name and directory
     *
     * @param ns, namespace
     * @param path
     * @param name
     * @param dir
     * @return true if a RundeckStorage exists, otherwise false
     */
    boolean hasPath(final String ns, final Path path);

    /**
     * Checks if a Rundeck Storage exists in a given directory path
     *
     * @param ns, namespace
     * @param path
     * @return true if a RundeckStorage exists, otherwise false
     */
    boolean hasDirectory(final String ns, final Path path);

    /**
     * Finds all RundeckStorages in a directory
     *
     * @param ns, namespace
     * @param path
     * @return List of RundeckStorages
     */
    List<RundeckStorage> listDirectory(String ns, Path path);

    /**
     * Finds all RundeckStorages in a directory subdirectories
     *
     * @param ns, namespace
     * @param path
     * @return List of RundeckStorages
     */
    List<RundeckStorage> listDirectorySubdirs(final String ns, final Path path);


    }
