package org.planit.cost.physical.initial;

import org.planit.network.physical.LinkSegment;
import org.planit.output.property.OutputProperty;
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
	protected Map<Long, double[]> costPerModeAndLinkSegment;
	
	/**
	 * Number of link segments in the network
	 */
	protected int noLinkSegments;
	
	/**
	 * Map to store the column index of whichever output property has been used
	 */
	protected Map<OutputProperty, Integer> propertyColumnIndices;

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
		costPerModeAndLinkSegment = new HashMap<Long, double[]>();
		propertyColumnIndices = new HashMap<OutputProperty, Integer>();
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
		double[] costArray = costPerModeAndLinkSegment.get(mode.getId());
		return costArray[(int) linkSegment.getId()];
	}
	
	/**
	 * Return all the link segment costs for a given mode
	 * 
	 * @param mode the specified mode
	 * @return array of initial costs for each link segment
	 */
	public double[] getAllSegmentCostsPerMode(Mode mode) {
		return costPerModeAndLinkSegment.get(mode.getId());
	}
	
	/**
	 * Store the column index for an output property being used
	 *  
	 * @param property the output property
	 * @param col the column index
	 */
	public void setPropertyColumnIndex(OutputProperty property, int col) {
		propertyColumnIndices.put(property, col);
	}
	
	/**
     * Get the column index for a specified output property
	 * 
	 * @param property the specified output property
	 * @return the column index of the property (or -1 if the property is not being used)
	 */
	public int getPropertyColumnIndex(OutputProperty property) {
		if (!propertyColumnIndices.containsKey(property) ) {
			return -1;
		}
		return propertyColumnIndices.get(property);
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
		
		if (!costPerModeAndLinkSegment.keySet().contains(mode.getId())) {
			costPerModeAndLinkSegment.put(mode.getId(), new double[noLinkSegments]);
		}
		costPerModeAndLinkSegment.get(mode.getId())[(int) linkSegment.getId()] = cost;
	}

	/**
	 * Collect initial cost id
	 * 
	 * @return id
	 */
	public long getId() {
		return id;
	}

	/**
	 * Set the number of link segments in the network
	 * 
	 * @param noLinkSegments
	 */
	public void setNoLinkSegments(int noLinkSegments) {
		this.noLinkSegments = noLinkSegments;
	}

}
