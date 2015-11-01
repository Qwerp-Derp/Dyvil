package dyvil.collection.immutable;

import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import dyvil.lang.literal.ArrayConvertible;

import dyvil.collection.*;
import dyvil.collection.impl.AbstractTreeMap;
import dyvil.tuple.Tuple2;

@ArrayConvertible
public class TreeMap<K, V> extends AbstractTreeMap<K, V>implements ImmutableMap<K, V>
{
	private static final long serialVersionUID = 2012245218476747334L;
	
	public static <K extends Comparable<K>, V> TreeMap<K, V> apply(Tuple2<K, V>... entries)
	{
		TreeMap<K, V> map = new TreeMap();
		for (Tuple2<K, V> entry : entries)
		{
			map.putInternal(entry._1, entry._2);
		}
		return map;
	}
	
	public static <K, V> Builder<K, V> builder()
	{
		return new Builder<K, V>();
	}
	
	protected static final class Builder<K, V> implements ImmutableMap.Builder<K, V>
	{
		private TreeMap<K, V> map = new TreeMap<K, V>();
		
		@Override
		public void put(K key, V value)
		{
			if (this.map == null)
			{
				throw new IllegalStateException("Already built!");
			}
			this.map.putInternal(key, value);
		}
		
		@Override
		public ImmutableMap<K, V> build()
		{
			TreeMap<K, V> map = this.map;
			this.map = null;
			return map;
		}
	}
	
	public TreeMap()
	{
	}
	
	public TreeMap(Map<? extends K, ? extends V> map)
	{
		super(map, null);
	}
	
	public TreeMap(Map<? extends K, ? extends V> m, Comparator<? super K> comparator)
	{
		super(m, comparator);
	}
	
	@Override
	public ImmutableMap<K, V> $plus(K key, V value)
	{
		TreeMap<K, V> copy = new TreeMap(this, this.comparator);
		copy.putInternal(key, value);
		return copy;
	}
	
	@Override
	public ImmutableMap<K, V> $plus$plus(Map<? extends K, ? extends V> map)
	{
		TreeMap<K, V> copy = new TreeMap(this, this.comparator);
		for (Entry<? extends K, ? extends V> entry : map)
		{
			copy.putInternal(entry.getKey(), entry.getValue());
		}
		return copy;
	}
	
	@Override
	public ImmutableMap<K, V> $minus$at(Object key)
	{
		TreeMap<K, V> copy = new TreeMap(this, this.comparator);
		boolean found = false;
		for (TreeEntry<K, V> entry = this.getFirstEntry(); entry != null; entry = successor(entry))
		{
			K entryKey = entry.getKey();
			if (!found && Objects.equals(key, entryKey))
			{
				found = true;
				continue;
			}
			
			copy.putInternal(entryKey, entry.getValue());
		}
		return copy;
	}
	
	@Override
	public ImmutableMap<K, V> $minus(Object key, Object value)
	{
		TreeMap<K, V> copy = new TreeMap(this, this.comparator);
		boolean found = false;
		for (TreeEntry<K, V> entry = this.getFirstEntry(); entry != null; entry = successor(entry))
		{
			K entryKey = entry.getKey();
			V entryValue = entry.getValue();
			if (!found && Objects.equals(key, entryKey))
			{
				found = true;
				if (Objects.equals(value, entryValue))
				{
					continue;
				}
			}
			
			copy.putInternal(entryKey, entryValue);
		}
		return copy;
	}
	
	@Override
	public ImmutableMap<K, V> $minus$colon(Object value)
	{
		TreeMap<K, V> copy = new TreeMap(this, this.comparator);
		for (TreeEntry<K, V> entry = this.getFirstEntry(); entry != null; entry = successor(entry))
		{
			V entryValue = entry.getValue();
			if (!Objects.equals(value, entryValue))
			{
				copy.putInternal(entry.getKey(), entryValue);
			}
		}
		return copy;
	}
	
	@Override
	public ImmutableMap<K, V> $minus$minus(Map<?, ?> map)
	{
		TreeMap<K, V> copy = new TreeMap(this, this.comparator);
		for (TreeEntry<K, V> entry = this.getFirstEntry(); entry != null; entry = successor(entry))
		{
			K entryKey = entry.getKey();
			V entryValue = entry.getValue();
			if (!map.contains(entryKey, entryValue))
			{
				copy.putInternal(entryKey, entryValue);
			}
		}
		return copy;
	}
	
	@Override
	public ImmutableMap<K, V> $minus$minus(Collection<?> keys)
	{
		TreeMap<K, V> copy = new TreeMap(this, this.comparator);
		for (TreeEntry<K, V> entry = this.getFirstEntry(); entry != null; entry = successor(entry))
		{
			K entryKey = entry.getKey();
			if (!keys.contains(entryKey))
			{
				copy.putInternal(entryKey, entry.getValue());
			}
		}
		return copy;
	}
	
	@Override
	public <NK> ImmutableMap<NK, V> keyMapped(BiFunction<? super K, ? super V, ? extends NK> mapper)
	{
		TreeMap<NK, V> copy = new TreeMap(this, this.comparator);
		for (TreeEntry<K, V> entry = this.getFirstEntry(); entry != null; entry = successor(entry))
		{
			V value = entry.value;
			copy.putInternal(mapper.apply(entry.key, value), value);
		}
		return copy;
	}
	
	@Override
	public <NV> ImmutableMap<K, NV> valueMapped(BiFunction<? super K, ? super V, ? extends NV> mapper)
	{
		TreeMap<K, NV> copy = new TreeMap(this, this.comparator);
		for (TreeEntry<K, V> entry = this.getFirstEntry(); entry != null; entry = successor(entry))
		{
			K key = entry.key;
			copy.putInternal(key, mapper.apply(key, entry.getValue()));
		}
		return null;
	}
	
	@Override
	public <NK, NV> ImmutableMap<NK, NV> entryMapped(BiFunction<? super K, ? super V, ? extends Entry<? extends NK, ? extends NV>> mapper)
	{
		TreeMap<NK, NV> copy = new TreeMap(this, this.comparator);
		for (TreeEntry<K, V> entry = this.getFirstEntry(); entry != null; entry = successor(entry))
		{
			Entry<? extends NK, ? extends NV> newEntry = mapper.apply(entry.key, entry.value);
			if (newEntry != null)
			{
				copy.putInternal(newEntry.getKey(), newEntry.getValue());
			}
		}
		return copy;
	}
	
	@Override
	public <NK, NV> ImmutableMap<NK, NV> flatMapped(BiFunction<? super K, ? super V, ? extends Iterable<? extends Entry<? extends NK, ? extends NV>>> mapper)
	{
		TreeMap<NK, NV> copy = new TreeMap(this, this.comparator);
		for (TreeEntry<K, V> entry = this.getFirstEntry(); entry != null; entry = successor(entry))
		{
			for (Entry<? extends NK, ? extends NV> newEntry : mapper.apply(entry.key, entry.value))
			{
				copy.putInternal(newEntry.getKey(), newEntry.getValue());
			}
		}
		return copy;
	}
	
	@Override
	public ImmutableMap<K, V> filtered(BiPredicate<? super K, ? super V> condition)
	{
		TreeMap<K, V> copy = new TreeMap(this, this.comparator);
		for (TreeEntry<K, V> entry = this.getFirstEntry(); entry != null; entry = successor(entry))
		{
			K key = entry.key;
			V value = entry.value;
			if (condition.test(key, value))
			{
				copy.putInternal(key, value);
			}
		}
		return copy;
	}
	
	@Override
	public ImmutableMap<V, K> inverted()
	{
		TreeMap<V, K> copy = new TreeMap(this);
		for (TreeEntry<K, V> entry = this.getFirstEntry(); entry != null; entry = successor(entry))
		{
			copy.putInternal(entry.value, entry.key);
		}
		return copy;
	}
	
	@Override
	public ImmutableMap<K, V> copy()
	{
		return new TreeMap(this, this.comparator);
	}
	
	@Override
	public MutableMap<K, V> mutable()
	{
		return new dyvil.collection.mutable.TreeMap(this, this.comparator);
	}
	
	@Override
	public java.util.Map<K, V> toJava()
	{
		return Collections.unmodifiableMap(super.toJava());
	}
}
