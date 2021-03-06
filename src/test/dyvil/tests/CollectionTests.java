package dyvil.tests;

import dyvil.annotation.Immutable;
import dyvil.collection.*;
import dyvil.collection.immutable.EmptyList;
import dyvil.collection.immutable.EmptySet;
import dyvil.collection.immutable.SingletonList;
import dyvil.collection.immutable.SingletonSet;
import org.junit.Test;

import static org.junit.Assert.*;

public class CollectionTests
{
	private static final Object[] SAMPLES = new Object[] { "abc", "def", "Aa", "BB", null, 42, 100000000000000L, 3.5F,
			1.123D };
	
	@Test
	public void testSets()
	{
		this.testSet(EmptySet.apply());
		this.testSet(SingletonSet.apply("abc"));
		this.testSet(this.testSetBuilder(dyvil.collection.immutable.ArraySet.builder()));
		this.testSet(this.testSetBuilder(dyvil.collection.immutable.HashSet.builder()));
		
		this.testSet(dyvil.collection.mutable.ArraySet.apply(SAMPLES));
		this.testSet(dyvil.collection.mutable.HashSet.apply(SAMPLES));
	}
	
	@Test
	public void testLists()
	{
		this.testList(EmptyList.apply());
		this.testList(SingletonList.apply("abc"));
		this.testList(this.testListBuilder(dyvil.collection.immutable.ArrayList.builder()));
		
		this.testList(dyvil.collection.mutable.ArrayList.apply(SAMPLES));
		this.testList(dyvil.collection.mutable.LinkedList.apply(SAMPLES));
	}
	
	@Test
	public void testMaps()
	{

	}
	
	public void testQueryable(Queryable queryable)
	{

	}
	
	public void testBidiQueryable(BidiQueryable queryable)
	{

	}
	
	public void testCollection(Collection collection)
	{
		boolean isImmutable = collection.isImmutable();
		Collection copy = collection.copy();
		Collection mutable = collection.mutable();
		Collection mutableCopy = collection.mutableCopy();
		Collection immutable = collection.immutable();
		Collection immutableCopy = collection.immutableCopy();
		
		assertTrue("Immutable Collection must have a dyvil.annotation.Immutable annotation",
		           isImmutable == collection.getClass().isAnnotationPresent(Immutable.class));

		assertEquals("Collection must be equal to it's copy: " + copy.getClass(), collection, copy);
		assertEquals("Collection must be equal to it's mutable version: " + mutable.getClass(), collection, mutable);
		assertEquals("Collection must be equal to it's mutable copy: " + mutableCopy.getClass(), collection,
		             mutableCopy);
		assertEquals("Collection must be equal to it's immutable version: " + immutable.getClass(), collection,
		             immutable);
		assertEquals("Collection must be equal to it's immutable copy: " + mutableCopy.getClass(), collection,
		             immutableCopy);
		
		assertFalse("Mutable Version must be mutable: " + mutable.getClass(), mutable.isImmutable());
		assertFalse("Mutable Copy must be mutable: " + mutableCopy.getClass(), mutableCopy.isImmutable());
		assertTrue("Immutable Version must be immutable: " + immutable.getClass(), immutable.isImmutable());
		assertTrue("Immutable Copy must be immutable: " + immutableCopy.getClass(), immutableCopy.isImmutable());
		
		if (!isImmutable)
		{
			Collection newCollection = collection.added("newValue");
			collection.add("newValue");
			assertEquals("+ or += does not work correctly: " + collection.getClass(), collection, newCollection);
			collection.remove("newValue");
			assertEquals("- or -= does not work correctly: " + collection.getClass(), copy, collection);
			
			assertEquals(immutable.added("newElement"), collection.added("newElement"));
			assertEquals(immutable.removed("newElement"), collection.removed("newElement"));
		}
		else
		{
			assertEquals(mutable.added("newElement"), collection.added("newElement"));
			assertEquals(immutable.removed("newElement"), collection.removed("newElement"));
		}
	}
	
	public void testSet(Set set)
	{
		this.testQueryable(set);
		this.testCollection(set);
	}
	
	public void testList(List list)
	{
		this.testQueryable(list);
		this.testBidiQueryable(list);
		this.testCollection(list);
	}
	
	public void testMap(Map map)
	{

	}
	
	public ImmutableList testListBuilder(ImmutableList.Builder builder)
	{
		for (Object s : SAMPLES)
		{
			builder.add(s);
		}
		
		ImmutableList list = builder.build();
		assertFalse("Builder list must not be null", list == null);
		this.testBuilderError(builder);
		assertEquals("Builder list has incorrect size", list.size(), SAMPLES.length);
		return list;
	}
	
	public ImmutableSet testSetBuilder(ImmutableSet.Builder builder)
	{
		for (Object s : SAMPLES)
		{
			builder.add(s);
		}
		// Add the elements twice to see if it duplicates
		for (Object s : SAMPLES)
		{
			builder.add(s);
		}
		
		ImmutableSet set = builder.build();
		assertFalse("Builder set must not be null", set == null);
		this.testBuilderError(builder);
		assertEquals("Builder set has incorrect size", set.size(), SAMPLES.length);
		return set;
	}
	
	public ImmutableMap testMapBuilder(ImmutableMap.Builder builder)
	{
		return builder.build();
	}
	
	public void testBuilderError(ImmutableCollection.Builder builder)
	{
		assertTrue("Repeated invocation of builder.build() has to return null: " + builder.getClass(),
		           builder.build() == null);
		
		try
		{
			builder.add(null);
			assertTrue("Builder did not cause exception", false);
		}
		catch (IllegalStateException ex)
		{
			// No problem, expected behavior
		}
	}
}
