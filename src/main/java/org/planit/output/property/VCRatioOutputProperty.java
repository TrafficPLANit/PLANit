package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

/**
 * Calculate V/C ratio output property class
 * 
 * @author gman6028
 *
 */
public final class VCRatioOutputProperty extends BaseOutputProperty {

	public static final String VC_RATIO = "VC Ratio";
	
	@Override
	public String getName() {
		return VC_RATIO ;
	}

	@Override
	public Units getUnits() {
		return Units.NONE;
	}

	@Override
	public Type getType() {
		return Type.DOUBLE;
	}

	@Override
	public OutputProperty getOutputProperty() {
		return OutputProperty.VC_RATIO;
	}

	@Override
	public OutputPropertyPriority getColumnPriority() {
		return OutputPropertyPriority.RESULT_PRIORITY;
	}

}
