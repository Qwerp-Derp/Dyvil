package dyvil.collection;

import dyvil.tuple.Tuple;

import java.io.Serializable;
import java.util.Objects;

public interface Cell<R, C, V> extends Serializable
{
	/**
	 * Returns the row stored by this cell
	 *
	 * @return the row
	 */
	R getRow();
	
	/**
	 * Returns the column stored by this cell
	 *
	 * @return the column
	 */
	C getColumn();
	
	/**
	 * Returns the value stored by this cell
	 *
	 * @return the value
	 */
	V getValue();
	
	/**
	 * Converts this entry to a {@link Tuple.Of3 Tuple}.
	 *
	 * @return a tuple with this cell's row, column and value
	 */
	default Tuple.Of3<R, C, V> toTuple()
	{
		return new Tuple.Of3<R, C, V>(this.getRow(), this.getColumn(), this.getValue());
	}
	
	static String cellToString(Cell<?, ?, ?> cell)
	{
		return "(" + cell.getRow() + ", " + cell.getColumn() + ") -> " + cell.getValue();
	}
	
	static boolean cellEquals(Cell<?, ?, ?> cell, Object obj)
	{
		if (!(obj instanceof Cell))
		{
			return false;
		}
		
		return cellEquals(cell, (Cell) obj);
	}
	
	static boolean cellEquals(Cell<?, ?, ?> cell1, Cell<?, ?, ?> cell2)
	{
		return Objects.equals(cell1.getRow(), cell2.getRow()) && Objects.equals(cell1.getColumn(), cell2.getColumn())
				&& Objects.equals(cell1.getValue(), cell2.getValue());
	}
	
	static int cellHashCode(Cell<?, ?, ?> cell)
	{
		Object row = cell.getRow();
		Object column = cell.getColumn();
		Object value = cell.getValue();
		int keyHash = (row == null ? 0 : row.hashCode() * 31) + (column == null ? 0 : column.hashCode());
		int hash = keyHash * 31 + (value == null ? 0 : value.hashCode());
		return hash * 31 + hash;
	}
}
