package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;
import org.planit.userclass.Mode;

public final class ModeExternalIdOutputProperty extends BaseOutputProperty {

	public static final String MODE_EXTERNAL_ID = "Mode External Id";
	
	@Override
	public String getName() {
		return MODE_EXTERNAL_ID;
	}

	@Override
	public Units getUnits() {
		return Units.NONE;
	}

	@Override
	public Type getType() {
		return Type.LONG;
	}

	@Override
	public OutputProperty getOutputProperty() {
		return OutputProperty.MODE_EXTERNAL_ID;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.ID_PRIORITY;
	}

	/**
	 * Returns the external Id of the current mode
	 * 
	 * @param mode current mode
	 * @return the external Id of the current mode
	 */
	public static long getModeExternalId(Mode mode) {
		return mode.getExternalId();
	}

}