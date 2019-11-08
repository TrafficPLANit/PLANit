package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

/**
 * Calculate speed output property class
 * 
 * @author gman6028
 *
 */
public final class CalculatedSpeedOutputProperty extends BaseOutputProperty {

	public static final String CALCULATED_SPEED = "Calculated Speed";
	
	@Override
	public String getName() {
		return CALCULATED_SPEED;
	}

	@Override
	public Units getUnits() {
		return Units.KM_H;
	}

	@Override
	public Type getType() {
		return Type.DOUBLE;
	}

	@Override
	public OutputProperty getOutputProperty() {
		return OutputProperty.CALCULATED_SPEED;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.RESULT_PRIORITY;
	}

}