package dyvil.collection;

import dyvil.annotation.Immutable;
import dyvil.annotation.Mutating;
import dyvil.annotation.internal.Covariant;
import dyvil.annotation.internal.DyvilName;
import dyvil.collection.immutable.ArrayList;
import dyvil.collection.range.*;
import dyvil.lang.LiteralConvertible;
import dyvil.util.ImmutableException;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@LiteralConvertible.FromNil
@LiteralConvertible.FromTuple
@Immutable
public interface Range<@Covariant T> extends Queryable<T>, Serializable
{
	@DyvilName("apply")
	static <T> Range empty()
	{
		return EmptyRange.instance;
	}

	@DyvilName("apply")
	static IntRange closed(int from, int to)
	{
		return IntRange.closed(from, to);
	}

	@DyvilName("apply")
	static IntRange halfOpen(int from, int toExclusive)
	{
		return IntRange.halfOpen(from, toExclusive);
	}

	@DyvilName("apply")
	static LongRange closed(long from, long to)
	{
		return LongRange.closed(from, to);
	}

	@DyvilName("apply")
	static LongRange halfOpen(long from, long toExclusive)
	{
		return LongRange.halfOpen(from, toExclusive);
	}

	@DyvilName("apply")
	static FloatRange closed(float from, float to)
	{
		return FloatRange.closed(from, to);
	}

	@DyvilName("apply")
	static FloatRange halfOpen(float from, float toExclusive)
	{
		return FloatRange.halfOpen(from, toExclusive);
	}

	@DyvilName("apply")
	static DoubleRange closed(double from, double to)
	{
		return DoubleRange.closed(from, to);
	}

	@DyvilName("apply")
	static DoubleRange halfOpen(double from, double toExclusive)
	{
		return DoubleRange.halfOpen(from, toExclusive);
	}

	@DyvilName("apply")
	static <T extends Rangeable<T>> Range<T> closed(T from, T to)
	{
		return new dyvil.collection.range.closed.ObjectRange<>(from, to);
	}

	@DyvilName("apply")
	static <T extends Rangeable<T>> Range<T> halfOpen(T from, T toExclusive)
	{
		return new dyvil.collection.range.halfopen.ObjectRange<>(from, toExclusive);
	}

	/**
	 * Returns the first element in this range
	 *
	 * @return the first element in this range
	 */
	T first();

	/**
	 * Returns the last element in this range
	 *
	 * @return the last element in this range
	 */
	T last();

	/**
	 * Returns the exact number of elements in this range, i.e. the number of elements that would be returned by the
	 * {@link #iterator()}.
	 *
	 * @return the number of elements in this range
	 */
	@Override
	int size();

	Range asHalfOpen();

	Range asClosed();

	boolean isHalfOpen();

	@Override
	Iterator<T> iterator();

	@Override
	default Spliterator<T> spliterator()
	{
		int size = this.size();
		int characteristics = Spliterator.ORDERED;
		if (size >= 0)
		{
			characteristics |= Spliterator.SIZED;
		}
		return Spliterators.spliterator(this.iterator(), size, characteristics);
	}

	@Override
	boolean contains(Object o);

	@Override
	void forEach(Consumer<? super T> action);

	@Override
	@Mutating
	default void map(Function<? super T, ? extends T> mapper)
	{
		throw new ImmutableException("map() on Immutable Range");
	}

	@Override
	@Mutating
	default void flatMap(Function<? super T, ? extends Iterable<? extends T>> mapper)
	{
		throw new ImmutableException("flatMap() on Immutable Range");
	}

	@Override
	@Mutating
	default void filter(Predicate<? super T> condition)
	{
		throw new ImmutableException("filter() on Immutable Range");
	}

	@Override
	default <R> Queryable<R> mapped(Function<? super T, ? extends R> mapper)
	{
		final ArrayList.Builder<R> builder = new ArrayList.Builder<>(this.size());
		for (T value : this)
		{
			builder.add(mapper.apply(value));
		}
		return builder.build();
	}

	@Override
	default <R> Queryable<R> flatMapped(Function<? super T, ? extends Iterable<? extends R>> mapper)
	{
		final ArrayList.Builder<R> builder = new ArrayList.Builder<>(this.size());
		for (T value : this)
		{
			for (R result : mapper.apply(value))
			{
				builder.add(result);
			}
		}
		return builder.build();
	}

	@Override
	default Queryable<T> filtered(Predicate<? super T> condition)
	{
		final ArrayList.Builder<T> builder = new ArrayList.Builder<>(this.size());
		for (T value : this)
		{
			if (condition.test(value))
			{
				builder.add(value);
			}
		}
		return builder.build();
	}

	// toArray

	default Object[] toArray()
	{
		Object[] array = new Object[this.size()];
		this.toArray(0, array);
		return array;
	}

	default T[] toArray(Class<T> type)
	{
		T[] array = (T[]) Array.newInstance(type, this.size());
		this.toArray(0, array);
		return array;
	}

	default void toArray(Object[] store)
	{
		this.toArray(0, store);
	}

	void toArray(int index, Object[] store);

	// Copying

	Range copy();

	// toString, equals and hashCode

	@Override
	String toString();

	@Override
	boolean equals(Object obj);

	@Override
	int hashCode();

	static boolean rangeEquals(Range range, Object o)
	{
		return o instanceof Range && rangeEquals(range, (Range) o);
	}

	static boolean rangeEquals(Range range1, Range range2)
	{
		return range1.first().equals(range2.first()) && range1.last().equals(range2.last()) // same first and last
			       && range1.size() == range2.size(); // same size
	}

	static int rangeHashCode(Range range)
	{
		return range.first().hashCode() * 31 + range.last().hashCode();
	}
}
