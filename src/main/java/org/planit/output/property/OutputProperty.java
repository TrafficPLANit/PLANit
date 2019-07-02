package org.planit.output.property;

/**
 * Enumeration of possible output properties
 * 
 * @author gman6028
 *
 */
public enum OutputProperty {

	DENSITY("org.planit.output.property.DensityOutputProperty"),
	LINK_ID("org.planit.output.property.LinkIdOutputProperty"),
	MODE_ID("org.planit.output.property.ModeIdOutputProperty"), 
	SPEED("org.planit.output.property.SpeedOutputProperty"),
	FLOW("org.planit.output.property.FlowOutputProperty"), 
	LENGTH("org.planit.output.property.LengthOutputProperty"),
	UPSTREAM_NODE_EXTERNAL_ID("org.planit.output.property.UpstreamNodeExternalIdOutputProperty"),
	DOWNSTREAM_NODE_EXTERNAL_ID("org.planit.output.property.DownstreamNodeExternalIdOutputProperty"),
	TRAVEL_TIME("org.planit.output.property.TravelTimeOutputProperty");
	
	private final String value;

	OutputProperty(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

}
