package org.planit.output.property;

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
	SPEED("org.planit.output.property.SpeedOutputProperty"),
	FLOW("org.planit.output.property.FlowOutputProperty"), 
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

	public String value() {
		return value;
	}

}
