package javassist.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;


/**
 * 
 * @author rdong
 */
public class CollectionUtils {

    public static <K, V> Map<K, V> toMap(Collection<Map<K, V>> values) {
        Map<K, V> ret = new LinkedHashMap<K, V>();
        for (Map<K, V> v : values) {
            ret.putAll(v);
        }
        return ret;
    }

    public static <K, V> void addToListMap(K key, V value, Map<K, List<V>> map) {
        List<V> list = map.get(key);
        if (list == null) {
            list = put(map, key, new ArrayList<V>());
        }
        list.add(value);
    }

    public static <K, V> void addAllToListMap(K key, List<V> value, Map<K, List<V>> map) {
        List<V> list = map.get(key);
        if (list == null) {
            map.put(key, value);
            return;
        }
        list.addAll(value);
    }

    public static <K, V> void addToSetMap(K key, V value, Map<K, Set<V>> map) {
        Set<V> list = map.get(key);
        if (list == null) {
            list = put(map, key, new LinkedHashSet<V>());
        }
        list.add(value);
    }

    public static <K, V> void addAllToSetMap(K key, Set<V> value, Map<K, Set<V>> map) {
        Set<V> list = map.get(key);
        if (list == null) {
            map.put(key, value);
            return;
        }
        list.addAll(value);
    }

    public static <K1, K2, V> void addToMapMap(K1 key1, K2 key2, V value, Map<K1, Map<K2, V>> map) {
        Map<K2, V> list = map.get(key1);
        if (list == null) {
            list = put(map, key1, new LinkedHashMap<K2, V>());
        }
        list.put(key2, value);
    }

    public static <K1, K2, V> void addAllToMapMap(K1 key, Map<K2, V> value, Map<K1, Map<K2, V>> map) {
        Map<K2, V> list = map.get(key);
        if (list == null) {
            map.put(key, value);
            return;
        }
        list.putAll(value);
    }

    /**
     * 
     * @param <K1>
     * @param <K2>
     * @param <V>
     * @param key1
     * @param key2
     * @param value
     * @param mapToModify
     */
    public static <K1, K2, V> void addToKeyedHashMap(K1 key1, K2 key2, V value,
            Map<K1, Map<K2, V>> mapToModify) {
        Map<K2, V> innerMap = mapToModify.get(key1);
        if (innerMap == null) {
            // not in map yet, so add
            innerMap = put(mapToModify, key1, new LinkedHashMap<K2, V>());
        }

        // add to the map
        innerMap.put(key2, value);
    }

    /**
     * 
     * @param key1
     * @param key2
     * @param value
     * @param mapToModify
     * @param cp
     */
    public static <K1, K2, V> void addToKeyedTreeMap(K1 key1, K2 key2, V value,
            Map<K1, TreeMap<K2, V>> mapToModify, Comparator<K2> cp) {
        TreeMap<K2, V> innerMap = mapToModify.get(key1);
        if (innerMap == null) {
            // not in map yet, so add
            innerMap = put(mapToModify, key1, new TreeMap<K2, V>(cp));
        }

        // add to the map
        innerMap.put(key2, value);
    }

    /**
     * 
     * @param <K1>
     * @param <K2>
     * @param <V>
     * @param key1
     * @param key2
     * @param value
     * @param mapToModify
     */
    public static <K1, K2, K3, V> void addToKeyedTreeMap(K1 key1, K2 key2, K3 key3, V value,
            Map<K1, Map<K2, TreeMap<K3, V>>> mapToModify, Comparator<K3> cp) {
        Map<K2, TreeMap<K3, V>> innerMap = mapToModify.get(key1);
        if (innerMap == null) {
            // not in map yet, so add
            innerMap = put(mapToModify, key1, new LinkedHashMap<K2, TreeMap<K3, V>>());
        }

        // add to set
        addToKeyedTreeMap(key2, key3, value, innerMap, cp);
    }

    /**
     * Util method for repetitive logic: Adds the valueToAdd to the list keyed by the data, will
     * create the list if it doesn't yet exist.
     * 
     * @param key to map
     * @param valueToAdd to key's list
     * @param map to modify
     */
    public static <K, V> boolean addToKeyedConcurrentHashSet(K key, V valueToAdd, Map<K, Set<V>> mapToModify) {
        Set<V> set = mapToModify.get(key);

        if (set == null) {
            // not in map yet, so add
            set = put(mapToModify, key, Collections.newSetFromMap(new ConcurrentHashMap<V, Boolean>()));
        }

        // add to list
        return set.add(valueToAdd);
    }

    /**
     * 
     * @param <K1>
     * @param <K2>
     * @param <V>
     * @param key1
     * @param key2
     * @param value
     * @param mapToModify
     */
    public static <K1, K2, V> void addToKeyedConcurrentHashMap(K1 key1, K2 key2, V value,
            Map<K1, Map<K2, V>> mapToModify) {
        Map<K2, V> innerMap = mapToModify.get(key1);
        if (innerMap == null) {
            // not in map yet, so add
            innerMap = put(mapToModify, key1, new ConcurrentHashMap<K2, V>());
        }

        // add to the map
        innerMap.put(key2, value);
    }

    /**
     * Util method for repetitive logic: Adds the valueToAdd to the list keyed by the data, will
     * create the list if it doesn't yet exist.
     * 
     * @param key to map
     * @param valueToAdd to key's list
     * @param map to modify
     */
    public static <K1, K2, V> void addToKeyedMapList(K1 key1, K2 key2, V valueToAdd,
            Map<K1, Map<K2, List<V>>> mapToModify) {
        Map<K2, List<V>> map = mapToModify.get(key1);
        if (map == null) {
            // not in map yet, so add
            map = put(mapToModify, key1, new HashMap());
        }
        addToKeyedList(key2, valueToAdd, map);
    }

    /**
     * Util method for repetitive logic: Adds the valueToAdd to the list keyed by the data, will
     * create the list if it doesn't yet exist.
     * 
     * @param key to map
     * @param valueToAdd to key's list
     * @param map to modify
     */
    public static <K, V> void addToKeyedList(K key, V valueToAdd, Map<K, List<V>> mapToModify) {
        List<V> currentList = mapToModify.get(key);

        if (currentList == null) {
            // not in map yet, so add
            currentList = put(mapToModify, key, new ArrayList<V>());
        }

        // add to list
        currentList.add(valueToAdd);
    }

    /**
     * Util method for repetitive logic: Adds the valueToAdd to the list keyed by the data, will
     * create the list if it doesn't yet exist.
     * 
     * @param key to map
     * @param valueToAdd to key's list
     * @param map to modify
     */
    public static <K, V> void addToKeyedConcurrentLinkedQueue(K key, V valueToAdd,
            Map<K, ConcurrentLinkedQueue<V>> mapToModify) {
        Queue<V> queue = mapToModify.get(key);
        if (queue == null) {
            // not in map yet, so add
            queue = put(mapToModify, key, new ConcurrentLinkedQueue<V>());
        }
        // add to queue
        queue.add(valueToAdd);
    }

    /**
     * @param mutex
     * @param missedMessages
     * @param mutex2MsgQueue
     */
    public static <K, V> boolean addToKeyedLinkedHashSet(K key, V valueToAdd, Map<K, Set<V>> mapToModify) {
        Set<V> currentList = mapToModify.get(key);

        if (currentList == null) {
            // not in map yet, so add
            currentList = put(mapToModify, key, new LinkedHashSet<V>());
        }

        // add to list
        return currentList.add(valueToAdd);
    }

    public static <K, V> void addAllToKeyedLinkedHashSet(K key, Collection<V> valuesToAdd,
            Map<K, Set<V>> mapToModify) {
        Set<V> currentSet = mapToModify.get(key);

        if (currentSet == null) {
            // not in map yet, so add
            currentSet = put(mapToModify, key, new LinkedHashSet<V>());
        }

        // add to list
        currentSet.addAll(valuesToAdd);
    }

    /**
     * Util method for repetitive logic: Adds the valueToAdd to the list keyed by the data, will
     * create the list if it doesn't yet exist.
     * 
     * @param key to map
     * @param valueToAdd to key's list
     * @param map to modify
     */
    public static <K, V> void addToKeyedHashSet(K key, V valueToAdd, Map<K, Set<V>> mapToModify) {
        Set<V> currentList = mapToModify.get(key);

        if (currentList == null) {
            // not in map yet, so add
            currentList = put(mapToModify, key, new HashSet<V>());
        }

        // add to list
        currentList.add(valueToAdd);
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isEmpty(String... objects) {
        return objects == null || objects.length == 0;
    }

    public static boolean isEmpty(Collection<?> objects) {
        return objects == null || objects.isEmpty();
    }

    public static boolean isEmpty(Object[] values) {
        return values == null || values.length == 0;
    }

    public static boolean isEmpty(int[] values) {
        return values == null || values.length == 0;
    }

    public static final <K, V> V put(Map<K, V> map, K key, V value) {
        if (map instanceof ConcurrentMap) {
            V prev = ((ConcurrentMap<K, V>) map).putIfAbsent(key, value);
            return prev != null ? prev : value;
        }
        map.put(key, value);
        return value;
    }

}
