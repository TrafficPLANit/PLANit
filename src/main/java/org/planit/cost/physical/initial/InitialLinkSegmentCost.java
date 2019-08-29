package org.planit.cost.physical.initial;

import org.planit.network.physical.LinkSegment;
import org.planit.userclass.Mode;
import org.planit.utils.IdGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * Initial Link Segment Costs
 * 
 * @author gman6028
 *
 */
public class InitialLinkSegmentCost extends InitialPhysicalCost {

	/**
	 * Map to store initial cost for each mode and link segment
	 */
	protected Map<Long, Map<Long, Double>> costPerModeAndLinkSegment;
	
	/**
	 * Unique id of the initial link segment cost
	 */
	protected final long id;

	/**
	 * Constructor
	 */
	public InitialLinkSegmentCost() {
		super();
		this.id = IdGenerator.generateId(InitialLinkSegmentCost.class);
		costPerModeAndLinkSegment = new HashMap<Long, Map<Long, Double>>();
	}

	/**
	 * Returns the initial cost for each link segment and mode
	 * 
	 * @param mode        the current mode
	 * @param linkSegment the current link segment
	 * @return the cost for this link segment and mode
	 */
	@Override
	public double getSegmentCost(Mode mode, LinkSegment linkSegment) {
		return costPerModeAndLinkSegment.get(mode.getId()).get(linkSegment.getId());
	}
	
	/**
	 * Sets the initial cost for each link segment and mode
	 * 
	 * @param mode        the current mode
	 * @param linkSegment the current link segment
	 * @param cost        the initial cost for this link segment and mode
	 */
	@Override
	public void setSegmentCost(Mode mode, LinkSegment linkSegment, double cost) {
		
		if (!costPerModeAndLinkSegment.containsKey(mode.getId())) {
			costPerModeAndLinkSegment.put(mode.getId(), new HashMap<Long, Double>());
		}
		costPerModeAndLinkSegment.get(mode.getId()).put(linkSegment.getId(), cost);
	}

	/**
	 * Sets the initial cost for each link segment and mode
	 * 
	 * @param mode        the current mode
	 * @param linkSegmentId the id of the current link segment
	 * @param cost        the initial cost for this link segment and mode
	 * 
	 * At present this method is only used in unit tests.
	 */
	public void setSegmentCost(Mode mode, long linkSegmentId, double cost) {
		
		if (!costPerModeAndLinkSegment.containsKey(mode.getId())) {
			costPerModeAndLinkSegment.put(mode.getId(), new HashMap<Long, Double>());
		}
		costPerModeAndLinkSegment.get(mode.getId()).put(id, cost);
	}

	/**
	 * Collect initial cost id
	 * 
	 * @return id
	 */
	public long getId() {
		return id;
	}

}
