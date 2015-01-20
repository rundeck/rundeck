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
 * Mapper.java
 *
 * Created on September 26, 2003, 4:30 PM
 * $Id: Mapper.java 9378 2009-08-06 18:59:53Z gschueler $
 */

package com.dtolabs.utils;


import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.Predicate;

import java.util.*;


/**
 * Some basic functions from other languages just don't exist in java :) <br> This utility class provides simple methods
 * for "mapping" a set of objects to a new set of objects, based on a provided implementation.<br> The mapping will be
 * one to one, although if the result of a particular mapping returns null, the result can be discarded (default) rather
 * than included. <br> Each static utility method which by default ignores null results has an equivalent method which
 * has a boolean parameter indicating whether nulls should be discarded or not. <br> The other use for this utility
 * class is to perform the same action on all items of a collection, where no result is necessarily returned or
 * important.
 * <br>
 * Example usage:<br>
 * <pre>
 * //create a collection of Strings from some objects
 * Collection c = Mapper.map(new Mapper(){
 *      public Object map(Object o){
 *          return o.toString();
 *      }, someIterator);
 * <br>
 * <br>
 * //simply perform some action and discard the results
 * Mapper.map(new Mapper(){
 *      public Object map(Object o){
 *          ((MyTaskThread)o).performTask();
 *          return null;
 *      }, someIterator);
 * </pre>
 *
 * @author greg
 */
public abstract class Mapper {
    /**
     * Create a Comparator which compares two objects by first mapping
     * each object with a mapper and then comparing the result objects.
     * Note: The Mapper instance <b>must</b> map every object into one
     * that implements the {@link Comparable} interface, otherwise the two
     * objects cannot be compared. (All of the java.lang.* wrapper types will work.)
     * @param mapper mapper
     * @return comparator
     */
    public static Comparator comparator(final Mapper mapper){
        return comparator(mapper, false);
    }
    /**
     * see {@link #comparator(Mapper)}
     * @param mapper mapper
     * @param reverse if true, reverse the order of comparison
     * @return comparator
     */
    public static Comparator comparator(final Mapper mapper, final boolean reverse){
        return new Comparator(){
            public int compare(Object o, Object o1) {
                return ((Comparable) mapper.map(reverse?o1:o)).compareTo(mapper.map(reverse?o:o1));
            }
        };
    }

    /**
     * Concatenate another mapper onto this one.
     * @param mapper mapper
     * @return mapper
     */
    public Mapper concat(Mapper mapper){
        return Mapper.concat(this, mapper);
    }
    private static class ConcatMapper extends Mapper{
        private Mapper[] arr;
        ConcatMapper(Mapper[] maps){
            this.arr = maps;
        }

        /**
         * Map one object to another.
         *
         * @return the new object.
         */
        public Object map(Object a) {
            Object b = a;
            if (b == null) {
                return null;
            }
            if(null!=arr){
                for (int i=0;i<arr.length;i++) {
                    if(null!=arr[i]){
                        b = arr[i].map(b);

                        if (b == null) {
                            return null;
                        }
                    }
                }
            }
            return b;

        }

        public Mapper concat(Mapper mapper) {
            if(arr!=null){
                Mapper[] l = new Mapper[arr.length + 1];
                System.arraycopy(arr, 0, l, 0, arr.length);
                l[arr.length] = mapper;
                return new ConcatMapper(l);
            }else{
                return new ConcatMapper(new Mapper[]{mapper});
            }

        }
    }
    /**
     * Concatenate more than two Mappers.
     *
     * @param arr array of Mapper instances.  a null array will return the identity Mapper.
     *
     * @return single Mapper concatenating the Mappers.
     */
    public static Mapper concat(final Mapper[] arr){
        return new ConcatMapper(arr);
    }
    /**
     * Concatenate two Mappers.
     * @param first first mapper to apply
     * @param second second mapper to apply
     * @return mapper
     */
    public static Mapper concat(final Mapper first, final Mapper second){
        return new ConcatMapper(new Mapper[]{first,second});
    }
    /**
     * Creates a map where the object at index N from the first List is the key for the object at index N of the second
     * List. <br> By default discards both key and value if either one is null.
     *
     * @param keys   List of keys
     * @param values List of values
     * @return map
     */
    public static Map zip(List keys, List values) {
        return zip(keys, values, false);
    }

    /**
     * Creates a map where the object at index N from the first List is the key for the object at index N of the second
     * List. If either List is shorter than the other, the resulting Map will have only as many keys as are in the
     * smallest of the two Lists. <br> If includeNull is true, both keys and values of null are allowed. Note that if
     * more than one key is null, previous entries will be overwritten.
     *
     * @param keys        List of keys
     * @param values      List of values
     * @param includeNull allow null values and keys
     * @return map
     */
    public static Map zip(List keys, List values, boolean includeNull) {
        Iterator k = keys.iterator();
        Iterator v = values.iterator();
        HashMap hm = new HashMap();
        while (k.hasNext() && v.hasNext()) {
            Object o = k.next();
            Object p = v.next();
            if (includeNull || o != null && p != null) {
                hm.put(o, p);
            }
        }
        return hm;
    }


    /**
     * Creates a map where the object at index N from the first Iterator is the key for the object at index N of the
     * second Iterator. <br> By default discards both key and value if either one is null.
     *
     * @param keys   array of keys
     * @param values array of values
     * @return map
     */
    public static Map zip(Object[] keys, Object[] values) {
        return zip(java.util.Arrays.asList(keys), java.util.Arrays.asList(values), false);
    }

    /**
     * Creates a map where the object at index N from the first Iterator is the key for the object at index N of the
     * second Iterator. If either Iterator is shorter than the other, the resulting Map will have only as many keys as
     * are in the smallest of the two Iterator. <br> If includeNull is true, both keys and values of null are allowed.
     * Note that if more than one key is null, previous entries will be overwritten.
     *
     * @param keys        array of keys
     * @param values      array of values
     * @param includeNull allow null values and keys
     * @return map
     */
    public static Map zip(Object[] keys, Object[] values, boolean includeNull) {
        return zip(java.util.Arrays.asList(keys), java.util.Arrays.asList(values), includeNull);
    }

    /**
     * Creates a map where the object at index N from the first Iterator is the key for the object at index N of the
     * second Iterator. <br> By default discards both key and value if either one is null.
     *
     * @param keys   Iterator of keys
     * @param values Iterator of values
     * @return map
     */
    public static Map zip(Iterator keys, Iterator values) {
        return zip(keys, values, false);
    }

    /**
     * Creates a map where the object at index N from the first Iterator is the key for the object at index N of the
     * second Iterator. If either Iterator is shorter than the other, the resulting Map will have only as many keys as
     * are in the smallest of the two Iterator. <br> If includeNull is true, both keys and values of null are allowed.
     * Note that if more than one key is null, previous entries will be overwritten.
     *
     * @param keys        Iterator of keys
     * @param values      Iterator of values
     * @param includeNull allow null values and keys
     * @return map
     */
    public static Map zip(Iterator keys, Iterator values, boolean includeNull) {
        HashMap hm = new HashMap();
        while (keys.hasNext() && values.hasNext()) {
            Object o = keys.next();
            Object p = values.next();
            if (includeNull || o != null && p != null) {
                hm.put(o, p);
            }
        }
        return hm;
    }

    /**
     * Trivial case of a single object.
     *
     * @param mapper an Mapper
     * @param o an     Object
     *
     * @return a Collection of the results.
     */
    public static Collection map(Mapper mapper, Object o) {
        return java.util.Collections.singleton(mapper.map(o));
    }

    /**
     * Return the results of mapping all objects with the mapper.
     *
     * @param mapper an Mapper
     * @param c a      Collection
     *
     * @return a Collection of the results.
     */
    public static Collection map(Mapper mapper, Collection c) {
        return map(mapper, c, false);
    }

    public Collection apply(Collection c) {
        return Mapper.map(this, c);
    }

    /**
     * Return the results of mapping all objects with the mapper.
     *
     * @param mapper an Mapper
     * @param c a      Collection
     * @param allowNull allow null values
     *
     * @return a Collection of the results.
     */
    public static Collection map(Mapper mapper, Collection c, boolean allowNull) {
        return map(mapper, c.iterator(), allowNull);
    }


    /**
     * Return the results of mapping all objects with the mapper.
     *
     * @param mapper an Mapper
     * @param arr an     array of objects
     *
     * @return a Collection of the results.
     */
    public static Collection map(Mapper mapper, Object[] arr) {
        return map(mapper, arr, false);
    }

    public Collection apply(Object[] arr) {
        return Mapper.map(this, arr);
    }

    /**
     * Return the results of mapping all objects with the mapper.
     *
     * @param mapper an Mapper
     * @param arr an     array of objects
     * @param allowNull allow null values
     *
     * @return a Collection of the results.
     */
    public static Collection map(Mapper mapper, Object[] arr, boolean allowNull) {
        return map(mapper, java.util.Arrays.asList(arr), allowNull);
    }

    /**
     * Return the results of mapping all objects with the mapper.
     *
     * @param mapper an Mapper
     * @param en an     Enumeration
     *
     * @return a Collection of the results.
     */
    public static Collection map(Mapper mapper, Enumeration en) {
        return map(mapper, en, false);
    }

    public Collection apply(Enumeration en) {
        return Mapper.map(this, en);
    }

    /**
     * Return the results of mapping all objects with the mapper.
     *
     * @param mapper an Mapper
     * @param en an     Enumeration
     * @param allowNull allow null values
     *
     * @return a Collection of the results.
     */
    public static Collection map(Mapper mapper, Enumeration en, boolean allowNull) {
        ArrayList l = new ArrayList();
        while (en.hasMoreElements()) {
            Object o = mapper.map(en.nextElement());
            if (allowNull || o != null) {
                l.add(o);
            }
        }
        return l;
    }


    /**
     * Create a new Map by mapping all keys from the original map and maintaining the original value.
     *
     * @param mapper a Mapper to map the keys
     * @param map a      Map
     *
     * @return a new Map with keys mapped
     */
    public static Map mapKeys(Mapper mapper, Map map) {
        return mapKeys(mapper, map, false);
    }

    public Map applyToKeys(Map map) {
        return Mapper.mapKeys(this, map);
    }
    /**
     * Create a new Map by mapping all keys from the original map and maintaining the original value.
     *
     * @param mapper a Mapper to map the keys
     * @param map a      Map
     * @param allowNull allow null values
     *
     * @return a new Map with keys mapped
     */
    public static Map mapKeys(Mapper mapper, Map map, boolean allowNull) {
        HashMap h = new HashMap();
        for (Object e : map.entrySet()) {
            Map.Entry entry = (Map.Entry) e;
            Object o = mapper.map(entry.getKey());
            if (allowNull || o != null) {
                h.put(o, entry.getValue());
            }
        }
        return h;
    }

    /**
     * Create a new Map by mapping all values from the original map and maintaining the original key.
     *
     * @param mapper a Mapper to map the values
     * @param map an     Map
     *
     * @return a new Map with values mapped
     */
    public static Map mapValues(Mapper mapper, Map map) {
        return mapValues(mapper, map, false);
    }

    public Map applyToValues(Map map) {
        return Mapper.mapValues(this, map);
    }

    /**
     * Create a new Map by mapping all values from the original map and maintaining the original key.
     *
     * @param mapper a Mapper to map the values
     * @param map an     Map
     * @param includeNull true to include null
     *
     * @return a new Map with values mapped
     */
    public static Map mapValues(Mapper mapper, Map map, boolean includeNull) {
        HashMap h = new HashMap();
        for (Object e : map.keySet()) {
            Map.Entry entry = (Map.Entry) e;
            Object v = entry.getValue();
            Object o = mapper.map(v);
            if (includeNull || o != null) {
                h.put(entry.getKey(), o);
            }
        }
        return h;
    }


    /**
     * Create a new Map by mapping all values from the original map and mapping all keys.
     *
     * @param mapper a Mapper to map the values and keys
     * @param map a Map
     *
     * @return a new Map with values mapped
     */
    public static Map mapEntries(Mapper mapper, Map map) {
        return mapEntries(mapper, map, false);
    }

    public Map applyToEntries(Map map) {
        return Mapper.mapEntries(this, map);
    }
    /**
     * Create a new Map by mapping all values from the original map, and mapping all keys.
     * @param mapper a Mapper to map both values and keys
     * @param map Map input
     * @param includeNull if true, allow null as either key or value after mapping
     * @return a new Map with both keys and values mapped using the Mapper
     */
    public static Map mapEntries(Mapper mapper, Map map, boolean includeNull){
        HashMap h = new HashMap();
        for (Object e : map.entrySet()) {
            Map.Entry entry = (Map.Entry) e;
            Object k = entry.getKey();
            Object v = entry.getValue();
            Object nk = mapper.map(k);
            Object o = mapper.map(v);
            if (includeNull || (o != null && nk != null)) {
                h.put(nk, o);
            }
        }
        return h;
    }


    /**
     * Create a new Map by using the array objects as keys, and the mapping result as values. Discard keys which return
     * null values from the mapper.
     *
     * @param mapper a Mapper to map the values
     * @param a      array of items
     *
     * @return a new Map with values mapped
     */
    public static Map makeMap(Mapper mapper, Object[] a) {
        return makeMap(mapper, java.util.Arrays.asList(a), false);
    }

    public Map makeMap(Object[] a) {
        return Mapper.makeMap(this, a);
    }


    /**
     * Create a new Map by using the array objects as keys, and the mapping result as values.
     *
     * @param mapper a Mapper to map the values
     * @param a      array of items
     * @param includeNull true to include null
     *
     * @return a new Map with values mapped
     */
    public static Map makeMap(Mapper mapper, Object[] a, boolean includeNull) {
        return makeMap(mapper, java.util.Arrays.asList(a), includeNull);
    }

    /**
     * Create a new Map by using the collection objects as keys, and the mapping result as values. Discard keys which
     * return null values from the mapper.
     *
     * @param mapper a Mapper to map the values
     * @param c      Collection of items
     *
     * @return a new Map with values mapped
     */
    public static Map makeMap(Mapper mapper, Collection c) {
        return makeMap(mapper, c.iterator(), false);
    }

    public Map makeMap(Collection c) {
        return Mapper.makeMap(this, c);
    }
    /**
     * Create a new Map by using the collection objects as keys, and the mapping result as values.
     *
     * @param mapper a Mapper to map the values
     * @param c      Collection of items
     * @param includeNull true to include null
     *
     * @return a new Map with values mapped
     */
    public static Map makeMap(Mapper mapper, Collection c, boolean includeNull) {
        return makeMap(mapper, c.iterator(), includeNull);
    }

    /**
     * Create a new Map by using the iterator objects as keys, and the mapping result as values. Discard keys which
     * return null values from the mapper.
     *
     * @param mapper a Mapper to map the values
     * @param i      Iterator
     *
     * @return a new Map with values mapped
     */
    public static Map makeMap(Mapper mapper, Iterator i) {
        return makeMap(mapper, i, false);
    }

    public Map makeMap(Iterator i) {
        return Mapper.makeMap(this, i);
    }
    /**
     * Create a new Map by using the iterator objects as keys, and the mapping result as values.
     *
     * @param mapper a Mapper to map the values
     * @param i      Iterator
     * @param includeNull true to include null
     *
     * @return a new Map with values mapped
     */
    public static Map makeMap(Mapper mapper, Iterator i, boolean includeNull) {
        HashMap h = new HashMap();
        for (; i.hasNext();) {
            Object k = i.next();
            Object v = mapper.map(k);
            if (includeNull || v != null) {
                h.put(k, v);
            }
        }
        return h;
    }

    /**
     * Create a new Map by using the enumeration objects as keys, and the mapping result as values. Discard keys which
     * return null values from the mapper.
     *
     * @param mapper a Mapper to map the values
     * @param en     Enumeration
     *
     * @return a new Map with values mapped
     */
    public static Map makeMap(Mapper mapper, Enumeration en) {
        return makeMap(mapper, en, false);
    }

    public Map makeMap(Enumeration en) {
        return Mapper.makeMap(this, en);
    }
    /**
     * Create a new Map by using the enumeration objects as keys, and the mapping result as values.
     *
     * @param mapper a Mapper to map the values
     * @param en     Enumeration
     * @param includeNull true to include null
     *
     * @return a new Map with values mapped
     */
    public static Map makeMap(Mapper mapper, Enumeration en, boolean includeNull) {
        HashMap h = new HashMap();
        for (; en.hasMoreElements();) {
            Object k = en.nextElement();
            Object v = mapper.map(k);
            if (includeNull || v != null) {
                h.put(k, v);
            }
        }
        return h;
    }

    /**
     * Return the results of mapping all objects with the mapper.
     *
     * @param mapper an Mapper
     * @param i an     Iterator
     *
     * @return a Collection of the results.
     */
    public static Collection map(Mapper mapper, Iterator i) {
        return map(mapper, i, false);
    }

    public Collection apply(Iterator i) {
        return Mapper.map(this, i);
    }
    /**
     * Return the results of mapping all objects with the mapper.
     *
     * @param mapper an Mapper
     * @param i an     Iterator
     * @param includeNull specify whether nulls are included
     *
     * @return a Collection of the results.
     */
    public static Collection map(Mapper mapper, Iterator i, boolean includeNull) {
        ArrayList l = new ArrayList();
        while (i.hasNext()) {
            Object o = mapper.map(i.next());
            if (includeNull || o != null) {
                l.add(o);
            }
        }
        return l;
    }


    /**
     * Map dynamically using a bean property name.
     *
     * @param property the name of a bean property
     * @param objs     an array of objects
     *
     * @return collection
     * @throws ClassCastException if there is an error invoking the method on any object.
     */
    public static Collection beanMap(String property, Object[] objs) {
        return beanMap(property, java.util.Arrays.asList(objs), false);
    }

    /**
     * Map dynamically using a bean property name.
     *
     * @param property the name of a bean property
     * @param i        an iterator of objects
     *
     * @return collection
     *
     * @throws ClassCastException if there is an error invoking the method on any object.
     */
    public static Collection beanMap(String property, Iterator i) {
        return beanMap(property, i, false);
    }

    /**
     * Map dynamically using a bean property name.
     *
     * @param property    the name of a bean property
     * @param c           an Collection of objects
     * @param includeNull true to include null results in the response
     *
     * @return collection
     *
     * @throws ClassCastException if there is an error invoking the method on any object.
     */
    public static Collection beanMap(String property, Collection c, boolean includeNull) {
        return beanMap(property, c.iterator(), includeNull);
    }

    /**
     * Map dynamically using a bean property name.
     * 
     * @param property    the name of a bean property
     * @param c a collection of objects
     *
     * @return collection
     *
     * @throws ClassCastException if there is an error invoking the method on any object.
     */
    public static Collection beanMap(String property, Collection c) {
        return beanMap(property, c.iterator(), false);
    }

    /**
     * Create a mapper for bean properties
     * @param property name of the bean property
     * @return mapper
     */
    public static Mapper beanMapper(final String property){
        return  new Mapper() {
        public Object map(Object a) {
            try {
                return BeanUtils.getProperty(a, property);
            } catch (Exception e) {
                throw new ClassCastException("Object was not the expected class: " + a.getClass());
            }
        }
        };
    }
    /**
     * Map dynamically using a bean property name.
     *
     * @param property    the name of a bean property
     * @param i           an iterator of objects
     * @param includeNull true to include null results in the response
     *
     * @return collection
     *
     * @throws ClassCastException if there is an error invoking the method on any object.
     */
    public static Collection beanMap(final String property, Iterator i, boolean includeNull) {
        return map(beanMapper(property), i, includeNull);
    }

    /**
     * Return a mapper than maps an object to itself if the predicate evaluates to true,
     * and to null otherwise.
     * @param pred predicate
     * @return mapper
     */
    public static Mapper filterMapper(final Predicate pred){
        return new Mapper(){
            public Object map(Object a) {
                return pred.evaluate(a) ? a : null;
            }
        };
    }

    /**
     * @return a mapper that maps unique objects to themselves, and duplicates to null.
     */
    public static Mapper uniqueMapper(){
        return new Mapper() {
            HashSet set = new HashSet();
            public Object map(Object a) {
                if (set.contains(a)) {
                    return null;
                }
                set.add(a);
                return a;
            }
        };
    }

    /**
     * Return a mapper that uses a {@link java.util.Map} instance, and maps keys of that Map to the values.
     * @param map a {@link java.util.Map} instance
     * @return a Mapper instance
     */
    public static Mapper mapMapper(final Map map) {
        return new Mapper() {
            public Object map(final Object a) {
                return map.get(a);
            }
        };
    }

    /**
     * Maps an object to the results of the toString() method.
     */
    public final static Mapper toString = new Mapper(){

        public Object map(Object a) {
            return a == null ? null : a.toString();
        }
    };
    /**
     * Maps an object to itself.
     */
    public final static Mapper identity = new Mapper() {

        public Object map(Object a) {
            return a;
        }
    };
    
    /**
     * Map one object to another.
     *
     * @param a object
     * @return the new object.
     */
    public abstract Object map(Object a);
}
