package org.planit.output.property;

import org.planit.network.physical.Node;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.userclass.Mode;

public class EndNodeIdOutputProperty extends BaseOutputProperty {

	@Override
	public String getName() {
		return "end node id";
	}

	@Override
	public String getUnits() {
		return "none";
	}

	@Override
	public Type getType() {
		return Type.INTEGER;
	}

	@Override
	public Object getOutputValue(MacroscopicLinkSegment linkSegment, Mode mode, int id, double flow,
			double travelTime) {
		Node endNode = (Node) linkSegment.getDownstreamVertex();
		return endNode.getExternalId();
	}

}
