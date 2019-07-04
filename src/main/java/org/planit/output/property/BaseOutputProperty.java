package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

/**
 * Template for output property classes which can be included in the output
 * files.
 * 
 * All concrete output property classes must be final and override this class.
 * 
 * @author gman6028
 *
 */
public abstract class BaseOutputProperty implements Comparable<BaseOutputProperty> {

	/**
	 * Returns the name of the output property
	 * 
	 * @return name of the output property
	 */
	public abstract String getName();

	/**
	 * Returns the units of the output property
	 * 
	 * @return units of the output property
	 */
	public abstract Units getUnits();

	/**
	 * Returns the data type of the output property
	 * 
	 * @return data type of the output property
	 */
	public abstract Type getType();
	
	/**
	 * Return the value of the OutputProperty enumeration for this property
	 * 
	 * @return the value of the OutputProperty enumeration for this property
	 */
	public abstract OutputProperty getOutputProperty();

	/**
	 * Sets the column position of the output property in output files
	 * 
	 * The lower the column position value of a property, the further to the left it is placed in the output file
	 * 
	 * @return
	 */
	public abstract int getColumnPosition();
	/**
	 * Overridden equals() method
	 * 
	 * This method is needed to allow output properties to be removed from the
	 * output list if required.
	 * 
	 * @param otherProperty output property to be compared to this one
	 * 
	 */
	public boolean equals(Object otherProperty) {
		return this.getClass().getCanonicalName().equals(otherProperty.getClass().getCanonicalName());
	}

	/**
	 * Overridden hashCode() method
	 * 
	 * This method is needed to allow output properties to be removed from the
	 * output list if required.
	 * 
	 */
	public int hashCode() {
		return getUnits().hashCode() + getType().hashCode() + getName().hashCode();
	}
	
	public int compareTo(BaseOutputProperty otherProperty) {
		return    getColumnPosition() - otherProperty.getColumnPosition();
	}
}
