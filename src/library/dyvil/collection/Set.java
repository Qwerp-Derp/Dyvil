package dyvil.collection;

import dyvil.annotation.internal.DyvilModifiers;
import dyvil.lang.LiteralConvertible;
import dyvil.reflect.Modifiers;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

@LiteralConvertible.FromNil(methodName = "empty")
@LiteralConvertible.FromArray
public interface Set<E> extends Collection<E>
{
	static <E> ImmutableSet<E> empty()
	{
		return ImmutableSet.apply();
	}
	
	static <E> MutableSet<E> apply()
	{
		return MutableSet.apply();
	}
	
	static <E> ImmutableSet<E> apply(E element)
	{
		return ImmutableSet.apply(element);
	}
	
	@SafeVarargs
	static <E> ImmutableSet<E> apply(E... elements)
	{
		return ImmutableSet.apply(elements);
	}
	
	static <E> ImmutableSet<E> from(E[] array)
	{
		return ImmutableSet.from(array);
	}

	static <E> ImmutableSet<E> from(Iterable<? extends E> iterable)
	{
		return ImmutableSet.from(iterable);
	}

	static <E> ImmutableSet<E> from(Collection<? extends E> collection)
	{
		return ImmutableSet.from(collection);
	}
	
	// Accessors
	
	@Override
	int size();
	
	@Override
	default boolean isDistinct()
	{
		return true;
	}
	
	@Override
	Iterator<E> iterator();
	
	@Override
	default Spliterator<E> spliterator()
	{
		return Spliterators.spliterator(this.iterator(), this.size(), Spliterator.DISTINCT);
	}
	
	// Non-mutating Operations
	
	@Override
	Set<E> added(E element);

	@Override
	Set<E> removed(Object element);

	/**
	 * {@inheritDoc} This operator represents the 'union' Set operation and delegates to {@link #union(Collection)}.
	 */
	@Override
	Set<E> union(Collection<? extends E> collection);

	/**
	 * {@inheritDoc} This operator represents the 'difference' Set operation.
	 */
	@Override
	Set<E> difference(Collection<?> collection);
	
	/**
	 * {@inheritDoc} This operator represents the 'intersect' Set operation.
	 */
	@Override
	Set<E> intersection(Collection<? extends E> collection);
	
	/**
	 * Returns a collection that contains all elements that are present in either this or the given {@code collection},
	 * but not in both. This operator represents the 'symmetric difference' Set operation.
	 *
	 * @param collection
	 * 		the collection
	 *
	 * @return a collection that contains all elements that are present in either this or the given collection, but not
	 * in both.
	 */
	Set<E> symmetricDifference(Collection<? extends E> collection);
	
	@Override
	<R> Set<R> mapped(Function<? super E, ? extends R> mapper);
	
	@Override
	<R> Set<R> flatMapped(Function<? super E, ? extends Iterable<? extends R>> mapper);
	
	@Override
	Set<E> filtered(Predicate<? super E> condition);
	
	// Mutating Operations
	
	@Override
	void clear();
	
	@Override
	boolean add(E element);
	
	@Override
	boolean remove(Object element);
	
	default boolean unionInplace(Collection<? extends E> collection)
	{
		return this.addAll(collection);
	}

	/**
	 * Removes all elements of the given {@code collection} from this collection and adds those that are not currently
	 * present in this collection.
	 *
	 * @param collection
	 * 	the collection to XOR with
	 */
	default boolean symmetricDifferenceInplace(Collection<? extends E> collection)
	{
		boolean changed = false;
		for (E element : collection)
		{
			if (!this.contains(element))
			{
				this.remove(element);
				changed = true;
			}
		}
		for (E element : this)
		{
			if (!collection.contains(element))
			{
				this.remove(element);
				changed = true;
			}
		}
		return changed;
	}
	
	@Override
	void map(Function<? super E, ? extends E> mapper);
	
	@Override
	void flatMap(Function<? super E, ? extends Iterable<? extends E>> mapper);
	
	// Copying and Views
	
	@Override
	Set<E> copy();

	@Override
	<RE> MutableSet<RE> emptyCopy();

	@Override
	<RE> MutableSet<RE> emptyCopy(int capacity);

	@Override
	MutableSet<E> mutable();
	
	@Override
	MutableSet<E> mutableCopy();
	
	@Override
	ImmutableSet<E> immutable();
	
	@Override
	ImmutableSet<E> immutableCopy();

	@Override
	<RE> ImmutableSet.Builder<RE> immutableBuilder();

	@Override
	<RE> ImmutableSet.Builder<RE> immutableBuilder(int capacity);

	@Override
	ImmutableSet<E> view();
	
	@Override
	java.util.Set<E> toJava();
	
	// Utility Methods
	
	static <E> boolean setEquals(Set<E> set, Object o)
	{
		return o instanceof Set && setEquals(set, (Set<E>) o);
	}
	
	static <E> boolean setEquals(Set<E> c1, Set<E> c2)
	{
		// size checked in unorderedEquals
		return Collection.unorderedEquals(c1, c2);
	}
	
	static <E> int setHashCode(Set<E> set)
	{
		return Collection.unorderedHashCode(set);
	}

	@DyvilModifiers(Modifiers.INTERNAL)
	static int distinct(Object[] array, int size)
	{
		if (size < 2)
		{
			return size;
		}
		
		for (int i = 0; i < size; i++)
		{
			for (int j = i + 1; j < size; j++)
			{
				if (Objects.equals(array[i], array[j]))
				{
					array[j--] = array[--size];
				}
			}
		}
		return size;
	}

	@DyvilModifiers(Modifiers.INTERNAL)
	static int sortDistinct(Object[] array, int size)
	{
		if (size < 2)
		{
			return size;
		}
		
		Arrays.sort(array);
		return distinctSorted(array, size);
	}

	@DyvilModifiers(Modifiers.INTERNAL)
	static <T> int sortDistinct(T[] array, int size, Comparator<? super T> comparator)
	{
		if (size < 2)
		{
			return size;
		}
		
		Arrays.sort(array, comparator);
		
		return distinctSorted(array, size);
	}

	@DyvilModifiers(Modifiers.INTERNAL)
	static int distinctSorted(Object[] array, int size)
	{
		if (size < 2)
		{
			return size;
		}
		
		int len = 0;
		int i = 1;
		
		while (i < size)
		{
			if (Objects.equals(array[i], array[len]))
			{
				i++;
			}
			else
			{
				array[++len] = array[i++];
			}
		}
		
		return len + 1;
	}

	@DyvilModifiers(Modifiers.INTERNAL)
	static boolean isDistinct(Object[] array, int size)
	{
		if (size < 2)
		{
			return true;
		}
		
		for (int i = 0; i < size; i++)
		{
			Object o = array[i];
			for (int j = 0; j < i; j++)
			{
				if (o.equals(array[j]))
				{
					return false;
				}
			}
		}
		return true;
	}

	@DyvilModifiers(Modifiers.INTERNAL)
	static boolean isDistinctSorted(Object[] array, int size)
	{
		if (size < 2)
		{
			return true;
		}
		
		for (int i = 1; i < size; i++)
		{
			if (array[i - 1].equals(array[i]))
			{
				return false;
			}
		}
		return true;
	}
}
