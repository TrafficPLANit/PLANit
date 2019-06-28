package org.planit.output.property;

import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.userclass.Mode;

public abstract class BaseOutputProperty {
	// name
	// unit --> enums
	// type-->enums

	public abstract String getName();

	// TODO - Units is a String for now. Later create an <xs:simpleType
	// name="units"> in the linkmetadata.xsd file which
	// restricts possible values for units, and create an enumeration in this
	// package to match it. Like the
	// "Type" in this package matches the generated enumeration
	// "Datatypedescription"
	public abstract String getUnits();

	public abstract Type getType();

	public abstract Object getOutputValue(MacroscopicLinkSegment linkSegment, Mode mode, int id, double flow,
			double travelTime);
	
	public boolean equals(Object otherProperty) {
		return this.getClass().getCanonicalName().equals(otherProperty.getClass().getCanonicalName());
	}

	public int hashCode() {
		return getUnits().hashCode() + getType().hashCode() + getName().hashCode();
	}
}
