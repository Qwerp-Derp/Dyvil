package dyvil.collection.impl;

import java.lang.reflect.Array;
import java.util.function.Consumer;

import dyvil.lang.Collection;
import dyvil.lang.List;

public abstract class AbstractArrayList<E> implements List<E>
{
	protected static final int	INITIAL_CAPACITY	= 10;
	
	protected Object[]			elements;
	protected int				size;
	
	public AbstractArrayList()
	{
		this.elements = new Object[INITIAL_CAPACITY];
	}
	
	public AbstractArrayList(int size)
	{
		this.elements = new Object[size];
	}
	
	public AbstractArrayList(Object... elements)
	{
		this.elements = elements.clone();
		this.size = elements.length;
	}
	
	public AbstractArrayList(Object[] elements, int size)
	{
		this.elements = new Object[size];
		System.arraycopy(elements, 0, this.elements, 0, size);
		this.size = size;
	}
	
	public AbstractArrayList(Object[] elements, boolean trusted)
	{
		this.elements = elements;
		this.size = elements.length;
	}
	
	public AbstractArrayList(Object[] elements, int size, boolean trusted)
	{
		this.elements = elements;
		this.size = size;
	}
	
	public AbstractArrayList(Collection<E> collection)
	{
		this.size = collection.size();
		this.elements = new Object[this.size];
		
		int index = 0;
		for (E element : collection)
		{
			this.elements[index++] = element;
		}
	}
	
	protected void rangeCheck(int index)
	{
		if (index < 0)
		{
			throw new IndexOutOfBoundsException("List Index out of Bounds: " + index + " < 0");
		}
		if (index >= this.size)
		{
			throw new IndexOutOfBoundsException("List Index out of Bounds: " + index + " >= " + this.size);
		}
	}
	
	@Override
	public int size()
	{
		return this.size;
	}
	
	@Override
	public boolean isEmpty()
	{
		return this.size == 0;
	}
	
	@Override
	public void forEach(Consumer<? super E> action)
	{
		for (int i = 0; i < this.size; i++)
		{
			action.accept((E) this.elements[i]);
		}
	}
	
	@Override
	public boolean contains(Object element)
	{
		if (element == null)
		{
			for (int i = 0; i < this.size; i++)
			{
				if (this.elements[i] == null)
				{
					return true;
				}
			}
			return false;
		}
		
		for (int i = 0; i < this.size; i++)
		{
			if (element.equals(this.elements[i]))
			{
				return true;
			}
		}
		return false;
	}
	
	@Override
	public E apply(int index)
	{
		this.rangeCheck(index);
		return (E) this.elements[index];
	}
	
	@Override
	public E get(int index)
	{
		if (index < 0 || index >= this.size)
		{
			return null;
		}
		return (E) this.elements[index];
	}
	
	@Override
	public int indexOf(Object element)
	{
		if (element == null)
		{
			for (int i = 0; i < this.size; i++)
			{
				if (this.elements[i] == null)
				{
					return i;
				}
			}
			return -1;
		}
		
		for (int i = 0; i < this.size; i++)
		{
			if (element.equals(this.elements[i]))
			{
				return i;
			}
		}
		return -1;
	}
	
	@Override
	public int lastIndexOf(Object element)
	{
		if (element == null)
		{
			for (int i = this.size - 1; i >= 0; i--)
			{
				if (this.elements[i] == null)
				{
					return i;
				}
			}
			return -1;
		}
		
		for (int i = this.size - 1; i >= 0; i--)
		{
			if (element.equals(this.elements[i]))
			{
				return i;
			}
		}
		return -1;
	}
	
	@Override
	public Object[] toArray()
	{
		Object[] array = new Object[this.size];
		System.arraycopy(this.elements, 0, array, 0, this.size);
		return array;
	}
	
	@Override
	public E[] toArray(Class<E> type)
	{
		E[] array = (E[]) Array.newInstance(type, this.size);
		for (int i = 0; i < this.size; i++)
		{
			array[i] = type.cast(this.elements[i]);
		}
		return array;
	}
	
	@Override
	public void toArray(int index, Object[] store)
	{
		System.arraycopy(this.elements, 0, store, index, this.size);
	}
	
	@Override
	public String toString()
	{
		if (this.size == 0)
		{
			return "[]";
		}
		
		StringBuilder buf = new StringBuilder(this.size * 10).append('[');
		buf.append(this.elements[0]);
		for (int i = 1; i < this.size; i++)
		{
			buf.append(", ");
			buf.append(this.elements[i]);
		}
		buf.append(']');
		return buf.toString();
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
}