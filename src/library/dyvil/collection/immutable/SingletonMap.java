package dyvil.collection.immutable;

import dyvil.annotation.Immutable;
import dyvil.annotation.internal.NonNull;
import dyvil.annotation.internal.Nullable;
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

	@NonNull
	public static <K, V> SingletonMap<K, V> apply(K key, V value)
	{
		return new SingletonMap<>(key, value);
	}

	@NonNull
	public static <K, V> SingletonMap<K, V> apply(@NonNull Entry<K, V> entry)
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

	@NonNull
	@Override
	public Iterator<Entry<K, V>> iterator()
	{
		return new SingletonIterator<>(this);
	}

	@NonNull
	@Override
	public Iterator<K> keyIterator()
	{
		return new SingletonIterator<>(this.key);
	}

	@NonNull
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
	public void forEach(@NonNull Consumer<? super Entry<K, V>> action)
	{
		action.accept(this);
	}

	@Override
	public void forEach(@NonNull BiConsumer<? super K, ? super V> action)
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

	@Nullable
	@Override
	public Entry<K, V> getEntry(Object key)
	{
		return Objects.equals(key, this.key) ? this : null;
	}

	@NonNull
	@Override
	public Option<V> getOption(Object key)
	{
		return Objects.equals(key, this.key) ? new Some<>(this.value) : Option.apply();
	}

	@NonNull
	@Override
	public ImmutableMap<K, V> withEntry(K key, V value)
	{
		return ImmutableMap.singleton(key, value);
	}

	@NonNull
	@Override
	public ImmutableMap<K, V> union(@NonNull Map<? extends K, ? extends V> map)
	{
		final ImmutableMap.Builder<K, V> builder = new ArrayMap.Builder<>(map.size() + 1);
		builder.put(this.key, this.value);
		builder.putAll(map);
		return builder.build();
	}

	@NonNull
	@Override
	public ImmutableMap<K, V> keyRemoved(Object key)
	{
		return Objects.equals(this.key, key) ? EmptyMap.instance : this;
	}

	@NonNull
	@Override
	public ImmutableMap<K, V> removed(Object key, Object value)
	{
		return Objects.equals(this.key, key) && Objects.equals(this.value, value) ? EmptyMap.instance : this;
	}

	@NonNull
	@Override
	public ImmutableMap<K, V> valueRemoved(Object value)
	{
		return Objects.equals(this.value, value) ? EmptyMap.instance : this;
	}

	@NonNull
	@Override
	public ImmutableMap<K, V> difference(@NonNull Map<?, ?> map)
	{
		return map.contains(this.key, this.value) ? EmptyMap.instance : this;
	}

	@NonNull
	@Override
	public ImmutableMap<K, V> keyDifference(@NonNull Collection<?> keys)
	{
		return keys.contains(this.key) ? EmptyMap.instance : this;
	}

	@NonNull
	@Override
	public <NK> ImmutableMap<NK, V> keyMapped(@NonNull BiFunction<? super K, ? super V, ? extends NK> mapper)
	{
		return new SingletonMap<>(mapper.apply(this.key, this.value), this.value);
	}

	@NonNull
	@Override
	public <NV> ImmutableMap<K, NV> valueMapped(@NonNull BiFunction<? super K, ? super V, ? extends NV> mapper)
	{
		return new SingletonMap<>(this.key, mapper.apply(this.key, this.value));
	}

	@NonNull
	@Override
	public <NK, NV> ImmutableMap<NK, NV> entryMapped(@NonNull BiFunction<? super K, ? super V, ? extends @NonNull Entry<? extends NK, ? extends NV>> mapper)
	{
		Entry<? extends NK, ? extends NV> entry = mapper.apply(this.key, this.value);
		return entry == null ? EmptyMap.instance : new SingletonMap<>(entry.getKey(), entry.getValue());
	}

	@Nullable
	@Override
	public <NK, NV> ImmutableMap<NK, NV> flatMapped(@NonNull BiFunction<? super K, ? super V, ? extends @NonNull Iterable<? extends @NonNull Entry<? extends NK, ? extends NV>>> mapper)
	{
		ArrayMap.Builder<NK, NV> builder = new ArrayMap.Builder<>();
		for (Entry<? extends NK, ? extends NV> entry : mapper.apply(this.key, this.value))
		{
			builder.put(entry.getKey(), entry.getValue());
		}
		return builder.build();
	}

	@NonNull
	@Override
	public ImmutableMap<K, V> filtered(@NonNull BiPredicate<? super K, ? super V> condition)
	{
		return condition.test(this.key, this.value) ? this : EmptyMap.instance;
	}

	@Override
	public Entry<K, V> @NonNull[] toArray()
	{
		return (Entry<K, V>[]) new Entry[] { this };
	}

	@Override
	public void toArray(int index, @NonNull Entry<K, V> @NonNull [] store)
	{
		store[index] = this;
	}

	@Override
	public Object @NonNull [] toKeyArray()
	{
		return new Object[] { this.key };
	}

	@Override
	public void toKeyArray(int index, Object @NonNull [] store)
	{
		store[index] = this.key;
	}

	@Override
	public Object @NonNull [] toValueArray()
	{
		return new Object[] { this.value };
	}

	@Override
	public void toValueArray(int index, Object @NonNull [] store)
	{
		store[index] = this.value;
	}

	@NonNull
	@Override
	public ImmutableMap<V, K> inverted()
	{
		return new SingletonMap<>(this.value, this.key);
	}

	@NonNull
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

	@NonNull
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

	@NonNull
	@Override
	public String toString()
	{
		return Map.START_STRING + this.key + Map.KEY_VALUE_SEPARATOR_STRING + this.value + Map.END_STRING;
	}

	@Override
	public java.util.@NonNull Map<K, V> toJava()
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

	private void writeObject(java.io.@NonNull ObjectOutputStream out) throws IOException
	{
		out.writeObject(this.key);
		out.writeObject(this.value);
	}

	private void readObject(java.io.@NonNull ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		this.key = (K) in.readObject();
		this.value = (V) in.readObject();
	}
}
