package dyvil.collection.immutable;

import dyvil.annotation.Immutable;
import dyvil.collection.Collection;
import dyvil.collection.ImmutableSet;
import dyvil.collection.MutableSet;
import dyvil.collection.impl.AbstractArraySet;
import dyvil.lang.literal.ArrayConvertible;
import dyvil.util.ImmutableException;

import java.util.Collections;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

@ArrayConvertible
@Immutable
public class ArraySet<E> extends AbstractArraySet<E> implements ImmutableSet<E>
{
	public static class Builder<E> implements ImmutableSet.Builder<E>
	{
		private ArraySet<E> result;

		public Builder()
		{
			this.result = new ArraySet<>();
		}

		public Builder(int capacity)
		{
			this.result = new ArraySet<>(capacity);
		}

		@Override
		public void add(E element)
		{
			if (this.result == null)
			{
				throw new IllegalStateException("Already built");
			}

			this.result.addInternal(element);
		}

		@Override
		public ArraySet<E> build()
		{
			final ArraySet<E> result = this.result;
			this.result = null;
			return result;
		}
	}

	private static final long serialVersionUID = 5534347282324757054L;

	@SafeVarargs
	public static <E> ArraySet<E> apply(E... elements)
	{
		return new ArraySet<>(elements, true);
	}

	public static <E> ArraySet<E> fromArray(E[] elements)
	{
		return new ArraySet<>(elements);
	}

	public static <E> Builder<E> builder()
	{
		return new Builder<>();
	}

	public static <E> Builder<E> builder(int capacity)
	{
		return new Builder<>(capacity);
	}

	protected ArraySet()
	{
		super();
	}

	protected ArraySet(int capacity)
	{
		super(capacity);
	}

	@SafeVarargs
	public ArraySet(E... elements)
	{
		super((Object[]) elements);
	}

	public ArraySet(E[] elements, int size)
	{
		super(elements, size);
	}

	public ArraySet(E[] elements, boolean trusted)
	{
		super(elements, elements.length, trusted);
	}

	public ArraySet(E[] elements, int size, boolean trusted)
	{
		super(elements, size, trusted);
	}

	public ArraySet(Collection<E> elements)
	{
		super(elements);
	}

	@Override
	protected void removeAt(int index)
	{
		throw new ImmutableException("removeAt() on Immutable Set");
	}

	@Override
	public ImmutableSet<E> added(E element)
	{
		if (this.contains(element))
		{
			return this;
		}

		Object[] newArray = new Object[this.size + 1];
		System.arraycopy(this.elements, 0, newArray, 0, this.size);
		newArray[this.size] = element;
		return new ArraySet<>((E[]) newArray, this.size + 1, true);
	}

	@Override
	public ImmutableSet<E> removed(Object element)
	{
		Object[] newArray = new Object[this.size];
		int index = 0;
		for (int i = 0; i < this.size; i++)
		{
			final Object thisElement = this.elements[i];
			if (!Objects.equals(thisElement, element))
			{
				newArray[index++] = thisElement;
			}
		}
		return new ArraySet<>((E[]) newArray, index, true);
	}

	@Override
	public ImmutableSet<? extends E> difference(Collection<?> collection)
	{
		Object[] newArray = new Object[this.size];
		int index = 0;
		for (int i = 0; i < this.size; i++)
		{
			E element = (E) this.elements[i];
			if (!collection.contains(element))
			{
				newArray[index++] = element;
			}
		}
		return new ArraySet<>((E[]) newArray, index, true);
	}

	@Override
	public ImmutableSet<? extends E> intersection(Collection<? extends E> collection)
	{
		Object[] newArray = new Object[Math.min(this.size, collection.size())];
		int index = 0;
		for (int i = 0; i < this.size; i++)
		{
			E element = (E) this.elements[i];
			if (collection.contains(element))
			{
				newArray[index++] = element;
			}
		}
		return new ArraySet<>((E[]) newArray, index, true);
	}

	@Override
	public ImmutableSet<? extends E> union(Collection<? extends E> collection)
	{
		int size = this.size;
		Object[] newArray = new Object[size + collection.size()];
		System.arraycopy(this.elements, 0, newArray, 0, this.size);
		for (E element : collection)
		{
			if (!this.contains(element))
			{
				newArray[size++] = element;
			}
		}
		return new ArraySet<>((E[]) newArray, size, true);
	}

	@Override
	public ImmutableSet<? extends E> symmetricDifference(Collection<? extends E> collection)
	{
		Object[] newArray = new Object[this.size + collection.size()];
		int index = 0;
		for (int i = 0; i < this.size; i++)
		{
			Object element = this.elements[i];
			if (!collection.contains(element))
			{
				newArray[index++] = element;
			}
		}
		for (E element : collection)
		{
			if (!this.contains(element))
			{
				newArray[index++] = element;
			}
		}
		return new ArraySet<>((E[]) newArray, index, true);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> ImmutableSet<R> mapped(Function<? super E, ? extends R> mapper)
	{
		ArraySet<R> copy = (ArraySet<R>) this.copy();
		copy.mapImpl((Function) mapper);
		return copy;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> ImmutableSet<R> flatMapped(Function<? super E, ? extends Iterable<? extends R>> mapper)
	{
		ArraySet<R> copy = (ArraySet<R>) this.copy();
		copy.flatMapImpl((Function) mapper);
		return copy;
	}

	@Override
	public ImmutableSet<E> filtered(Predicate<? super E> condition)
	{
		Object[] newArray = new Object[this.size];
		int index = 0;
		for (int i = 0; i < this.size; i++)
		{
			Object element = this.elements[i];
			if (condition.test((E) element))
			{
				newArray[index++] = element;
			}
		}
		return new ArraySet<>((E[]) newArray, index, true);
	}

	@Override
	public ImmutableSet<E> copy()
	{
		return this.immutableCopy();
	}

	@Override
	public MutableSet<E> mutable()
	{
		return this.mutableCopy();
	}

	@Override
	public java.util.Set<E> toJava()
	{
		return Collections.unmodifiableSet(super.toJava());
	}
}
