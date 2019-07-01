package org.planit.output.property;

import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.output.enums.Type;
import org.planit.output.enums.Units;
import org.planit.userclass.Mode;

/**
 * Template for output property classes which can be included in the output
 * files.
 * 
 * All concrete output property classes must be final and override this class.
 * 
 * @author gman6028
 *
 */
public abstract class BaseOutputProperty {

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
	 * Returns the value to be included in the output value for the current property
	 * 
	 * Most concrete implementations of this method only use one or two of the
	 * method parameters, but the parameter list covers every value which any
	 * implementation might need.
	 * 
	 * @param linkSegment the current link segment
	 * @param mode        the current mode
	 * @param id          the id of the current link
	 * @param flow        the flow through the current link
	 * @param travelTime  the calculated travel time for the current link
	 * @return the value to be output
	 */
	public abstract Object getOutputValue(MacroscopicLinkSegment linkSegment, Mode mode, int id, double flow,
			double travelTime);

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
}
