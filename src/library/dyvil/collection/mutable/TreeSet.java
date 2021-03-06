package dyvil.collection.mutable;

import dyvil.annotation.internal.NonNull;
import dyvil.lang.LiteralConvertible;

import java.util.Comparator;

@LiteralConvertible.FromArray
public class TreeSet<E> extends MapBasedSet<E>
{
	private static final long serialVersionUID = 3616255313908232391L;

	@NonNull
	public static <E> TreeSet<E> apply()
	{
		return new TreeSet<>();
	}

	@NonNull
	@SafeVarargs
	public static <E> TreeSet<E> apply(@NonNull E... elements)
	{
		return new TreeSet<>(elements);
	}

	@NonNull
	public static <E> TreeSet<E> from(E @NonNull [] array)
	{
		return new TreeSet<>(array);
	}

	@NonNull
	public static <E> TreeSet<E> from(@NonNull Iterable<? extends E> iterable)
	{
		return new TreeSet<>(iterable);
	}

	public TreeSet()
	{
		super(new TreeMap<>());
	}

	public TreeSet(Comparator<? super E> comparator)
	{
		super(new TreeMap<>(comparator));
	}

	public TreeSet(E @NonNull [] elements)
	{
		this();

		for (E element : elements)
		{
			this.map.put(element, true);
		}
	}

	public TreeSet(@NonNull Iterable<? extends E> iterable)
	{
		this();

		for (E element : iterable)
		{
			this.map.put(element, true);
		}
	}
}
