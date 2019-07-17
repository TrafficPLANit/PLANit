package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public class LinkSegmentExternalIdOutputProperty extends BaseOutputProperty {

	@Override
	public String getName() {
		return LINK_SEGMENT_EXTERNAL_ID;
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
		return OutputProperty.LINK_SEGMENT_EXTERNAL_ID;
	}

	@Override
	public int getColumnPriority() {
		return ID_PRIORITY;
	}

}
