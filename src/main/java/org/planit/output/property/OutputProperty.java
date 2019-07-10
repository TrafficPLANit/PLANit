package org.planit.output.property;

import org.planit.exceptions.PlanItException;

/**
 * Enumeration of possible output properties
 * 
 * @author gman6028
 *
 */
public enum OutputProperty {

	DENSITY("org.planit.output.property.DensityOutputProperty"),
	LINK_SEGMENT_ID("org.planit.output.property.LinkSegmentIdOutputProperty"),
	LINK_SEGMENT_EXTERNAL_ID("org.planit.output.property.LinkSegmentExternalIdOutputProperty"),
	MODE_ID("org.planit.output.property.ModeIdOutputProperty"),
	MODE_EXTERNAL_ID("org.planit.output.property.ModeExternalIdOutputProperty"),
	SPEED("org.planit.output.property.SpeedOutputProperty"), FLOW("org.planit.output.property.FlowOutputProperty"),
	LENGTH("org.planit.output.property.LengthOutputProperty"),
	UPSTREAM_NODE_EXTERNAL_ID("org.planit.output.property.UpstreamNodeExternalIdOutputProperty"),
	DOWNSTREAM_NODE_EXTERNAL_ID("org.planit.output.property.DownstreamNodeExternalIdOutputProperty"),
	CAPACITY_PER_LANE("org.planit.output.property.CapacityPerLaneOutputProperty"),
	COST("org.planit.output.property.CostOutputProperty"),
	DOWNSTREAM_NODE_LOCATION("org.planit.output.property.DownstreamNodeLocationOutputProperty"),
	UPSTREAM_NODE_LOCATION("org.planit.output.property.UpstreamNodeLocationOutputProperty");

	private final String value;

	OutputProperty(String v) {
		value = v;
	}

	/**
	 * Return the String value associated with this enumeration value (the fully qualified class name)
	 * 
	 * @return the class name associated with this enumeration value
	 */
	public String value() {
		return value;
	}

	/**
	 * Returns the enumeration value associated with the specified class name
	 * 
	 * @param v the specified class name
	 * @return the enumeration value associated with this class name
	 */
	public static OutputProperty fromValue(String v) {
		for (OutputProperty outputProperty : OutputProperty.values()) {
			if (outputProperty.value.equals(v)) {
				return outputProperty;
			}
		}
		throw new IllegalArgumentException(v);
	}

	/**
	 * Returns the enumeration value associated with a specified header name (the header name in input and output files)
	 * 
	 * @param name the header name
	 * @return the enumeration associated with the specified header name
	 * @throws PlanItException if the name is not associated with any output property
	 */
	public static OutputProperty fromHeaderName(String name) throws PlanItException {
		try {
			for (OutputProperty outputProperty : OutputProperty.values()) {
				Class<?> entityClass = Class.forName(outputProperty.value);
				BaseOutputProperty baseOutputProperty = (BaseOutputProperty) entityClass.getDeclaredConstructor()
						.newInstance();
				if (baseOutputProperty.getName().equals(name)) {
					return outputProperty;
				}
			}
			throw new PlanItException("The header name " + name + " is not associated with any output property.");
		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}

}