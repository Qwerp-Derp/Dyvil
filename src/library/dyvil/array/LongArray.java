package dyvil.array;

import static dyvil.reflect.Opcodes.*;

import java.util.Arrays;
import java.util.function.IntConsumer;
import java.util.function.LongPredicate;
import java.util.function.LongUnaryOperator;

import dyvil.annotation.Intrinsic;
import dyvil.annotation.infix;

public interface LongArray
{
	public static final long[]	EMPTY	= new long[0];
	
	public static long[] apply()
	{
		return EMPTY;
	}
	
	public static long[] apply(int count)
	{
		return new long[count];
	}
	
	public static long[] apply(int count, long repeatedValue)
	{
		long[] array = new long[count];
		for (int i = 0; i < count; i++)
		{
			array[i] = repeatedValue;
		}
		return array;
	}
	
	public static long[] apply(int count, LongUnaryOperator generator)
	{
		long[] array = new long[count];
		for (int i = 0; i < count; i++)
		{
			array[i] = generator.applyAsLong(i);
		}
		return array;
	}
	
	// Basic Array Operations
	
	@Intrinsic({ INSTANCE, ARGUMENTS, ARRAYLENGTH })
	public static @infix int length(long[] array)
	{
		return array.length;
	}
	
	@Intrinsic({ INSTANCE, ARGUMENTS, LALOAD })
	public static @infix long apply(long[] array, int i)
	{
		return array[i];
	}
	
	@Intrinsic({ INSTANCE, ARGUMENTS, LASTORE })
	public static @infix void update(long[] array, int i, long v)
	{
		array[i] = v;
	}
	
	@Intrinsic({ INSTANCE, ARGUMENTS, ARRAYLENGTH, IFEQ })
	public static @infix boolean isEmpty(int[] array)
	{
		return array.length == 0;
	}
	
	public static @infix void forEach(int[] array, IntConsumer action)
	{
		int len = array.length;
		for (int i = 0; i < len; i++)
		{
			action.accept(array[i]);
		}
	}
	
	// Operators
	
	public static @infix long[] $plus(long[] array, long v)
	{
		int len = array.length;
		long[] res = new long[len + 1];
		System.arraycopy(array, 0, res, 0, len);
		res[len] = v;
		return res;
	}
	
	public static @infix long[] $plus$plus(long[] array1, long[] array2)
	{
		int len1 = array1.length;
		int len2 = array2.length;
		long[] res = new long[len1 + len2];
		System.arraycopy(array1, 0, res, 0, len1);
		System.arraycopy(array2, 0, res, len1, len2);
		return res;
	}
	
	public static @infix long[] $minus(long[] array, long v)
	{
		int index = indexOf(array, v, 0);
		if (index < 0)
		{
			return array;
		}
		
		int len = array.length;
		long[] res = new long[len - 1];
		if (index > 0)
		{
			// copy the first part before the index
			System.arraycopy(array, 0, res, 0, index);
		}
		if (index < len)
		{
			// copy the second part after the index
			System.arraycopy(array, index + 1, res, index, len - index - 1);
		}
		return res;
	}
	
	public static @infix long[] $minus$minus(long[] array1, long[] array2)
	{
		int index = 0;
		int len = array1.length;
		long[] res = new long[len];
		
		for (int i = 0; i < len; i++)
		{
			long v = array1[i];
			if (indexOf(array2, v, 0) < 0)
			{
				res[index++] = v;
			}
		}
		
		// Return a resized copy of the temporary array
		return Arrays.copyOf(res, index);
	}
	
	public static @infix long[] $amp(long[] array1, long[] array2)
	{
		int index = 0;
		int len = array1.length;
		long[] res = new long[len];
		
		for (int i = 0; i < len; i++)
		{
			long v = array1[i];
			if (indexOf(array2, v, 0) >= 0)
			{
				res[index++] = v;
			}
		}
		
		// Return a resized copy of the temporary array
		return Arrays.copyOf(res, index);
	}
	
	public static @infix long[] mapped(long[] array, LongUnaryOperator mapper)
	{
		int len = array.length;
		long[] res = new long[len];
		for (int i = 0; i < len; i++)
		{
			res[i] = mapper.applyAsLong(array[i]);
		}
		return res;
	}
	
	public static @infix long[] filtered(long[] array, LongPredicate condition)
	{
		int index = 0;
		int len = array.length;
		long[] res = new long[len];
		for (int i = 0; i < len; i++)
		{
			long v = array[i];
			if (condition.test(v))
			{
				res[index++] = v;
			}
		}
		
		// Return a resized copy of the temporary array
		return Arrays.copyOf(res, index);
	}
	
	public static @infix long[] sorted(long[] array)
	{
		long[] res = array.clone();
		Arrays.sort(res);
		return res;
	}
	
	public static @infix String toString(long[] array)
	{
		if (array == null)
		{
			return "null";
		}
		
		int len = array.length;
		if (len <= 0)
		{
			return "[]";
		}
		
		StringBuilder buf = new StringBuilder(len * 3 + 4);
		buf.append('[').append(array[0]);
		for (int i = 1; i < len; i++)
		{
			buf.append(", ");
			buf.append(array[i]);
		}
		return buf.append(']').toString();
	}
	
	// Search Operations
	
	public static @infix int indexOf(long[] array, long v)
	{
		return indexOf(array, v, 0);
	}
	
	public static @infix int indexOf(long[] array, long v, int start)
	{
		for (; start < array.length; start++)
		{
			if (array[start] == v)
			{
				return start;
			}
		}
		return -1;
	}
	
	public static @infix int lastIndexOf(long[] array, long v)
	{
		return lastIndexOf(array, v, array.length - 1);
	}
	
	public static @infix int lastIndexOf(long[] array, long v, int start)
	{
		for (; start >= 0; start--)
		{
			if (array[start] == v)
			{
				return start;
			}
		}
		return -1;
	}
	
	public static @infix boolean contains(long[] array, long v)
	{
		return indexOf(array, v, 0) != -1;
	}
}