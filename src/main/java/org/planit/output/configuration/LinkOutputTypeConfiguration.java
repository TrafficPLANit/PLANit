package org.planit.output.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.planit.exceptions.PlanItException;
import org.planit.output.OutputType;
import org.planit.output.adapter.LinkOutputAdapter;
import org.planit.output.property.BaseOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.output.property.OutputPropertyPriority;

/**
 * The configuration for the link output type
 * 
 * @author markr
 *
 */
public class LinkOutputTypeConfiguration extends OutputTypeConfiguration {

	/**
	 * default for exclude capacity per lane
	 */
	private final boolean EXCLUDE_CAPACITY_PER_LANE = true;

	/**
	 * default for exclude number of lanes
	 */
	private final boolean EXCLUDE_NUMBER_OF_LANES = true;

	/**
	 * Default for persisting link id
	 */
	private final boolean EXCLUDE_LINK_ID = false;
	
	/**
	 * LinkOutputAdapter required for Link Output Type Configuration
	 */
	private LinkOutputAdapter linkOutputAdapter;

	/**
	 * choice to exclude number of lanes
	 */
	protected boolean excludeCapacityPerLane = EXCLUDE_CAPACITY_PER_LANE;

	/**
	 * choice to exclude number of lanes
	 */
	protected boolean excludeNumberOfLanes = EXCLUDE_NUMBER_OF_LANES;

	/**
	 * excluding link id from persistence
	 */
	protected boolean excludeLinkId = EXCLUDE_LINK_ID;

	/**
	 * Constructor
	 * 
	 * Define the default output properties here.
	 * 
	 * @param linkOutputAdapter LinkOutputAdapter to access data for output persistence
	 * @throws PlanItException thrown if there is an error adding the default properties
	 */
	public LinkOutputTypeConfiguration(LinkOutputAdapter linkOutputAdapter) throws PlanItException {
		super(linkOutputAdapter);
		this.outputType = OutputType.LINK;
		this.linkOutputAdapter = linkOutputAdapter;
		// add default output properties
		addProperty(OutputProperty.LINK_SEGMENT_ID);
		addProperty(OutputProperty.MODE_EXTERNAL_ID);
		addProperty(OutputProperty.FLOW);
		addProperty(OutputProperty.SPEED);
		addProperty(OutputProperty.COST);
	}

	/**
	 * Add an output property to be included in the output files
	 * 
	 * @param propertyClassName class name of the output property to be included in
	 *                          the output files
	 * @throws PlanItException thrown if there is an error
	 */
	public void addProperty(String propertyClassName) throws PlanItException {
		linkOutputAdapter.addProperty(BaseOutputProperty.convertToBaseOutputProperty(propertyClassName));
	}

	/**
	 * Add an output property to be included in the output files
	 * 
	 * @param outputProperty enumeration value specifying which output property to
	 *                       be included in the output files
	 * @throws PlanItException thrown if there is an error
	 */
	public void addProperty(OutputProperty outputProperty) throws PlanItException {
		linkOutputAdapter.addProperty(BaseOutputProperty.convertToBaseOutputProperty(outputProperty));
	}

	/**
	 * Remove an output property from the list of properties to be included in the
	 * output file
	 * 
	 * @param propertyClassName class name of the property to be removed
	 * @return true if the property is successfully removed, false if it was not in
	 *         the List of output properties
	 * @throws PlanItException thrown if there is an error removing the property
	 */
	public boolean removeProperty(String propertyClassName) throws PlanItException {
		return linkOutputAdapter.removeProperty(BaseOutputProperty.convertToBaseOutputProperty(propertyClassName));
	}

	/**
	 * Remove an output property from the list of properties to be included in the
	 * output file
	 * 
	 * @param outputProperty enumeration value specifying which output property is
	 *                       to be removed
	 * @return true if the property is successfully removed, false if it was not in
	 *         the List of output properties
	 * @throws PlanItException thrown if there is an error removing the property
	 */
	public boolean removeProperty(OutputProperty outputProperty) throws PlanItException {
		return linkOutputAdapter.removeProperty(BaseOutputProperty.convertToBaseOutputProperty(outputProperty));
	}

	/**
	 * Include all available output properties in the output files
	 * 
	 * @throws PlanItException thrown if there is an error setting up the output
	 *                         property list
	 */
	public void addAllProperties() throws PlanItException {
		for (OutputProperty outputProperty : OutputProperty.values()) {
			addProperty(outputProperty);
		}
	}

	/**
	 * Remove all properties from the current output list
	 */
	public void removeAllProperties() {
		linkOutputAdapter.removeAllProperties();
	}

	// getters - setters

	public boolean isExcludeCapacityPerLane() {
		return excludeCapacityPerLane;
	}

	public void setExcludeCapacityPerLane(boolean excludeCapacityPerLane) {
		this.excludeCapacityPerLane = excludeCapacityPerLane;
	}

	public boolean isExcludeNumberOfLanes() {
		return excludeNumberOfLanes;
	}

	public void setExcludeNumberOfLanes(boolean excludeNumberOfLanes) {
		this.excludeNumberOfLanes = excludeNumberOfLanes;
	}

	// getters - setters

	public boolean isExcludeLinkId() {
		return excludeLinkId;
	}

	public void setExcludeLinkId(boolean excludeLinkId) {
		this.excludeLinkId = excludeLinkId;
	}
	
	public OutputProperty[] getOutputKeyProperties() {
		List<OutputProperty> outputKeyPropertyList = new ArrayList<OutputProperty>();
		Set<BaseOutputProperty> baseOutputProperties = linkOutputAdapter.getOutputProperties();
		for (BaseOutputProperty baseOutputProperty : baseOutputProperties) {
			if (baseOutputProperty.getColumnPriority().equals(OutputPropertyPriority.ID_PRIORITY)) {
				OutputProperty outputProperty = baseOutputProperty.getOutputProperty();
				outputKeyPropertyList.add(outputProperty);
			}
		}
		OutputProperty [] outputKeyProperties = new OutputProperty[outputKeyPropertyList.size()];
		for (int i=0; i<outputKeyProperties.length; i++) {
			outputKeyProperties[i] = outputKeyPropertyList.get(i);
		}		
		return outputKeyProperties;
	}
	
	public OutputProperty[] getOutputValueProperties() {
		List<OutputProperty> outputValuePropertyList = new ArrayList<OutputProperty>();
		Set<BaseOutputProperty> baseOutputProperties = linkOutputAdapter.getOutputProperties();
		for (BaseOutputProperty baseOutputProperty : baseOutputProperties) {
			if (!baseOutputProperty.getColumnPriority().equals(OutputPropertyPriority.ID_PRIORITY)) {
				OutputProperty outputProperty = baseOutputProperty.getOutputProperty();
				outputValuePropertyList.add(outputProperty);
			}
		}
		OutputProperty [] outputValueProperties = new OutputProperty[outputValuePropertyList.size()];
		for (int i=0; i<outputValueProperties.length; i++) {
			outputValueProperties[i] = outputValuePropertyList.get(i);
		}		
		return outputValueProperties;
	}

}
