package org.planit.output.adapter;

import java.util.SortedSet;
import java.util.TreeSet;

import org.planit.output.property.BaseOutputProperty;
import org.planit.trafficassignment.TrafficAssignment;

/**
 * Adapter providing properties specific to in LinkOutputTypeConfiguration
 *
 * @author gman6028
 *
 */
public abstract class LinkOutputAdapter extends OutputAdapter {

	/**
	 * Output properties to be included in the CSV output files
	 */
	protected SortedSet<BaseOutputProperty> outputProperties;

	public LinkOutputAdapter(TrafficAssignment trafficAssignment) {
		super(trafficAssignment);
		outputProperties = new TreeSet<BaseOutputProperty>();
	}

	public void addProperty(BaseOutputProperty outputProperty) {
		outputProperties.add(outputProperty);
	}

	public boolean removeProperty(BaseOutputProperty outputProperty) {
		return outputProperties.remove(outputProperty);
	}

	public SortedSet<BaseOutputProperty> getOutputProperties() {
		return outputProperties;
	}

	public void removeAllProperties() {
		outputProperties.clear();
	}

}
