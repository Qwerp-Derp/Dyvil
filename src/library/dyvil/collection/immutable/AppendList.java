package dyvil.collection.immutable;

import dyvil.annotation.Immutable;
import dyvil.collection.*;
import dyvil.collection.iterator.AppendIterator;
import dyvil.collection.iterator.PrependIterator;
import dyvil.collection.mutable.LinkedList;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

@Immutable
public class AppendList<E> implements ImmutableList<E>
{
	public static final class Builder<E> implements ImmutableList.Builder<E>
	{
		private ImmutableList<E> list = (ImmutableList<E>) EmptyList.instance;

		@Override
		public void add(E element)
		{
			if (this.list == null)
			{
				throw new IllegalStateException("Already built");
			}
			this.list = new AppendList<E>(this.list, element);
		}

		@Override
		public ImmutableList<E> build()
		{
			final ImmutableList<E> list = this.list;
			this.list = null;
			return list;
		}
	}

	private static final long serialVersionUID = 2683270385507677394L;
	
	private transient ImmutableList<E> head;
	private transient E                tail;
	
	private transient int size;

	@SafeVarargs
	public static <E> ImmutableList<E> apply(E... elements)
	{
		ImmutableList<E> list = EmptyList.apply();
		for (E element : elements)
		{
			list = new AppendList<>(list, element);
		}
		return list;
	}

	public static <E> ImmutableList<E> from(Iterable<? extends E> iterable)
	{
		ImmutableList<E> list = EmptyList.apply();
		for (E element : iterable)
		{
			list = new AppendList<>(list, element);
		}
		return list;
	}

	public static <E> ImmutableList<E> from(Collection<? extends E> collection)
	{
		return from((Iterable<E>) collection);
	}

	public static <E> Builder<E> builder()
	{
		return new Builder<>();
	}
	
	public AppendList(E element)
	{
		this.head = (ImmutableList<E>) EmptyList.instance;
		this.tail = element;
		this.size = 1;
	}
	
	public AppendList(ImmutableList<E> head, E tail)
	{
		this.head = head;
		this.tail = tail;
		this.size = 1 + head.size();
	}
	
	@Override
	public int size()
	{
		return this.size;
	}
	
	@Override
	public Iterator<E> iterator()
	{
		return new AppendIterator<>(this.head.iterator(), this.tail);
	}
	
	@Override
	public Iterator<E> reverseIterator()
	{
		return new PrependIterator<>(this.tail, this.head.reverseIterator());
	}
	
	@Override
	public E subscript(int index)
	{
		if (index < this.size - 1)
		{
			return this.head.subscript(index);
		}
		return index == this.size - 1 ? this.tail : null;
	}
	
	@Override
	public E get(int index)
	{
		if (index < this.size - 1)
		{
			return this.head.subscript(index);
		}
		if (index == this.size - 1)
		{
			return this.tail;
		}
		throw new IndexOutOfBoundsException(index + " >= " + this.size);
	}
	
	@Override
	public ImmutableList<E> subList(int startIndex, int length)
	{
		if (startIndex + length == this.size - 1)
		{
			return new AppendList<>(this.head.subList(startIndex, length - 1), this.tail);
		}
		return this.head.subList(startIndex, length - 1);
	}
	
	@Override
	public ImmutableList<E> added(E element)
	{
		return new AppendList<>(this, element);
	}
	
	@Override
	public ImmutableList<E> union(Collection<? extends E> collection)
	{
		AppendList<E> ll = this;
		for (E element : collection)
		{
			ll = new AppendList<>(ll, element);
		}
		return ll;
	}
	
	@Override
	public ImmutableList<E> removed(Object element)
	{
		if (Objects.equals(element, this.tail))
		{
			return this.head.removed(element);
		}
		return new AppendList<>(this.head.removed(element), this.tail);
	}
	
	@Override
	public ImmutableList<E> difference(Collection<?> collection)
	{
		if (collection.contains(this.tail))
		{
			return this.head.difference(collection);
		}
		return new AppendList<>((ImmutableList<E>) this.head.difference(collection), this.tail);
	}
	
	@Override
	public ImmutableList<E> intersection(Collection<? extends E> collection)
	{
		if (!collection.contains(this.tail))
		{
			return this.head.intersection(collection);
		}
		return new AppendList<>((ImmutableList<E>) this.head.intersection(collection), this.tail);
	}
	
	@Override
	public <R> ImmutableList<R> mapped(Function<? super E, ? extends R> mapper)
	{
		return new AppendList<>(this.head.mapped(mapper), mapper.apply(this.tail));
	}
	
	@Override
	public <R> ImmutableList<R> flatMapped(Function<? super E, ? extends Iterable<? extends R>> mapper)
	{
		ImmutableList<R> head = this.head.flatMapped(mapper);
		for (R element : mapper.apply(this.tail))
		{
			head = new AppendList<>(head, element);
		}
		return head;
	}
	
	@Override
	public ImmutableList<E> filtered(Predicate<? super E> condition)
	{
		if (!condition.test(this.tail))
		{
			return this.head.filtered(condition);
		}
		return new AppendList<>(this.head.filtered(condition), this.tail);
	}
	
	@Override
	public ImmutableList<E> reversed()
	{
		return new PrependList<>(this.tail, this.head.reversed());
	}
	
	private static <E> ImmutableList<E> fromArray(Object[] array, int length)
	{
		ImmutableList<E> list = (ImmutableList<E>) EmptyList.instance;
		for (int i = 0; i < length; i++)
		{
			list = new AppendList<>(list, (E) array[i]);
		}
		return list;
	}
	
	@Override
	public ImmutableList<E> sorted()
	{
		Object[] array = this.toArray();
		Arrays.sort(array);
		return fromArray(array, this.size);
	}
	
	@Override
	public ImmutableList<E> sorted(Comparator<? super E> comparator)
	{
		Object[] array = this.toArray();
		Arrays.sort((E[]) array, comparator);
		return fromArray(array, this.size);
	}
	
	@Override
	public ImmutableList<E> distinct()
	{
		Object[] array = this.toArray();
		int size = Set.distinct(array, this.size);
		return fromArray(array, size);
	}
	
	@Override
	public ImmutableList<E> distinct(Comparator<? super E> comparator)
	{
		Object[] array = this.toArray();
		int size = Set.sortDistinct((E[]) array, this.size, comparator);
		return fromArray(array, size);
	}
	
	@Override
	public int indexOf(Object element)
	{
		int i = this.head.indexOf(element);
		if (i < 0)
		{
			return Objects.equals(element, this.tail) ? this.size - 1 : -1;
		}
		return i;
	}
	
	@Override
	public int lastIndexOf(Object element)
	{
		if (Objects.equals(element, this.tail))
		{
			return this.size - 1;
		}
		return this.head.lastIndexOf(element);
	}
	
	@Override
	public void toArray(int index, Object[] store)
	{
		this.head.toArray(index, store);
		store[index + this.size - 1] = this.tail;
	}
	
	@Override
	public ImmutableList<E> copy()
	{
		return new AppendList<>(this.head.copy(), this.tail);
	}

	@Override
	public <RE> MutableList<RE> emptyCopy()
	{
		return MutableList.apply();
	}

	@Override
	public <RE> MutableList<RE> emptyCopy(int capacity)
	{
		return MutableList.withCapacity(capacity);
	}
	
	@Override
	public MutableList<E> mutable()
	{
		LinkedList<E> list = new LinkedList<>();
		list.addAll(this.head);
		list.addLast(this.tail);
		return list;
	}

	@Override
	public <RE> ImmutableList.Builder<RE> immutableBuilder()
	{
		return builder();
	}

	@Override
	public <RE> ImmutableList.Builder<RE> immutableBuilder(int capacity)
	{
		return builder();
	}
	
	@Override
	public java.util.List<E> toJava()
	{
		java.util.LinkedList<E> list = new java.util.LinkedList<>();
		for (E element : this.head)
		{
			list.addLast(element);
		}
		list.addLast(this.tail);
		return list;
	}
	
	@Override
	public String toString()
	{
		return Collection.collectionToString(this);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return List.listEquals(this, obj);
	}
	
	@Override
	public int hashCode()
	{
		return List.listHashCode(this);
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException
	{
		out.defaultWriteObject();
		
		out.writeObject(this.head);
		out.writeObject(this.tail);
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		in.defaultReadObject();
		
		this.head = (ImmutableList<E>) in.readObject();
		this.tail = (E) in.readObject();
		this.size = this.head.size() + 1;
	}
}
