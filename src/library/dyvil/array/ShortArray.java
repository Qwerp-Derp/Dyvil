package dyvil.array;

import static dyvil.reflect.Opcodes.*;

import java.util.Arrays;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

import dyvil.annotation.Intrinsic;
import dyvil.annotation.infix;

public interface ShortArray
{
	public static final short[]	EMPTY	= new short[0];
	
	public static short[] apply()
	{
		return EMPTY;
	}
	
	public static short[] apply(int count)
	{
		return new short[count];
	}
	
	public static short[] apply(int count, short repeatedValue)
	{
		short[] array = new short[count];
		for (int i = 0; i < count; i++)
		{
			array[i] = repeatedValue;
		}
		return array;
	}
	
	public static short[] apply(int count, IntUnaryOperator generator)
	{
		short[] array = new short[count];
		for (int i = 0; i < count; i++)
		{
			array[i] = (short) generator.applyAsInt(i);
		}
		return array;
	}
	
	// Basic Array Operations
	
	@Intrinsic({ INSTANCE, ARGUMENTS, ARRAYLENGTH })
	public static @infix int length(short[] array)
	{
		return array.length;
	}
	
	@Intrinsic({ INSTANCE, ARGUMENTS, SALOAD })
	public static @infix short apply(short[] array, int i)
	{
		return array[i];
	}
	
	@Intrinsic({ INSTANCE, ARGUMENTS, SASTORE })
	public static @infix void update(short[] array, int i, short v)
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
	
	public static @infix short[] $plus(short[] array, short v)
	{
		int len = array.length;
		short[] res = new short[len + 1];
		System.arraycopy(array, 0, res, 0, len);
		res[len] = v;
		return res;
	}
	
	public static @infix short[] $plus$plus(short[] array1, short[] array2)
	{
		int len1 = array1.length;
		int len2 = array2.length;
		short[] res = new short[len1 + len2];
		System.arraycopy(array1, 0, res, 0, len1);
		System.arraycopy(array2, 0, res, len1, len2);
		return res;
	}
	
	public static @infix short[] $minus(short[] array, short v)
	{
		int index = indexOf(array, v, 0);
		if (index < 0)
		{
			return array;
		}
		
		int len = array.length;
		short[] res = new short[len - 1];
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
	
	public static @infix short[] $minus$minus(short[] array1, short[] array2)
	{
		int index = 0;
		int len = array1.length;
		short[] res = new short[len];
		
		for (int i = 0; i < len; i++)
		{
			short v = array1[i];
			if (indexOf(array2, v, 0) < 0)
			{
				res[index++] = v;
			}
		}
		
		// Return a resized copy of the temporary array
		return Arrays.copyOf(res, index);
	}
	
	public static @infix short[] $amp(short[] array1, short[] array2)
	{
		int index = 0;
		int len = array1.length;
		short[] res = new short[len];
		
		for (int i = 0; i < len; i++)
		{
			short v = array1[i];
			if (indexOf(array2, v, 0) >= 0)
			{
				res[index++] = v;
			}
		}
		
		// Return a resized copy of the temporary array
		return Arrays.copyOf(res, index);
	}
	
	public static @infix short[] mapped(short[] array, IntUnaryOperator mapper)
	{
		int len = array.length;
		short[] res = new short[len];
		for (int i = 0; i < len; i++)
		{
			res[i] = (short) mapper.applyAsInt(array[i]);
		}
		return res;
	}
	
	public static @infix short[] filtered(short[] array, IntPredicate condition)
	{
		int index = 0;
		int len = array.length;
		short[] res = new short[len];
		for (int i = 0; i < len; i++)
		{
			short v = array[i];
			if (condition.test(v))
			{
				res[index++] = v;
			}
		}
		
		// Return a resized copy of the temporary array
		return Arrays.copyOf(res, index);
	}
	
	public static @infix short[] sorted(short[] array)
	{
		short[] res = array.clone();
		Arrays.sort(res);
		return res;
	}
	
	public static @infix String toString(short[] array)
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
	
	public static @infix int indexOf(short[] array, short v)
	{
		return indexOf(array, v, 0);
	}
	
	public static @infix int indexOf(short[] array, short v, int start)
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
	
	public static @infix int lastIndexOf(short[] array, short v)
	{
		return lastIndexOf(array, v, array.length - 1);
	}
	
	public static @infix int lastIndexOf(short[] array, short v, int start)
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
	
	public static @infix boolean contains(short[] array, short v)
	{
		return indexOf(array, v, 0) != -1;
	}
}