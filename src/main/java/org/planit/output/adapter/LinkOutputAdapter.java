package org.planit.output.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.planit.output.property.BaseOutputProperty;
import org.planit.trafficassignment.TrafficAssignment;

/**
 * Adapter providing properties specific to in LinkOutputTypeConfiguration
 *
 * @author gman6028
 *
 */
public abstract class LinkOutputAdapter extends OutputAdapter {

	private static final Logger LOGGER = Logger.getLogger(LinkOutputAdapter.class.getName());

	/**
	 * Output properties to be included in the CSV output files
	 */
	protected List<BaseOutputProperty> outputProperties;

	public LinkOutputAdapter(TrafficAssignment trafficAssignment) {
		super(trafficAssignment);
		outputProperties = new ArrayList<BaseOutputProperty>();
	}

	public void addProperty(BaseOutputProperty outputProperty) {
		outputProperties.add(outputProperty);
	}

	public boolean removeProperty(BaseOutputProperty outputProperty) {
		return outputProperties.remove(outputProperty);
	}

	public List<BaseOutputProperty> getOutputProperties() {
		return outputProperties;
	}

	public void removeAllProperties() {
		outputProperties.clear();
	}

}
