package dyvil.collection.iterator;

import dyvil.annotation.Immutable;
import dyvil.annotation.Mutating;
import dyvil.annotation.internal.NonNull;
import dyvil.lang.LiteralConvertible;
import dyvil.util.ImmutableException;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

@LiteralConvertible.FromTuple
@Immutable
public class SingletonIterator<E> implements Iterator<E>
{
	private       boolean returned;
	private final E       element;

	@NonNull
	public static <E> SingletonIterator<E> apply(E element)
	{
		return new SingletonIterator(element);
	}

	public SingletonIterator(E element)
	{
		this.element = element;
	}

	@Override
	public boolean hasNext()
	{
		return !this.returned;
	}

	@Override
	public E next()
	{
		if (!this.returned)
		{
			this.returned = true;
			return this.element;
		}
		throw new NoSuchElementException("Singleton Iterator already returned the element");
	}

	@Override
	@Mutating
	public void remove()
	{
		throw new ImmutableException();
	}

	@Override
	public void forEachRemaining(Consumer<? super E> action)
	{
		Objects.requireNonNull(action);
		if (!this.returned)
		{
			action.accept(this.element);
			this.returned = true;
		}
	}

	@NonNull
	@Override
	public String toString()
	{
		return "SingletonIterator(element: " + this.element + ", returned: " + this.returned + ")";
	}
}
