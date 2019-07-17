package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

/**
 * Template for output property classes which can be included in the output
 * files.
 * 
 * All concrete output property classes must be final and extend this class.
 * 
 * @author gman6028
 *
 */
public abstract class BaseOutputProperty implements Comparable<BaseOutputProperty> {
	
	protected static final int ID_PRIORITY = 0;
	protected static final int INPUT_PRIORITY = 1;
	protected static final int RESULT_PRORITY = 2;
	
	//Constants used as column headings in the output CSV files
	public static final String MODE_EXTERNAL_ID = "Mode External Id";
	public static final String COST = "Cost";
	public static final String LINK_SEGMENT_ID = "Link Segment Id";
	public static final String LINK_SEGMENT_EXTERNAL_ID = "Link Segment External Id";
	public static final String UPSTREAM_NODE_EXTERNAL_ID = "Node Upstream External Id";
	public static final String DOWNSTREAM_NODE_EXTERNAL_ID = "Node Downstream External Id";
	public static final String UPSTREAM_NODE_LOCATION = "Upstream Node Location";
	public static final String DOWNSTREAM_NODE_LOCATION = "Downstream Node Location";
	public static final String CAPACITY_PER_LANE = "Capacity per Lane";
	public static final String DENSITY = "Density";
	public static final String FLOW = "Flow"; 
	public static final String LENGTH = "Length";
	public static final String MODE_ID = "Mode Id";
	public static final String SPEED = "Speed";

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
	 * Sets the column priority of the output property in output files
	 * 
	 * The lower the column priority value of a property, the further to the left it is placed in the output file
	 * 
	 * @return
	 */
	public abstract int getColumnPriority();
	
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
	
	/**
	 * compareTo method used to order the output columns when output is being written
	 * 
	 * @param otherProperty output property which is being compared to the current one
	 */
	public int compareTo(BaseOutputProperty otherProperty) {
		int diff = getColumnPriority()  - otherProperty.getColumnPriority();
		if (!(diff == 0)) {
			return diff;
		}
		return getName().compareTo(otherProperty.getName());
	}
}
