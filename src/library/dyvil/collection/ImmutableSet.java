package dyvil.collection;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import dyvil.lang.Collection;
import dyvil.lang.ImmutableException;
import dyvil.lang.Set;

public interface ImmutableSet<E> extends Set<E>, ImmutableCollection<E>
{
	// Accessors
	
	@Override
	public int size();
	
	@Override
	public Iterator<E> iterator();
	
	@Override
	public default Spliterator<E> spliterator()
	{
		return Spliterators.spliterator(this.iterator(), this.size(), Spliterator.DISTINCT | Spliterator.IMMUTABLE);
	}
	
	@Override
	public boolean $qmark(Object element);
	
	// Non-mutating Operations
	
	@Override
	public ImmutableSet<E> $plus(E element);
	
	/**
	 * {@inheritDoc} This operator represents the 'union' ImmutableSet operation
	 * and delegates to {@link #$bar(Collection)}.
	 */
	@Override
	public default ImmutableSet<? extends E> $plus$plus(Collection<? extends E> collection)
	{
		return this.$bar(collection);
	}
	
	@Override
	public ImmutableSet<E> $minus(Object element);
	
	/**
	 * {@inheritDoc} This operator represents the 'subtract' ImmutableSet
	 * operation.
	 */
	@Override
	public ImmutableSet<? extends E> $minus$minus(Collection<? extends E> collection);
	
	/**
	 * {@inheritDoc} This operator represents the 'intersect' ImmutableSet
	 * operation.
	 */
	@Override
	public ImmutableSet<? extends E> $amp(Collection<? extends E> collection);
	
	/**
	 * {@inheritDoc} This operator represents the 'union' ImmutableSet
	 * operation.
	 */
	@Override
	public ImmutableSet<? extends E> $bar(Collection<? extends E> collection);
	
	/**
	 * {@inheritDoc} This operator represents the 'exclusive OR' ImmutableSet
	 * operation.
	 */
	@Override
	public ImmutableSet<? extends E> $up(Collection<? extends E> collection);
	
	@Override
	public <R> ImmutableSet<R> mapped(Function<? super E, ? extends R> mapper);
	
	@Override
	public <R> ImmutableSet<R> flatMapped(Function<? super E, ? extends Iterable<? extends R>> mapper);
	
	@Override
	public ImmutableSet<E> filtered(Predicate<? super E> condition);
	
	// Mutating Operations
	
	@Override
	public default E add(E element)
	{
		throw new ImmutableException("add() on Immutable Set");
	}
	
	@Override
	public default boolean remove(E element)
	{
		throw new ImmutableException("remove() on Immutable Set");
	}
	
	@Override
	public default void $plus$eq(E element)
	{
		throw new ImmutableException("+= on Immutable Set");
	}
	
	@Override
	public default void $plus$plus$eq(Collection<? extends E> collection)
	{
		throw new ImmutableException("++= on Immutable Set");
	}
	
	@Override
	public default void $minus$eq(E element)
	{
		throw new ImmutableException("-= on Immutable Set");
	}
	
	@Override
	public default void $minus$minus$eq(Collection<? extends E> collection)
	{
		throw new ImmutableException("--= on Immutable Set");
	}
	
	@Override
	public default void $amp$eq(Collection<? extends E> collection)
	{
		throw new ImmutableException("&= on Immutable Set");
	}
	
	@Override
	public default void $bar$eq(Collection<? extends E> collection)
	{
		throw new ImmutableException("|= on Immutable Set");
	}
	
	@Override
	public default void $up$eq(Collection<? extends E> collection)
	{
		throw new ImmutableException("^= on Immutable Set");
	}
	
	@Override
	public default void clear()
	{
		throw new ImmutableException("clear() on Immutable Set");
	}
	
	@Override
	public default void map(UnaryOperator<E> mapper)
	{
		throw new ImmutableException("map() on Immutable Set");
	}
	
	@Override
	public default void flatMap(Function<? super E, ? extends Iterable<? extends E>> mapper)
	{
		throw new ImmutableException("flatMap() on Immutable Set");
	}
	
	@Override
	public default void filter(Predicate<? super E> condition)
	{
		throw new ImmutableException("filter() on Immutable Set");
	}
	
	// toArray
	
	@Override
	public void toArray(int index, Object[] store);
	
	// Copying
	
	@Override
	public ImmutableSet<E> copy();
	
	@Override
	public MutableSet<E> mutable();
	
	@Override
	public default ImmutableSet<E> immutable()
	{
		return this;
	}
}