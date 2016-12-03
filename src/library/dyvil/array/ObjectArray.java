package dyvil.array;

import dyvil.annotation.Immutable;
import dyvil.annotation.Intrinsic;
import dyvil.annotation.Mutating;
import dyvil.annotation.Reified;
import dyvil.annotation.internal.DyvilModifiers;
import dyvil.annotation.internal.DyvilName;
import dyvil.annotation.internal.Primitive;
import dyvil.collection.ImmutableList;
import dyvil.collection.List;
import dyvil.collection.Range;
import dyvil.collection.immutable.ArrayList;
import dyvil.collection.range.Rangeable;
import dyvil.ref.ObjectRef;
import dyvil.ref.array.ObjectArrayRef;
import dyvil.reflect.Modifiers;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.*;

import static dyvil.annotation.Reified.Type.OBJECT_CLASS;
import static dyvil.reflect.Opcodes.*;

public abstract class ObjectArray extends PrimitiveObjectArray
{
	public static final Object[] EMPTY = new Object[0];

	private ObjectArray()
	{
		// no instances
	}

	public static <@Reified(OBJECT_CLASS) T> T[] apply(Class<T> _type)
	{
		return apply(0, _type);
	}

	public static <@Reified(OBJECT_CLASS) T> T[] apply(int size, Class<T> _type)
	{
		return (T[]) Array.newInstance(_type, size);
	}

	public static <@Reified(OBJECT_CLASS) T> T[] apply(int size, T repeatedValue, Class<T> _type)
	{
		final T[] array = apply(size, _type);
		for (int i = 0; i < size; i++)
		{
			array[i] = repeatedValue;
		}
		return array;
	}

	public static <@Reified(OBJECT_CLASS) T> T[] apply(int size, Supplier<T> valueSupplier, Class<T> _type)
	{
		final T[] array = apply(size, _type);
		for (int i = 0; i < size; i++)
		{
			array[i] = valueSupplier.get();
		}
		return array;
	}

	public static <@Reified(OBJECT_CLASS) T> T[] apply(int size, IntFunction<T> valueMapper, Class<T> _type)
	{
		final T[] array = apply(size, _type);
		for (int i = 0; i < size; i++)
		{
			array[i] = valueMapper.apply(i);
		}
		return array;
	}

	@DyvilModifiers(Modifiers.INLINE)
	public static <T> T[] apply(T[] array)
	{
		return array.clone();
	}

	@DyvilModifiers(Modifiers.IMPLICIT)
	public static <@Reified(OBJECT_CLASS) T extends Rangeable<T>> T[] apply(Range<T> range, Class<T> _type)
	{
		return range.toArray(_type);
	}

	@DyvilName("apply")
	public static <@Reified(OBJECT_CLASS) T extends Rangeable<T>> T[] rangeClosed(T from, T to, Class<T> _type)
	{
		int i = 0;
		final T[] array = apply(from.distanceTo(to) + 1, _type);
		for (T current = from; current.compareTo(to) <= 0; current = current.next())
		{
			array[i++] = current;
		}
		return array;
	}

	@DyvilName("apply")
	public static <@Reified(OBJECT_CLASS) T extends Rangeable<T>> T[] range(T from, T toExclusive,
		                                                                                  Class<T> _type)
	{
		int i = 0;
		final T[] array = apply(from.distanceTo(toExclusive), _type);
		for (T current = from; current.compareTo(toExclusive) < 0; current = current.next())
		{
			array[i++] = current;
		}
		return array;
	}

	@Intrinsic( { LOAD_0, ARRAYLENGTH })
	@DyvilModifiers(Modifiers.INFIX)
	public static <T> int length(T[] array)
	{
		return array.length;
	}

	@Intrinsic( { LOAD_0, ARRAYLENGTH })
	@DyvilModifiers(Modifiers.INFIX)
	public static <T> int size(T[] array)
	{
		return array.length;
	}

	@Intrinsic( { LOAD_0, ARRAYLENGTH, EQ0 })
	@DyvilModifiers(Modifiers.INFIX)
	public static <T> boolean isEmpty(T[] array)
	{
		return array.length == 0;
	}

	@Intrinsic( { LOAD_0, LOAD_1, AALOAD })
	@DyvilModifiers(Modifiers.INFIX)
	public static <T> T subscript(T[] array, int index)
	{
		return array[index];
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static <T> T[] subscript(T[] array, Range<@Primitive Integer> range)
	{
		final int size = range.size();
		final T[] result = apply(size, getComponentType(array));
		System.arraycopy(array, range.first(), result, 0, size);
		return result;
	}

	@Intrinsic( { LOAD_0, LOAD_1, LOAD_2, AASTORE })
	@DyvilModifiers(Modifiers.INFIX)
	@Mutating
	public static <T> void subscript_$eq(T[] array, int index, T newValue)
	{
		array[index] = newValue;
	}

	@DyvilModifiers(Modifiers.INFIX)
	@Mutating
	public static <T> void subscript_$eq(T[] array, Range<@Primitive Integer> range, T[] newValues)
	{
		System.arraycopy(newValues, 0, array, range.first(), range.size());
	}

	@DyvilModifiers(Modifiers.INFIX)
	@Mutating
	public static <T> ObjectRef<T> subscript_$amp(T[] array, int index)
	{
		return new ObjectArrayRef<>(array, index);
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static <T> void forEach(T[] array, Consumer<? super T> action)
	{
		for (T v : array)
		{
			action.accept(v);
		}
	}

	// Operators

	@DyvilModifiers(Modifiers.INFIX | Modifiers.INLINE)
	public static <T> boolean $eq$eq(T[] array1, T[] array2)
	{
		return Arrays.equals(array1, array2);
	}

	@DyvilModifiers(Modifiers.INFIX | Modifiers.INLINE)
	public static <T> boolean $bang$eq(T[] array1, T[] array2)
	{
		return !Arrays.equals(array1, array2);
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static <T> T[] added(T[] array, T value)
	{
		final int len = array.length;
		final T[] res = apply(len + 1, getComponentType(array));
		System.arraycopy(array, 0, res, 0, len);
		res[len] = value;
		return res;
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static <T> T[] union(T[] array1, T[] array2)
	{
		final int len1 = array1.length;
		final int len2 = array2.length;
		final T[] res = apply(len1 + len2, getComponentType(array1));
		System.arraycopy(array1, 0, res, 0, len1);
		System.arraycopy(array2, 0, res, len1, len2);
		return res;
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static <T> T[] removed(T[] array, T value)
	{
		int newSize = array.length;

		// Calculate number of element in new array
		for (T element : array)
		{
			if (Objects.equals(element, value))
			{
				newSize--;
			}
		}

		final T[] res = apply(newSize, getComponentType(array));
		int resIndex = 0;

		for (T element : array)
		{
			if (!Objects.equals(element, value))
			{
				res[resIndex++] = element;
			}
		}
		return res;
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static <T> T[] difference(T[] array1, T[] array2)
	{
		int index = 0;
		// We can safely use clone here because no data will be leaked
		final T[] res = array1.clone();

		for (T v : array1)
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
	public static <T> T[] intersection(T[] array1, T[] array2)
	{
		int index = 0;
		// We can safely use clone here because no data will be leaked
		final T[] res = array1.clone();

		for (T v : array1)
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
	public static <T, @Reified(OBJECT_CLASS) U> U[] mapped(T[] array, Function<? super T, ? extends U> mapper, Class<U> _type)
	{
		final int len = array.length;
		final U[] res = apply(len, _type);
		for (int i = 0; i < len; i++)
		{
			res[i] = mapper.apply(array[i]);
		}
		return res;
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static <T, @Reified(OBJECT_CLASS) U> U[] flatMapped(T[] array, Function<? super T, ? extends Iterable<? extends U>> mapper, Class<U> _type)
	{
		final List<U> list = new dyvil.collection.mutable.ArrayList<>(array.length << 2);

		for (T value : array)
		{
			list.addAll(mapper.apply(value));
		}

		return list.toArray(_type);
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static <T> T[] flatten(T[][] array)
	{
		int size = 0;
		for (T[] nested : array)
		{
			size += nested.length;
		}

		final T[] res = apply(size, (Class<T>) getComponentType(array).getComponentType());

		int index = 0;
		for (T[] nested : array)
		{
			final int nestedSize = nested.length;
			System.arraycopy(nested, 0, res, index, nestedSize);
			index += nestedSize;
		}

		return res;
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static <T> T[] filtered(T[] array, Predicate<T> condition)
	{
		int index = 0;
		// We can safely use clone here because no data will be leaked
		final T[] res = array.clone();

		for (T v : array)
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
	public static <T> T[] sorted(T[] array)
	{
		final T[] res = array.clone();
		Arrays.sort(res);
		return res;
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static <T> T[] sorted(T[] array, Comparator<? super T> comparator)
	{
		final T[] res = array.clone();
		Arrays.sort(array, comparator);
		return res;
	}

	@DyvilModifiers(Modifiers.INFIX | Modifiers.INLINE)
	public static <T> Class<T> getComponentType(T[] array)
	{
		return (Class<T>) array.getClass().getComponentType();
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static <T> Class<?> getDeepComponentType(T[] array)
	{
		Class<?> ret = array.getClass();
		while (true)
		{
			final Class<?> componentType = ret.getComponentType();
			if (componentType == null)
			{
				return ret;
			}
			ret = componentType;
		}
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static <T> Class<T[]> getArrayType(Class<T> componentType)
	{
		return (Class<T[]>) apply(componentType).getClass();
	}

	// Search Operations

	@DyvilModifiers(Modifiers.INFIX | Modifiers.INLINE)
	public static <T> boolean contains(T[] array, T value)
	{
		return indexOf(array, value, 0) > 0;
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static <T> int indexOf(T[] array, T value)
	{
		return indexOf(array, value, 0);
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static <T> int indexOf(T[] array, T value, int startIndex)
	{
		for (; startIndex < array.length; startIndex++)
		{
			if (Objects.equals(value, array[startIndex]))
			{
				return startIndex;
			}
		}
		return -1;
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static <T> int lastIndexOf(T[] array, T value)
	{
		return lastIndexOf(array, value, array.length - 1);
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static <T> int lastIndexOf(T[] array, T value, int startIndex)
	{
		for (; startIndex >= 0; startIndex--)
		{
			if (Objects.equals(value, array[startIndex]))
			{
				return startIndex;
			}
		}
		return -1;
	}

	// Copying

	@DyvilModifiers(Modifiers.INFIX)
	public static <T> T[] copy(T[] array)
	{
		return array.clone();
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static <T> T[] copy(T[] array, int newLength)
	{
		return copyAs(array, newLength, (Class<T>) array.getClass().getComponentType());
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static <@Reified(OBJECT_CLASS) N, T extends N> N[] copyAs(T[] array, int newSize, Class<N> type)
	{
		final N[] newArray = apply(newSize, type);
		System.arraycopy(array, 0, newArray, 0, newSize);
		return newArray;
	}

	@DyvilModifiers(Modifiers.INFIX | Modifiers.IMPLICIT)
	public static <T> Iterable<T> asIterable(T[] array)
	{
		return asList(array);
	}

	@DyvilModifiers(Modifiers.INFIX | Modifiers.IMPLICIT)
	public static <T> ImmutableList<T> asList(T @Immutable [] array)
	{
		return new ArrayList<>(array, true);
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static <T> ImmutableList<T> toList(T[] array)
	{
		return new ArrayList<>(array);
	}

	// toString, equals and hashCode

	@DyvilModifiers(Modifiers.INFIX | Modifiers.INLINE)
	public static <T> boolean equals(T[] array1, T[] array2)
	{
		return Arrays.equals(array1, array2);
	}

	@DyvilModifiers(Modifiers.INFIX | Modifiers.INLINE)
	public static <T> boolean deepEquals(T[] array1, T[] array2)
	{
		return Arrays.deepEquals(array1, array2);
	}

	@DyvilModifiers(Modifiers.INFIX | Modifiers.INLINE)
	public static <T> int hashCode(T[] array)
	{
		return Arrays.hashCode(array);
	}

	@DyvilModifiers(Modifiers.INFIX | Modifiers.INLINE)
	public static <T> int deepHashCode(T[] array)
	{
		return Arrays.deepHashCode(array);
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static <T> String toString(T[] array)
	{
		if (array == null)
		{
			return "null";
		}

		final int size = array.length;
		if (size <= 0)
		{
			return "[]";
		}

		final StringBuilder builder = new StringBuilder(size * 10);
		append(array, size, builder);
		return builder.toString();
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static void toString(Object[] array, StringBuilder builder)
	{
		if (array == null)
		{
			builder.append("null");
			return;
		}

		final int len = array.length;
		if (len <= 0)
		{
			builder.append("[]");
			return;
		}

		append(array, len, builder);
	}

	private static void append(Object[] array, int size, StringBuilder builder)
	{
		builder.append('[').append(array[0]);
		for (int i = 1; i < size; i++)
		{
			builder.append(", ");
			builder.append(array[i]);
		}
		builder.append(']');
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static String deepToString(Object[] array)
	{
		if (array == null)
		{
			return "null";
		}

		final int size = array.length;
		if (size <= 0)
		{
			return "[]";
		}

		final StringBuilder builder = new StringBuilder(size * 10);
		deepAppend(array, size, builder);
		return builder.toString();
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static void deepToString(Object[] array, StringBuilder builder)
	{
		if (array == null)
		{
			builder.append("null");
			return;
		}

		final int size = array.length;
		if (size <= 0)
		{
			builder.append("[]");
			return;
		}

		deepAppend(array, size, builder);
	}

	private static void deepAppend(Object[] array, int size, StringBuilder builder)
	{
		builder.append('[');
		deepToString(array[0], builder);
		for (int i = 1; i < size; i++)
		{
			builder.append(", ");
			deepToString(array[i], builder);
		}
		builder.append(']');
	}

	@DyvilModifiers(Modifiers.INFIX)
	public static void deepToString(Object object, StringBuilder builder)
	{
		if (object == null)
		{
			builder.append("null");
			return;
		}

		final Class objectClass = object.getClass();
		if (objectClass == boolean[].class)
		{
			BooleanArray.toString((boolean[]) object, builder);
			return;
		}
		if (objectClass == byte[].class)
		{
			ByteArray.toString((byte[]) object, builder);
			return;
		}
		if (objectClass == short[].class)
		{
			ShortArray.toString((short[]) object, builder);
			return;
		}
		if (objectClass == char[].class)
		{
			CharArray.toString((char[]) object, builder);
			return;
		}
		if (objectClass == int[].class)
		{
			IntArray.toString((int[]) object, builder);
			return;
		}
		if (objectClass == long[].class)
		{
			LongArray.toString((long[]) object, builder);
			return;
		}
		if (objectClass == float[].class)
		{
			FloatArray.toString((float[]) object, builder);
			return;
		}
		if (objectClass == double[].class)
		{
			DoubleArray.toString((double[]) object, builder);
			return;
		}
		if (objectClass.isArray())
		{
			deepToString((Object[]) object, builder);
			return;
		}

		builder.append(object.toString());
	}
}
