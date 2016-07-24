package dyvil.array;

import dyvil.annotation.Intrinsic;
import dyvil.annotation.Mutating;
import dyvil.annotation._internal.DyvilModifiers;
import dyvil.collection.Range;
import dyvil.collection.immutable.ArrayList;
import dyvil.ref.BooleanRef;
import dyvil.ref.array.BooleanArrayRef;
import dyvil.reflect.Modifiers;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import static dyvil.reflect.Opcodes.*;

public interface BooleanArray
{
	boolean[] EMPTY = new boolean[0];

	@DyvilModifiers(Modifiers.INLINE)
	static boolean[] empty()
	{
		return EMPTY;
	}

	@DyvilModifiers(Modifiers.INLINE)
	static boolean[] empty(int size)
	{
		return new boolean[size];
	}

	@DyvilModifiers(Modifiers.INLINE)
	static boolean[] apply()
	{
		return new boolean[0];
	}

	@DyvilModifiers(Modifiers.INLINE)
	static boolean[] apply(boolean... values)
	{
		return values;
	}

	@DyvilModifiers(Modifiers.INLINE)
	static boolean[] from(boolean[] array)
	{
		return array.clone();
	}

	static boolean[] repeat(int count, boolean repeatedValue)
	{
		boolean[] array = new boolean[count];
		for (int i = 0; i < count; i++)
		{
			array[i] = repeatedValue;
		}
		return array;
	}
	
	static boolean[] generate(int count, IntPredicate generator)
	{
		boolean[] array = new boolean[count];
		for (int i = 0; i < count; i++)
		{
			array[i] = generator.test(i);
		}
		return array;
	}
	
	// Basic Array Operations
	
	@Intrinsic( { LOAD_0, ARRAYLENGTH })
	@DyvilModifiers(Modifiers.INFIX)
	static int length(boolean[] array)
	{
		return array.length;
	}
	
	@Intrinsic( { LOAD_0, LOAD_1, BALOAD })
	@DyvilModifiers(Modifiers.INFIX)
	static boolean subscript(boolean[] array, int i)
	{
		return array[i];
	}

	@DyvilModifiers(Modifiers.INFIX)
	static boolean[] subscript(boolean[] array, Range<Integer> range)
	{
		final int start = range.first();
		final int count = range.count();
		final boolean[] slice = new boolean[count];
		System.arraycopy(array, start, slice, 0, count);
		return slice;
	}
	
	@Intrinsic( { LOAD_0, LOAD_1, LOAD_2, BASTORE })
	@DyvilModifiers(Modifiers.INFIX)
	@Mutating
	static void subscript_$eq(boolean[] array, int i, boolean v)
	{
		array[i] = v;
	}

	@DyvilModifiers(Modifiers.INFIX)
	@Mutating
	static void subscript_$eq(boolean[] array, Range<Integer> range, boolean[] values)
	{
		int start = range.first();
		int count = range.count();
		System.arraycopy(values, 0, array, start, count);
	}

	@DyvilModifiers(Modifiers.INFIX)
	@Mutating
	static BooleanRef subscript_$amp(boolean[] array, int index)
	{
		return new BooleanArrayRef(array, index);
	}

	@Intrinsic( { LOAD_0, ARRAYLENGTH, EQ0 })
	@DyvilModifiers(Modifiers.INFIX)
	static boolean isEmpty(boolean[] array)
	{
		return array.length == 0;
	}

	@DyvilModifiers(Modifiers.INFIX)
	static void forEach(boolean[] array, Consumer<Boolean> action)
	{
		for (boolean v : array)
		{
			action.accept(v);
		}
	}

	// Operators

	@DyvilModifiers(Modifiers.INFIX | Modifiers.INLINE)
	static boolean $qmark(boolean[] array, boolean v)
	{
		return indexOf(array, v, 0) >= 0;
	}

	@DyvilModifiers(Modifiers.INFIX | Modifiers.INLINE)
	static boolean $eq$eq(boolean[] array1, boolean[] array2)
	{
		return Arrays.equals(array1, array2);
	}

	@DyvilModifiers(Modifiers.INFIX | Modifiers.INLINE)
	static boolean $bang$eq(boolean[] array1, boolean[] array2)
	{
		return !Arrays.equals(array1, array2);
	}

	@DyvilModifiers(Modifiers.INFIX)
	static boolean[] $plus(boolean[] array, boolean v)
	{
		int len = array.length;
		boolean[] res = new boolean[len + 1];
		System.arraycopy(array, 0, res, 0, len);
		res[len] = v;
		return res;
	}

	@DyvilModifiers(Modifiers.INFIX)
	static boolean[] $plus$plus(boolean[] array1, boolean[] array2)
	{
		int len1 = array1.length;
		int len2 = array2.length;
		boolean[] res = new boolean[len1 + len2];
		System.arraycopy(array1, 0, res, 0, len1);
		System.arraycopy(array2, 0, res, len1, len2);
		return res;
	}

	@DyvilModifiers(Modifiers.INFIX)
	static boolean[] $minus(boolean[] array, boolean v)
	{
		int index = indexOf(array, v, 0);
		if (index < 0)
		{
			return array;
		}

		int len = array.length;
		boolean[] res = new boolean[len - 1];
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

	@DyvilModifiers(Modifiers.INFIX)
	static boolean[] $minus$minus(boolean[] array1, boolean[] array2)
	{
		int index = 0;
		int len = array1.length;
		boolean[] res = new boolean[len];

		for (boolean v : array1)
		{
			if (indexOf(array2, v, 0) < 0)
			{
				res[index++] = v;
			}
		}

		// Return a resized copy of the temporary array
		return Arrays.copyOf(res, index);
	}

	@DyvilModifiers(Modifiers.INFIX)
	static boolean[] $amp(boolean[] array1, boolean[] array2)
	{
		int index = 0;
		int len = array1.length;
		boolean[] res = new boolean[len];

		for (boolean v : array1)
		{
			if (indexOf(array2, v, 0) >= 0)
			{
				res[index++] = v;
			}
		}

		// Return a resized copy of the temporary array
		return Arrays.copyOf(res, index);
	}

	@DyvilModifiers(Modifiers.INFIX)
	static boolean[] mapped(boolean[] array, Predicate<Boolean> mapper)
	{
		int len = array.length;
		boolean[] res = new boolean[len];
		for (int i = 0; i < len; i++)
		{
			res[i] = mapper.test(array[i]);
		}
		return res;
	}

	@DyvilModifiers(Modifiers.INFIX)
	static boolean[] flatMapped(boolean[] array, Function<Boolean, boolean[]> mapper)
	{
		int size = 0;
		boolean[] res = EMPTY;

		for (boolean v : array)
		{
			boolean[] a = mapper.apply(v);
			int alen = a.length;
			if (size + alen >= res.length)
			{
				boolean[] newRes = new boolean[size + alen];
				System.arraycopy(res, 0, newRes, 0, res.length);
				res = newRes;
			}

			System.arraycopy(a, 0, res, size, alen);
			size += alen;
		}

		return res;
	}

	@DyvilModifiers(Modifiers.INFIX)
	static boolean[] filtered(boolean[] array, Predicate<Boolean> condition)
	{
		int index = 0;
		int len = array.length;
		boolean[] res = new boolean[len];
		for (boolean v : array)
		{
			if (condition.test(v))
			{
				res[index++] = v;
			}
		}

		// Return a resized copy of the temporary array
		return Arrays.copyOf(res, index);
	}

	@DyvilModifiers(Modifiers.INFIX)
	static boolean[] sorted(boolean[] array)
	{
		int len = array.length;
		if (len <= 0)
		{
			return array;
		}

		boolean[] res = new boolean[len];

		// Count the number of 'false' in the array
		int falseEntries = 0;

		for (boolean v : array)
		{
			if (!v)
			{
				falseEntries++;
			}
		}

		// Make the remaining elements of the result true
		for (; falseEntries < len; falseEntries++)
		{
			res[falseEntries] = true;
		}
		return res;
	}

	// Search Operations

	@DyvilModifiers(Modifiers.INFIX)
	static int indexOf(boolean[] array, boolean v)
	{
		return indexOf(array, v, 0);
	}

	@DyvilModifiers(Modifiers.INFIX)
	static int indexOf(boolean[] array, boolean v, int start)
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

	@DyvilModifiers(Modifiers.INFIX)
	static int lastIndexOf(boolean[] array, boolean v)
	{
		return lastIndexOf(array, v, array.length - 1);
	}

	@DyvilModifiers(Modifiers.INFIX)
	static int lastIndexOf(boolean[] array, boolean v, int start)
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

	@DyvilModifiers(Modifiers.INFIX | Modifiers.INLINE)
	static boolean contains(boolean[] array, boolean v)
	{
		return indexOf(array, v, 0) >= 0;
	}

	@DyvilModifiers(Modifiers.INFIX | Modifiers.INLINE)
	static boolean in(boolean v, boolean[] array)
	{
		return indexOf(array, v, 0) >= 0;
	}

	// Copying

	@DyvilModifiers(Modifiers.INFIX | Modifiers.INLINE)
	static boolean[] copy(boolean[] array)
	{
		return array.clone();
	}

	@DyvilModifiers(Modifiers.INFIX)
	static Boolean[] boxed(boolean[] array)
	{
		int len = array.length;
		Boolean[] boxed = new Boolean[len];
		for (int i = 0; i < len; i++)
		{
			boxed[i] = array[i];
		}
		return boxed;
	}

	@DyvilModifiers(Modifiers.INFIX)
	static Iterable<Boolean> toIterable(boolean[] array)
	{
		return new ArrayList<>(boxed(array), true);
	}

	// equals, hashCode and toString

	@DyvilModifiers(Modifiers.INFIX | Modifiers.INLINE)
	static boolean equals(boolean[] array1, boolean[] array2)
	{
		return Arrays.equals(array1, array2);
	}

	@DyvilModifiers(Modifiers.INFIX | Modifiers.INLINE)
	static int hashCode(boolean[] array)
	{
		return Arrays.hashCode(array);
	}

	@DyvilModifiers(Modifiers.INFIX)
	static String toString(boolean[] array)
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

	@DyvilModifiers(Modifiers.INFIX)
	static void toString(boolean[] array, StringBuilder builder)
	{
		if (array == null)
		{
			builder.append("null");
			return;
		}
		
		int len = array.length;
		if (len <= 0)
		{
			builder.append("[]");
			return;
		}
		
		builder.append('[').append(array[0]);
		for (int i = 1; i < len; i++)
		{
			builder.append(", ");
			builder.append(array[i]);
		}
		builder.append(']');
	}
}
