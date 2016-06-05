package dyvil.collection.immutable;

import dyvil.annotation.Immutable;
import dyvil.collection.*;
import dyvil.collection.impl.AbstractHashMap;
import dyvil.lang.literal.ArrayConvertible;
import dyvil.lang.literal.ColonConvertible;
import dyvil.lang.literal.NilConvertible;
import dyvil.util.ImmutableException;

import java.util.Collections;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

@NilConvertible
@ArrayConvertible
@ColonConvertible(methodName = "singleton")
@Immutable
public class HashMap<K, V> extends AbstractHashMap<K, V> implements ImmutableMap<K, V>
{
	public static class Builder<K, V> implements ImmutableMap.Builder<K, V>
	{
		private HashMap<K, V> map;

		public Builder()
		{
			this.map = new HashMap<>();
		}

		public Builder(int capacity)
		{
			this.map = new HashMap<>(capacity);
		}

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
		public HashMap<K, V> build()
		{
			HashMap<K, V> map = this.map;
			this.map = null;
			map.flatten();
			return map;
		}
	}

	private static final long serialVersionUID = -1489214367993445801L;

	// Factory Methods

	public static <K, V> HashMap<K, V> singleton(K key, V value)
	{
		final HashMap<K, V> result = new HashMap<>(1);
		result.putInternal(key, value);
		return result;
	}

	public static <K, V> HashMap<K, V> apply()
	{
		return new HashMap<>(0);
	}

	@SafeVarargs
	public static <K, V> HashMap<K, V> apply(Entry<K, V>... entries)
	{
		return new HashMap<>(entries);
	}

	public static <K, V> HashMap<K, V> from(Entry<? extends K, ? extends V>[] array)
	{
		return new HashMap<>(array);
	}

	public static <K, V> HashMap<K, V> from(Iterable<? extends Entry<? extends K, ? extends V>> iterable)
	{
		return new HashMap<>(iterable);
	}

	public static <K, V> HashMap<K, V> from(SizedIterable<? extends Entry<? extends K, ? extends V>> iterable)
	{
		return new HashMap<>(iterable);
	}

	public static <K, V> HashMap<K, V> from(Set<? extends Entry<? extends K, ? extends V>> set)
	{
		return new HashMap<>(set);
	}

	public static <K, V> HashMap<K, V> from(Map<? extends K, ? extends V> map)
	{
		return new HashMap<>(map);
	}

	public static <K, V> HashMap<K, V> from(AbstractHashMap<? extends K, ? extends V> hashMap)
	{
		return new HashMap<>(hashMap);
	}

	public static <K, V> Builder<K, V> builder()
	{
		return new Builder<>();
	}

	public static <K, V> Builder<K, V> builder(int capacity)
	{
		return new Builder<>(capacity);
	}

	// Constructors

	protected HashMap()
	{
		super();
	}
	
	protected HashMap(int capacity)
	{
		super(capacity);
	}

	public HashMap(Entry<? extends K, ? extends V>[] entries)
	{
		super(entries);
	}

	public HashMap(Iterable<? extends Entry<? extends K, ? extends V>> iterable)
	{
		super(iterable);
	}

	public HashMap(SizedIterable<? extends Entry<? extends K, ? extends V>> iterable)
	{
		super(iterable);
	}

	public HashMap(Set<? extends Entry<? extends K, ? extends V>> set)
	{
		super(set);
	}

	public HashMap(Map<? extends K, ? extends V> map)
	{
		super(map);
	}

	public HashMap(AbstractHashMap<? extends K, ? extends V> hashMap)
	{
		super(hashMap);
	}

	// Implementation Methods
	
	@Override
	protected void addEntry(int hash, K key, V value, int index)
	{
		this.entries[index] = new HashEntry<>(key, value, hash, this.entries[index]);
		this.size++;
	}
	
	@Override
	protected void removeEntry(HashEntry<K, V> entry)
	{
		throw new ImmutableException("Iterator.remove() on Immutable Map");
	}
	
	@Override
	public ImmutableMap<K, V> withEntry(K key, V value)
	{
		HashMap<K, V> copy = new HashMap<>(this);
		copy.ensureCapacity(this.size + 1);
		copy.putInternal(key, value);
		return copy;
	}
	
	@Override
	public ImmutableMap<K, V> union(Map<? extends K, ? extends V> map)
	{
		HashMap<K, V> copy = new HashMap<>(this);
		copy.putAllInternal(map);
		return copy;
	}
	
	@Override
	public ImmutableMap<K, V> keyRemoved(Object key)
	{
		HashMap<K, V> copy = new HashMap<>(this.size);
		for (Entry<K, V> entry : this)
		{
			K entryKey = entry.getKey();
			if (!Objects.equals(entryKey, key))
			{
				copy.putInternal(entryKey, entry.getValue());
			}
		}
		return copy;
	}
	
	@Override
	public ImmutableMap<K, V> removed(Object key, Object value)
	{
		HashMap<K, V> copy = new HashMap<>(this.size);
		for (Entry<K, V> entry : this)
		{
			K entryKey = entry.getKey();
			V entryValue = entry.getValue();
			if (!Objects.equals(entryKey, key) || !Objects.equals(entryValue, value))
			{
				copy.putInternal(entryKey, entryValue);
			}
		}
		return copy;
	}
	
	@Override
	public ImmutableMap<K, V> valueRemoved(Object value)
	{
		HashMap<K, V> copy = new HashMap<>(this.size);
		for (Entry<K, V> entry : this)
		{
			V entryValue = entry.getValue();
			if (!Objects.equals(entryValue, value))
			{
				copy.putInternal(entry.getKey(), entryValue);
			}
		}
		return copy;
	}
	
	@Override
	public ImmutableMap<K, V> difference(Map<?, ?> map)
	{
		HashMap<K, V> copy = new HashMap<>(this.size);
		for (Entry<K, V> entry : this)
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
	public ImmutableMap<K, V> keyDifference(Collection<?> keys)
	{
		HashMap<K, V> copy = new HashMap<>(this.size);
		for (Entry<K, V> entry : this)
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
		HashMap<NK, V> copy = new HashMap<>(this.size);
		for (Entry<K, V> entry : this)
		{
			V value = entry.getValue();
			copy.putInternal(mapper.apply(entry.getKey(), value), value);
		}
		return copy;
	}
	
	@Override
	public <NV> ImmutableMap<K, NV> valueMapped(BiFunction<? super K, ? super V, ? extends NV> mapper)
	{
		HashMap<K, NV> copy = new HashMap<>(this.size);
		for (Entry<K, V> entry : this)
		{
			K key = entry.getKey();
			copy.putInternal(key, mapper.apply(key, entry.getValue()));
		}
		return copy;
	}
	
	@Override
	public <NK, NV> ImmutableMap<NK, NV> entryMapped(BiFunction<? super K, ? super V, ? extends Entry<? extends NK, ? extends NV>> mapper)
	{
		HashMap<NK, NV> copy = new HashMap<>(this.size);
		for (Entry<K, V> entry : this)
		{
			Entry<? extends NK, ? extends NV> newEntry = mapper.apply(entry.getKey(), entry.getValue());
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
		HashMap<NK, NV> copy = new HashMap<>(this.size);
		for (Entry<K, V> entry : this)
		{
			for (Entry<? extends NK, ? extends NV> newEntry : mapper.apply(entry.getKey(), entry.getValue()))
			{
				copy.putInternal(newEntry.getKey(), newEntry.getValue());
			}
		}
		copy.flatten();
		return copy;
	}
	
	@Override
	public ImmutableMap<K, V> filtered(BiPredicate<? super K, ? super V> condition)
	{
		HashMap<K, V> copy = new HashMap<>(this.size);
		for (Entry<K, V> entry : this)
		{
			K key = entry.getKey();
			V value = entry.getValue();
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
		HashMap<V, K> copy = new HashMap<>(this.size);
		for (Entry<K, V> entry : this)
		{
			copy.putInternal(entry.getValue(), entry.getKey());
		}
		return copy;
	}
	
	@Override
	public ImmutableMap<K, V> copy()
	{
		return this.immutableCopy();
	}

	@Override
	public MutableMap<K, V> mutable()
	{
		return this.mutableCopy();
	}

	@Override
	public <RK, RV> ImmutableMap.Builder<RK, RV> immutableBuilder()
	{
		return builder();
	}

	@Override
	public <RK, RV> ImmutableMap.Builder<RK, RV> immutableBuilder(int capacity)
	{
		return builder(capacity);
	}
	
	@Override
	public java.util.Map<K, V> toJava()
	{
		return Collections.unmodifiableMap(super.toJava());
	}
}
