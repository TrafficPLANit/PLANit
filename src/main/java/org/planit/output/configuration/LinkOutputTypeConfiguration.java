package org.planit.output.configuration;

import org.planit.output.Column;
import org.planit.output.adapter.LinkOutputAdapter;
//import org.planit.output.adapter.OutputAdapter;

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
     * @param outputAdapter
     *            to access data for output persistence
     */
    public LinkOutputTypeConfiguration(LinkOutputAdapter outputAdapter) {
        super(outputAdapter);
    }

    /**
     * Add a column to be included in the output files
     * 
     * @param column column to be included in the output files
     */
    public void addColumn(Column column) {
    	((LinkOutputAdapter) outputAdapter).addColumn(column);
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

	public void setXmlOutputDirectory(String xmlOutputDirectory) {
		((LinkOutputAdapter) outputAdapter).setXmlOutputDirectory(xmlOutputDirectory);
		
	}

	public void setCsvOutputDirectory(String csvOutputDirectory) {
		((LinkOutputAdapter) outputAdapter).setCsvOutputDirectory(csvOutputDirectory);
	}
    
}
