package org.planit.output.adapter;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.planit.exceptions.PlanItException;
import org.planit.output.property.BaseOutputProperty;
import org.planit.output.property.OutputProperty;
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

	public void addProperty(OutputProperty outputProperty) throws PlanItException {
		addProperty(outputProperty.value());
	}

	public void addProperty(String propertyClassName) throws PlanItException {
		try {
			Class<?> entityClass = Class.forName(propertyClassName);
			BaseOutputProperty outputProperty = (BaseOutputProperty) entityClass.getDeclaredConstructor().newInstance();
			outputProperties.add(outputProperty);
		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}

	public void removeProperty(OutputProperty outputProperty) throws PlanItException {
		removeProperty(outputProperty.value());
	}

	public boolean removeProperty(String propertyClassName) throws PlanItException {
		try {
			Class<?> entityClass = Class.forName(propertyClassName);
			BaseOutputProperty outputProperty = (BaseOutputProperty) entityClass.getDeclaredConstructor().newInstance();
			return outputProperties.remove(outputProperty);
		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}

	public List<BaseOutputProperty> getOutputProperties() {
		return outputProperties;
	}

	public void removeAllProperties() {
		outputProperties.clear();
	}
}
