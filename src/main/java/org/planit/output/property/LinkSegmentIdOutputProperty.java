package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class LinkSegmentIdOutputProperty extends BaseOutputProperty {

	public static final String LINK_SEGMENT_ID = "Link Segment Id";
	
	@Override
	public String getName() {
		return LINK_SEGMENT_ID;
	}

	@Override
	public Units getUnits() {
		return Units.NONE;
	}

	@Override
	public Type getType() {
		return Type.INTEGER;
	}

	@Override
	public OutputProperty getOutputProperty() {
		return OutputProperty.LINK_SEGMENT_ID;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.ID_PRIORITY;
	}

}
