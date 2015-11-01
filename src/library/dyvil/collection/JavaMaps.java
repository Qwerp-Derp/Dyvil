package dyvil.collection;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import dyvil.annotation.infix;
import dyvil.annotation.inline;
import dyvil.collection.immutable.ArrayMap;
import dyvil.collection.immutable.EmptyMap;
import dyvil.collection.immutable.SingletonMap;
import dyvil.tuple.Tuple2;
import dyvil.util.None;
import dyvil.util.Option;
import dyvil.util.Some;

public interface JavaMaps
{
	// Accessors
	
	/**
	 * @see Map#$qmark$at(Object)
	 */
	public static @infix @inline boolean $qmark$at(java.util.Map<?, ?> map, Object key)
	{
		return map.containsKey(key);
	}
	
	/**
	 * @see Map#$qmark(Object, Object)
	 */
	public static @infix boolean $qmark(java.util.Map<?, ?> map, Object key, Object value)
	{
		return value == null ? map.get(key) == null : value.equals(map.get(key));
	}
	
	/**
	 * @see Map#$qmark(Entry)
	 */
	public static @infix boolean $qmark(java.util.Map<?, ?> map, Entry<?, ?> entry)
	{
		return $qmark(map, entry.getKey(), entry.getValue());
	}
	
	/**
	 * @see Map#$qmark(Object)
	 */
	public static @infix @inline boolean $qmark$colon(java.util.Map<?, ?> map, Object value)
	{
		return map.containsValue(value);
	}
	
	/**
	 * @see Map#subscript(Object)
	 */
	public static @infix @inline <K, V> V subscript(java.util.Map<K, V> map, Object key)
	{
		return map.get(key);
	}
	
	/**
	 * @see Map#subscript(Object)
	 */
	public static @infix @inline <K, V> Option<V> getOption(java.util.Map<K, V> map, Object key)
	{
		if (!map.containsKey(key))
		{
			return None.instance;
		}
		return new Some(map.get(key));
	}
	
	// Mutating Operations
	
	/**
	 * @see Map#subscript_$eq(Object, Object)
	 */
	public static @infix @inline <K, V> void subscript_$eq(java.util.Map<K, V> map, K key, V value)
	{
		map.put(key, value);
	}
	
	/**
	 * @see Map#$plus$eq(Tuple2)
	 */
	public static @infix @inline <K, V> void $plus$eq(java.util.Map<K, V> map, Entry<? extends K, ? extends V> entry)
	{
		map.put(entry.getKey(), entry.getValue());
	}
	
	/**
	 * @see Map#$plus$plus$eq(Map)
	 */
	public static @infix @inline <K, V> void $plus$plus$eq(java.util.Map<K, V> map1, java.util.Map<K, V> map2)
	{
		map1.putAll(map2);
	}
	
	/**
	 * @see Map#$minus$at$eq(Entry)
	 */
	public static @infix @inline <K, V> void $minus$at$eq(java.util.Map<K, V> map, Object key)
	{
		map.remove(key);
	}
	
	/**
	 * @see Map#$minus$eq(Entry)
	 */
	public static @infix @inline <K, V> void $minus$eq(java.util.Map<K, V> map, Entry<?, ?> entry)
	{
		map.remove(entry.getKey(), entry.getKey());
	}
	
	/**
	 * @see Map#$minus$colon$eq(Object)
	 */
	public static @infix <K, V> void $minus$colon$eq(java.util.Map<K, V> map, Object value)
	{
		map.values().remove(value);
	}
	
	/**
	 * @see Map#$minus$minus$eq(Map)
	 */
	public static @infix <K, V> void $minus$minus$eq(java.util.Map<K, V> map, java.util.Map<?, ?> remove)
	{
		for (java.util.Map.Entry<?, ?> e : remove.entrySet())
		{
			remove.remove(e.getKey(), e.getValue());
		}
	}
	
	/**
	 * @see Map#$minus$minus$eq(Collection)
	 */
	public static @infix <K, V> void $minus$minus$eq(java.util.Map<K, V> map, java.util.Collection<?> remove)
	{
		for (Object e : remove)
		{
			map.remove(e);
		}
	}
	
	/**
	 * @see Map#mapValues(BiFunction)
	 */
	public static @infix @inline <K, V> void map(java.util.Map<K, V> map, BiFunction<? super K, ? super V, ? extends V> mapper)
	{
		map.replaceAll(mapper);
	}
	
	/**
	 * @see Map#mapEntries(BiFunction)
	 */
	public static @infix @inline <K, V> void mapEntries(java.util.Map<K, V> map,
			BiFunction<? super K, ? super V, ? extends Entry<? extends K, ? extends V>> mapper)
	{
		java.util.Map<K, V> temp = new LinkedHashMap<>(map.size() << 2);
		for (java.util.Map.Entry<K, V> entry : map.entrySet())
		{
			Entry<? extends K, ? extends V> newEntry = mapper.apply(entry.getKey(), entry.getValue());
			if (newEntry != null)
			{
				temp.put(newEntry.getKey(), newEntry.getValue());
			}
		}
		
		map.clear();
		map.putAll(temp);
	}
	
	/**
	 * @see Map#flatMap(BiFunction)
	 */
	public static @infix @inline <K, V> void flatMap(java.util.Map<K, V> map,
			BiFunction<? super K, ? super V, ? extends Iterable<? extends Entry<? extends K, ? extends V>>> mapper)
	{
		java.util.Map<K, V> temp = new LinkedHashMap<>(map.size() << 2);
		for (java.util.Map.Entry<K, V> entry : map.entrySet())
		{
			for (Entry<? extends K, ? extends V> newEntry : mapper.apply(entry.getKey(), entry.getValue()))
			{
				temp.put(newEntry.getKey(), newEntry.getValue());
			}
		}
		
		map.clear();
		map.putAll(temp);
	}
	
	/**
	 * @see Map#filter(BiPredicate)
	 */
	public static @infix <K, V> void filter(java.util.Map<K, V> map, BiPredicate<? super K, ? super V> condition)
	{
		Iterator<java.util.Map.Entry<K, V>> iterator = map.entrySet().iterator();
		while (iterator.hasNext())
		{
			java.util.Map.Entry<K, V> entry = iterator.next();
			if (!condition.test(entry.getKey(), entry.getValue()))
			{
				iterator.remove();
			}
		}
	}
	
	/**
	 * @see Map#mutable()
	 */
	public static @infix <K, V> MutableMap<K, V> mutable(java.util.Map<K, V> map)
	{
		MutableMap<K, V> newMap = new dyvil.collection.mutable.HashMap();
		for (java.util.Map.Entry<K, V> entry : map.entrySet())
		{
			newMap.put(entry.getKey(), entry.getValue());
		}
		return newMap;
	}
	
	/**
	 * @see Map#immutable()
	 */
	public static @infix <K, V> ImmutableMap<K, V> immutable(java.util.Map<K, V> map)
	{
		int len = map.size();
		switch (len)
		{
		case 0:
			return EmptyMap.apply();
		case 1:
			java.util.Map.Entry<K, V> entry = map.entrySet().iterator().next();
			return SingletonMap.apply(entry.getKey(), entry.getValue());
		default:
		}
		
		Object[] keys = new Object[len];
		Object[] values = new Object[len];
		
		int index = 0;
		for (java.util.Map.Entry<K, V> entry : map.entrySet())
		{
			keys[index] = entry.getKey();
			values[index] = entry.getValue();
			index++;
		}
		return new ArrayMap(keys, values, len, true);
	}
}
