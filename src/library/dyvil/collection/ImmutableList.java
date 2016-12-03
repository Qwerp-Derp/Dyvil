package dyvil.collection;

import dyvil.annotation.Immutable;
import dyvil.annotation.Mutating;
import dyvil.annotation.internal.Covariant;
import dyvil.collection.immutable.AppendList;
import dyvil.collection.immutable.ArrayList;
import dyvil.collection.immutable.EmptyList;
import dyvil.collection.immutable.SingletonList;
import dyvil.lang.LiteralConvertible;
import dyvil.ref.ObjectRef;
import dyvil.util.ImmutableException;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;

@LiteralConvertible.FromNil
@LiteralConvertible.FromArray
@Immutable
public interface ImmutableList<@Covariant E> extends List<E>, ImmutableCollection<E>
{
	interface Builder<E> extends ImmutableCollection.Builder<E>
	{
		@Override
		ImmutableList<E> build();
	}
	
	static <E> ImmutableList<E> apply()
	{
		return EmptyList.apply();
	}
	
	static <E> ImmutableList<E> apply(E element)
	{
		return SingletonList.apply(element);
	}
	
	static <E> ImmutableList<E> apply(E e1, E e2)
	{
		return ArrayList.apply(e1, e2);
	}
	
	static <E> ImmutableList<E> apply(E e1, E e2, E e3)
	{
		return ArrayList.apply(e1, e2, e3);
	}
	
	@SafeVarargs
	static <E> ImmutableList<E> apply(E... elements)
	{
		return ArrayList.apply(elements);
	}
	
	static <E> ImmutableList<E> from(E[] array)
	{
		return ArrayList.from(array);
	}

	static <E> ImmutableList<E> from(Iterable<? extends E> iterable)
	{
		return ArrayList.from(iterable);
	}

	static <E> ImmutableList<E> from(Collection<? extends E> collection)
	{
		return ArrayList.from(collection);
	}

	static <E> ImmutableList<E> repeat(int count, E repeatedValue)
	{
		E[] elements = (E[]) new Object[count];
		for (int i = 0; i < count; i++)
		{
			elements[i] = repeatedValue;
		}
		return new ArrayList<>(elements, count, true);
	}

	static <E> ImmutableList<E> generate(int count, IntFunction<E> generator)
	{
		E[] elements = (E[]) new Object[count];
		for (int i = 0; i < count; i++)
		{
			elements[i] = generator.apply(i);
		}
		return new ArrayList<>(elements, count, true);
	}

	@SafeVarargs
	static <E> ImmutableList<E> linked(E... elements)
	{
		return AppendList.apply(elements);
	}
	
	static <E> Builder<E> builder()
	{
		return ArrayList.builder();
	}
	
	static <E> Builder<E> builder(int capacity)
	{
		return ArrayList.builder(capacity);
	}

	static <E> Builder<E> linkedBuilder()
	{
		return AppendList.builder();
	}
	
	// Accessors
	
	@Override
	int size();
	
	@Override
	Iterator<E> iterator();
	
	@Override
	Iterator<E> reverseIterator();
	
	@Override
	default Spliterator<E> spliterator()
	{
		return Spliterators.spliterator(this.iterator(), this.size(), Spliterator.SIZED | Spliterator.IMMUTABLE);
	}
	
	@Override
	E get(int index);
	
	// Non-mutating Operations
	
	@Override
	ImmutableList<E> subList(int startIndex, int length);
	
	@Override
	ImmutableList<E> added(E element);
	
	@Override
	ImmutableList<E> union(Collection<? extends E> collection);
	
	@Override
	ImmutableList<E> removed(Object element);
	
	@Override
	ImmutableList<E> difference(Collection<?> collection);
	
	@Override
	ImmutableList<E> intersection(Collection<? extends E> collection);
	
	@Override
	<R> ImmutableList<R> mapped(Function<? super E, ? extends R> mapper);
	
	@Override
	<R> ImmutableList<R> flatMapped(Function<? super E, ? extends Iterable<? extends R>> mapper);
	
	@Override
	ImmutableList<E> filtered(Predicate<? super E> condition);
	
	@Override
	ImmutableList<E> reversed();
	
	@Override
	ImmutableList<E> sorted();
	
	@Override
	ImmutableList<E> sorted(Comparator<? super E> comparator);
	
	@Override
	ImmutableList<E> distinct();
	
	@Override
	ImmutableList<E> distinct(Comparator<? super E> comparator);

	// Mutating Operations

	@Override
	@Mutating
	default void clear()
	{
		throw new ImmutableException("clear() on Immutable List");
	}

	@Override
	@Mutating
	default void ensureCapacity(int minSize)
	{
	}

	@Override
	@Mutating
	default void subscript_$eq(int index, E element)
	{
		throw new ImmutableException("subscript() on Immutable List");
	}

	@Override
	@Mutating
	default List<E> subscript(Range<Integer> range)
	{
		throw new ImmutableException("subscript() on Immutable List");
	}

	@Override
	@Mutating
	default void subscript_$eq(Range<Integer> range, E[] elements)
	{
		throw new ImmutableException("subscript_=() on Immutable List");
	}

	@Override
	@Mutating
	default void subscript_$eq(Range<Integer> range, List<? extends E> elements)
	{
		throw new ImmutableException("subscript_=() on Immutable List");
	}

	@Override
	@Mutating
	default ObjectRef<E> subscript_$amp(int index)
	{
		throw new ImmutableException("subscript_&() on Immutable List");
	}

	@Override
	@Mutating
	default E set(int index, E element)
	{
		throw new ImmutableException("set() on Immutable List");
	}

	@Override
	@Mutating
	default E setResizing(int index, E element)
	{
		throw new ImmutableException("setResizing() on Immutable List");
	}

	@Override
	@Mutating
	default void insert(int index, E element)
	{
		throw new ImmutableException("insert() on Immutable List");
	}

	@Override
	@Mutating
	default void insertResizing(int index, E element)
	{
		throw new ImmutableException("insertResizing() on Immutable List");
	}

	@Override
	@Mutating
	default void addElement(E element)
	{
		throw new ImmutableException("addElement() on Immutable List");
	}

	@Override
	@Mutating
	default boolean add(E element)
	{
		throw new ImmutableException("add() on Immutable List");
	}

	@Override
	@Mutating
	default boolean addAll(Collection<? extends E> collection)
	{
		throw new ImmutableException("addAll() on Immutable List");
	}
	
	@Override
	@Mutating
	default boolean remove(Object element)
	{
		throw new ImmutableException("remove() on Immutable List");
	}
	
	@Override
	@Mutating
	default boolean removeFirst(Object element)
	{
		throw new ImmutableException("removeFirst() on Immutable List");
	}
	
	@Override
	@Mutating
	default boolean removeLast(Object element)
	{
		throw new ImmutableException("removeLast() on Immutable List");
	}
	
	@Override
	@Mutating
	default void removeAt(int index)
	{
		throw new ImmutableException("removeAt() on Immutable List");
	}
	
	@Override
	@Mutating
	default boolean removeAll(Collection<?> collection)
	{
		throw new ImmutableException("removeAll() on Immutable List");
	}
	
	@Override
	@Mutating
	default boolean retainAll(Collection<? extends E> collection)
	{
		throw new ImmutableException("intersect() on Immutable List");
	}
	
	@Override
	@Mutating
	default void filter(Predicate<? super E> condition)
	{
		throw new ImmutableException("filter() on Immutable List");
	}
	
	@Override
	@Mutating
	default void map(Function<? super E, ? extends E> mapper)
	{
		throw new ImmutableException("map() on Immutable List");
	}
	
	@Override
	@Mutating
	default void flatMap(Function<? super E, ? extends Iterable<? extends E>> mapper)
	{
		throw new ImmutableException("flatMap() on Immutable List");
	}
	
	@Override
	@Mutating
	default void reverse()
	{
		throw new ImmutableException("reverse() on Immutable List");
	}
	
	@Override
	@Mutating
	default void sort()
	{
		throw new ImmutableException("sort() on Immutable List");
	}
	
	@Override
	@Mutating
	default void sort(Comparator<? super E> comparator)
	{
		throw new ImmutableException("sort() on Immutable List");
	}
	
	@Override
	@Mutating
	default void distinguish()
	{
		throw new ImmutableException("distinguish() on Immutable List");
	}
	
	@Override
	@Mutating
	default void distinguish(Comparator<? super E> comparator)
	{
		throw new ImmutableException("disinguish() on Immutable List");
	}
	
	// Searching
	
	@Override
	int indexOf(Object element);
	
	@Override
	int lastIndexOf(Object element);
	
	// Copying and Views
	
	@Override
	ImmutableList<E> copy();

	@Override
	<RE> MutableList<RE> emptyCopy();

	@Override
	<RE> MutableList<RE> emptyCopy(int capacity);

	@Override
	MutableList<E> mutable();
	
	@Override
	default MutableList<E> mutableCopy()
	{
		return this.mutable();
	}
	
	@Override
	default ImmutableList<E> immutable()
	{
		return this;
	}
	
	@Override
	default ImmutableList<E> immutableCopy()
	{
		return this.copy();
	}

	@Override
	<RE> Builder<RE> immutableBuilder();

	@Override
	<RE> Builder<RE> immutableBuilder(int capacity);

	@Override
	default ImmutableList<E> view()
	{
		return this;
	}
}
