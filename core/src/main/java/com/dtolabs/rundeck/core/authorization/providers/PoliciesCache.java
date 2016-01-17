/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* PoliciesCache.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Nov 16, 2010 11:26:12 AM
* 
*/
package com.dtolabs.rundeck.core.authorization.providers;

import com.dtolabs.rundeck.core.authorization.Attribute;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.parser.ParserException;

import java.io.File;
import java.util.*;

/**
 * PoliciesCache retains PolicyDocument objects for inserted Files, and reloads them if file modification time changes.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class PoliciesCache implements Iterable<PolicyCollection> {
    static final long DIR_LIST_CHECK_DELAY = Long.getLong(PoliciesCache.class.getName()+".DirListCheckDelay", 60000);
    static final long FILE_CHECK_DELAY = Long.getLong(PoliciesCache.class.getName() + ".FileCheckDelay", 60000);
    private final static Logger logger = Logger.getLogger(PoliciesCache.class);

    private Set<File> warned = new HashSet<File>();
    private Map<String, CacheItem> cache = new HashMap<>();
    private SourceProvider provider;
    /**
     * Context to load the polices within, invalid policies will be flagged
     */
    final private Set<Attribute> forcedContext;

    private PoliciesCache(final SourceProvider provider) {
        this.provider = provider;
        this.forcedContext =null;
    }
    private PoliciesCache(final SourceProvider provider, final Set<Attribute> forcedContext) {
        this.provider = provider;
        this.forcedContext = forcedContext;
    }
    private static class CacheItem{
        PolicyCollection policyCollection;
        Long cacheTime;
        Long modTime;

        private CacheItem(PolicyCollection policyCollection, Long modTime) {
            this.policyCollection = policyCollection;
            this.modTime = modTime;
            this.cacheTime=System.currentTimeMillis();
        }

        public void touch(Long time) {
            this.cacheTime = time;
        }
    }

    private PolicyCollection createEntry(final YamlSource source) throws PoliciesParseException {
        try {
            return YamlProvider.policiesFromSource(source, forcedContext);
        } catch (ParserException e1) {
            throw new PoliciesParseException("YAML syntax error: " + e1.toString(), e1);
        }catch (Exception e1) {
            throw new PoliciesParseException(e1);
        }
    }

    /**
     * @param source source
     * @return collection
     * @throws PoliciesParseException
     */
    public synchronized PolicyCollection getDocument(final CacheableYamlSource source) throws PoliciesParseException {
//        cacheTotal++;
        CacheItem entry = cache.get(source.getIdentity());

        long checkTime = System.currentTimeMillis();
        if (null == entry || ((checkTime - entry.cacheTime) > FILE_CHECK_DELAY)) {
            final long lastmod = source.getLastModified().getTime();
            if (null == entry || lastmod > entry.modTime) {
                    if (!source.isValid()) {
                        CacheItem remove = cache.remove(source.getIdentity());
                        entry = null;
//                        cacheRemove++;
                    } else {
//                        cacheMiss++;
                        PolicyCollection entry1 = createEntry(source);
                        if (null != entry1) {
                            entry = new CacheItem(entry1, lastmod);
                            cache.put(source.getIdentity(), entry);
                        } else {
                            cache.remove(source.getIdentity());
                            entry = null;
                        }
                    }
            }else{
//                cacheUnmodifiedHit++;
                entry.touch(checkTime);
            }
        }else{
//            cacheHit++;
        }
        return null != entry ? entry.policyCollection : null;
    }

    public Iterator<PolicyCollection> iterator() {
        return new cacheIterator(provider.getSourceIterator());
    }

    /**
     * Create a cache from a single file source
     * @param singleFile file
     * @return cache
     */
    public static PoliciesCache fromFile(File singleFile) {
        return fromSourceProvider(YamlProvider.getFileProvider(singleFile));
    }

    /**
     * Create a cache from a single file source
     *
     * @param singleFile file
     *
     * @return cache
     */
    public static PoliciesCache fromFile(File singleFile, Set<Attribute> forcedContext) {
        return fromSourceProvider(YamlProvider.getFileProvider(singleFile), forcedContext);
    }


    /**
     * Create from a provider
     * @param provider source provider
     * @return policies cache
     */
    public static PoliciesCache fromSourceProvider(final SourceProvider provider) {
        return new PoliciesCache(provider);
    }

    /**
     * Create from a provider with a forced context
     * @param provider source provider
     * @param forcedContext forced context
     * @return policies cache
     */
    public static PoliciesCache fromSourceProvider(
            final SourceProvider provider,
            final Set<Attribute> forcedContext
    )
    {
        return new PoliciesCache(provider, forcedContext);
    }

    /**
     * Create a cache from a directory source
     * @param rootDir base director
     * @return cache
     */
    public static PoliciesCache fromDir(File rootDir) {
        return fromSourceProvider(YamlProvider.getDirProvider(rootDir));
    }

    /**
     * Create a cache from a directory source
     * @param rootDir base director
     * @return cache
     */
    public static PoliciesCache fromDir(File rootDir, final Set<Attribute> forcedContext) {
        return fromSourceProvider(YamlProvider.getDirProvider(rootDir),forcedContext);
    }
    /**
     * Create a cache from cacheable sources
     * @param sources source
     * @return cache
     */
    public static PoliciesCache fromSources(final Iterable<CacheableYamlSource> sources) {
        return fromSourceProvider(
                new SourceProvider() {
                    @Override
                    public Iterator<CacheableYamlSource> getSourceIterator() {
                        return sources.iterator();
                    }
                }
        );
    }
    /**
     * Create a cache from cacheable sources
     * @param sources source
     * @return cache
     */
    public static PoliciesCache fromSources(final Iterable<CacheableYamlSource> sources, final Set<Attribute> context) {
        return fromSourceProvider(
                new SourceProvider() {
                    @Override
                    public Iterator<CacheableYamlSource> getSourceIterator() {
                        return sources.iterator();
                    }
                },
                context
        );
    }


    private Map<CacheableYamlSource, Long> cooldownset = Collections.synchronizedMap(new HashMap<CacheableYamlSource, Long>());
    /**
     * Iterator over the PolicyCollections for the cache's sources.  It skips
     * sources that are no longer valid
     */
    private class cacheIterator implements Iterator<PolicyCollection> {
        Iterator<CacheableYamlSource> intIter;
        private CacheableYamlSource nextSource;
        private PolicyCollection nextPolicyCollection;

        public cacheIterator(final Iterator<CacheableYamlSource> intIter) {
            this.intIter = intIter;
            nextSource = this.intIter.hasNext() ? this.intIter.next() : null;
            loadNextSource();
        }

        private void loadNextSource() {
            while (hasNextFile() && null == nextPolicyCollection) {
                CacheableYamlSource newNextSource = getNextSource();
                Long aLong = cooldownset.get(newNextSource);
                if (null != aLong && newNextSource.getLastModified().getTime() == aLong) {
                    logger.debug("Skip parsing of: " + newNextSource + ". Reason: parse error cooldown until modified");
                    continue;
                } else if (null != aLong) {
                    //clear
                    cooldownset.remove(newNextSource);
                }
                try {
                    nextPolicyCollection = getDocument(newNextSource);
                } catch (PoliciesParseException e) {
                    logger.error("ERROR unable to parse aclpolicy: " + newNextSource + ". Reason: " + e.getMessage());
                    logger.debug("ERROR unable to parse aclpolicy: " + newNextSource + ". Reason: " + e.getMessage(), e);
                    cache.remove(newNextSource.getIdentity());
                    cooldownset.put(newNextSource, newNextSource.getLastModified().getTime());
                }
            }
        }

        private CacheableYamlSource getNextSource() {
            CacheableYamlSource next = nextSource;
            nextSource = intIter.hasNext() ? intIter.next() : null;
            return next;
        }

        private PolicyCollection getNextPolicyCollection() {
            PolicyCollection doc = nextPolicyCollection;
            nextPolicyCollection =null;
            loadNextSource();
            return doc;
        }

        public boolean hasNextFile() {
            return null != nextSource;
        }

        public boolean hasNext() {
            return null != nextPolicyCollection;
        }

        public PolicyCollection next() {
            return getNextPolicyCollection();
        }

        public void remove() {
        }
    }

}
