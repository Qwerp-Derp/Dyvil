package dyvil.collection.immutable;

import dyvil.annotation.Immutable;
import dyvil.collection.*;
import dyvil.collection.iterator.SingletonIterator;
import dyvil.lang.LiteralConvertible;
import dyvil.tuple.Tuple;
import dyvil.util.Option;
import dyvil.util.Some;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

@Immutable
@LiteralConvertible.FromColonOperator
public class SingletonMap<K, V> implements ImmutableMap<K, V>, Entry<K, V>
{
	private static final long serialVersionUID = 2791619158507681686L;
	
	private transient K key;
	private transient V value;
	
	public static <K, V> SingletonMap<K, V> apply(K key, V value)
	{
		return new SingletonMap<>(key, value);
	}
	
	public static <K, V> SingletonMap<K, V> apply(Entry<K, V> entry)
	{
		return new SingletonMap<>(entry.getKey(), entry.getValue());
	}
	
	public SingletonMap(K key, V value)
	{
		this.key = key;
		this.value = value;
	}
	
	@Override
	public int size()
	{
		return 1;
	}
	
	@Override
	public boolean isEmpty()
	{
		return false;
	}
	
	@Override
	public Iterator<Entry<K, V>> iterator()
	{
		return new SingletonIterator<>(this);
	}
	
	@Override
	public Iterator<K> keyIterator()
	{
		return new SingletonIterator<>(this.key);
	}
	
	@Override
	public Iterator<V> valueIterator()
	{
		return new SingletonIterator<>(this.value);
	}
	
	@Override
	public K getKey()
	{
		return this.key;
	}
	
	@Override
	public V getValue()
	{
		return this.value;
	}
	
	@Override
	public void forEach(Consumer<? super Entry<K, V>> action)
	{
		action.accept(this);
	}
	
	@Override
	public void forEach(BiConsumer<? super K, ? super V> action)
	{
		action.accept(this.key, this.value);
	}
	
	@Override
	public boolean containsKey(Object key)
	{
		return Objects.equals(this.key, key);
	}
	
	@Override
	public boolean contains(Object key, Object value)
	{
		return Objects.equals(this.key, key) && Objects.equals(this.value, value);
	}
	
	@Override
	public boolean containsValue(Object value)
	{
		return Objects.equals(this.value, value);
	}
	
	@Override
	public V get(Object key)
	{
		return Objects.equals(key, this.key) ? this.value : null;
	}

	@Override
	public Entry<K, V> getEntry(Object key)
	{
		return Objects.equals(key, this.key) ? this : null;
	}

	@Override
	public Option<V> getOption(Object key)
	{
		return Objects.equals(key, this.key) ? new Some<>(this.value) : Option.apply();
	}
	
	@Override
	public ImmutableMap<K, V> withEntry(K key, V value)
	{
		return ImmutableMap.singleton(key, value);
	}
	
	@Override
	public ImmutableMap<K, V> union(Map<? extends K, ? extends V> map)
	{
		final ImmutableMap.Builder<K, V> builder = new ArrayMap.Builder<>(map.size() + 1);
		builder.put(this.key, this.value);
		builder.putAll(map);
		return builder.build();
	}
	
	@Override
	public ImmutableMap<K, V> keyRemoved(Object key)
	{
		return Objects.equals(this.key, key) ? EmptyMap.instance : this;
	}
	
	@Override
	public ImmutableMap<K, V> removed(Object key, Object value)
	{
		return Objects.equals(this.key, key) && Objects.equals(this.value, value) ? EmptyMap.instance : this;
	}
	
	@Override
	public ImmutableMap<K, V> valueRemoved(Object value)
	{
		return Objects.equals(this.value, value) ? EmptyMap.instance : this;
	}
	
	@Override
	public ImmutableMap<K, V> difference(Map<?, ?> map)
	{
		return map.contains(this.key, this.value) ? EmptyMap.instance : this;
	}
	
	@Override
	public ImmutableMap<K, V> keyDifference(Collection<?> keys)
	{
		return keys.contains(this.key) ? EmptyMap.instance : this;
	}
	
	@Override
	public <NK> ImmutableMap<NK, V> keyMapped(BiFunction<? super K, ? super V, ? extends NK> mapper)
	{
		return new SingletonMap<>(mapper.apply(this.key, this.value), this.value);
	}
	
	@Override
	public <NV> ImmutableMap<K, NV> valueMapped(BiFunction<? super K, ? super V, ? extends NV> mapper)
	{
		return new SingletonMap<>(this.key, mapper.apply(this.key, this.value));
	}
	
	@Override
	public <NK, NV> ImmutableMap<NK, NV> entryMapped(BiFunction<? super K, ? super V, ? extends Entry<? extends NK, ? extends NV>> mapper)
	{
		Entry<? extends NK, ? extends NV> entry = mapper.apply(this.key, this.value);
		return entry == null ? EmptyMap.instance : new SingletonMap<>(entry.getKey(), entry.getValue());
	}
	
	@Override
	public <NK, NV> ImmutableMap<NK, NV> flatMapped(BiFunction<? super K, ? super V, ? extends Iterable<? extends Entry<? extends NK, ? extends NV>>> mapper)
	{
		ArrayMap.Builder<NK, NV> builder = new ArrayMap.Builder<>();
		for (Entry<? extends NK, ? extends NV> entry : mapper.apply(this.key, this.value))
		{
			builder.put(entry.getKey(), entry.getValue());
		}
		return builder.build();
	}
	
	@Override
	public ImmutableMap<K, V> filtered(BiPredicate<? super K, ? super V> condition)
	{
		return condition.test(this.key, this.value) ? this : EmptyMap.instance;
	}
	
	@Override
	public Entry<K, V>[] toArray()
	{
		return (Entry<K, V>[]) new Entry[] { this };
	}
	
	@Override
	public void toArray(int index, Entry<K, V>[] store)
	{
		store[index] = this;
	}
	
	@Override
	public Object[] toKeyArray()
	{
		return new Object[] { this.key };
	}
	
	@Override
	public void toKeyArray(int index, Object[] store)
	{
		store[index] = this.key;
	}
	
	@Override
	public Object[] toValueArray()
	{
		return new Object[] { this.value };
	}
	
	@Override
	public void toValueArray(int index, Object[] store)
	{
		store[index] = this.value;
	}
	
	@Override
	public ImmutableMap<V, K> inverted()
	{
		return new SingletonMap<>(this.value, this.key);
	}
	
	@Override
	public ImmutableMap<K, V> copy()
	{
		return new SingletonMap<>(this.key, this.value);
	}

	@Override
	public <RK, RV> MutableMap<RK, RV> emptyCopy()
	{
		return MutableMap.apply();
	}

	@Override
	public <RK, RV> MutableMap<RK, RV> emptyCopy(int capacity)
	{
		return MutableMap.withCapacity(capacity);
	}
	
	@Override
	public MutableMap<K, V> mutable()
	{
		return MutableMap.apply(new Tuple.Of2<>(this.key, this.value));
	}

	@Override
	public <RK, RV> Builder<RK, RV> immutableBuilder()
	{
		return ImmutableMap.builder();
	}

	@Override
	public <RK, RV> Builder<RK, RV> immutableBuilder(int capacity)
	{
		return ImmutableMap.builder(capacity);
	}
	
	@Override
	public String toString()
	{
		return Map.START_STRING + this.key + Map.KEY_VALUE_SEPARATOR_STRING + this.value + Map.END_STRING;
	}
	
	@Override
	public java.util.Map<K, V> toJava()
	{
		return Collections.singletonMap(this.key, this.value);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof Map)
		{
			return Map.mapEquals(this, (Map<K, V>) obj);
		}
		return obj instanceof Entry && Entry.entryEquals(this, (Entry) obj);
	}
	
	@Override
	public int hashCode()
	{
		return Entry.entryHashCode(this);
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException
	{
		out.writeObject(this.key);
		out.writeObject(this.value);
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		this.key = (K) in.readObject();
		this.value = (V) in.readObject();
	}
}
