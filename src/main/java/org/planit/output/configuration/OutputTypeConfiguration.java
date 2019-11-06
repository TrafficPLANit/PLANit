package org.planit.output.configuration;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

import org.planit.exceptions.PlanItException;
import org.planit.output.enums.OutputType;
import org.planit.output.property.BaseOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.output.property.OutputPropertyPriority;
import org.planit.trafficassignment.TrafficAssignment;

/**
 * Configuration for a specific output type including the adapter allowing
 * access to the underlying raw data
 * 
 * @author markr
 *
 */
public abstract class OutputTypeConfiguration {

    /**
     * The output type being used with the current instance - this must be set in each concrete class which extends OutputTypeConfiguration
     */
    protected OutputType outputType;

    /**
	 * Output properties to be included in the CSV output files
	 */
	protected SortedSet<BaseOutputProperty> outputProperties;

	/**
	 * Filters output properties in the OutputAdapter and outputs them as an array
	 * 
	 * @param test lambda function to filter which output properties should be included
	 * @return array containing the relevant OutputProperty objects
	 */
	private OutputProperty[] getOutputPropertyArray(Function<BaseOutputProperty, Boolean> test) {
		OutputProperty[] outputPropertyArray = outputProperties.stream()
				                                                                                     .filter(baseOutputProperty -> test.apply(baseOutputProperty))
				                                                                                     .map(BaseOutputProperty::getOutputProperty)
				                                                                                     .toArray(OutputProperty[]::new);
		return outputPropertyArray;
	}

/**
 * OutputTypeconfiguration constructor
 * 
 * @param trafficAssignent TrafficAssignment object whose results are being reported
 * @param outputType  the output type being created
 * @throws PlanItException 
 */
 	public OutputTypeConfiguration(TrafficAssignment trafficAssignment, OutputType outputType) throws PlanItException {
        this.outputType = outputType;
        outputProperties = new TreeSet<BaseOutputProperty>();
    }

 	/**
 	 * Returns the OutputAdapter being used for this configuration
 	 * 
 	 * @return the OutputAdapter being used
 	 */
	public OutputType getOutputType() {
		return outputType;
	}

	/**
	 * Add an output property to be included in the output files
	 * 
	 * @param propertyClassName class name of the output property to be included in
	 *                          the output files
	 * @throws PlanItException thrown if there is an error
	 */
	public void addProperty(String propertyClassName) throws PlanItException {
		outputProperties.add(BaseOutputProperty.convertToBaseOutputProperty(propertyClassName));
	}

	/**
	 * Add an output property to be included in the output files
	 * 
	 * @param outputProperty enumeration value specifying which output property to
	 *                       be included in the output files
	 * @throws PlanItException thrown if there is an error
	 */
	public void addProperty(OutputProperty outputProperty) throws PlanItException {
		outputProperties.add(BaseOutputProperty.convertToBaseOutputProperty(outputProperty));
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
		return outputProperties.remove(BaseOutputProperty.convertToBaseOutputProperty(propertyClassName));
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
		BaseOutputProperty baseOutputProperty = BaseOutputProperty.convertToBaseOutputProperty(outputProperty);
		if (outputProperties.contains( baseOutputProperty)) {
			return outputProperties.remove(baseOutputProperty);
		}
		return true;
	}

	/**
	 * Include all available output properties in the output files
	 * 
	 * @throws PlanItException thrown if there is an error setting up the output property list
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
		outputProperties.clear();
	}

	/**
	 * Returns an array of output properties for keys used in MemoryOutputFormatter
	 * 
	 * The output array can only contain output properties which are of ID_PRIORITY
	 * 
	 * @return array of output key properties used in the LinkOutputAdapter
	 */
	public OutputProperty[] getOutputKeyProperties() {
		return getOutputPropertyArray(baseOutputProperty -> {
			return baseOutputProperty.getColumnPriority().equals(OutputPropertyPriority.ID_PRIORITY);
		});
	}

	/**
	 * Returns an array of output properties for values used in
	 * MemoryOutputFormatter
	 * 
	 * The output array can only contain output properties which are not of
	 * ID_PRIORITY
	 * 
	 * @return array of output value properties used in the LinkOutputAdapter
	 */
	public OutputProperty[] getOutputValueProperties() {
		return getOutputPropertyArray(baseOutputProperty -> {
			return !baseOutputProperty.getColumnPriority().equals(OutputPropertyPriority.ID_PRIORITY);
		});
	}
	
	/**
	 * Returns the current set of output properties for this output configuration
	 * 
	 * @return the current set of output properties for this output configuration
	 */
	public SortedSet<BaseOutputProperty> getOutputProperties() {
		return outputProperties;
	}

	/**
	 * Find the identification type for this output configuration
	 * 
	 * @param outputKeyProperties array of output key property types
	 * @return the value of the identification type determined
	 */
	//public abstract int findIdentificationMethod(OutputProperty[] outputKeyPropertiesArray);
	
	/**
	 * Validate whether the specified list of keys is valid, and if it is return only the keys which will be used
	 * 
	 * @param identificationMethod the identification method being used
	 * @return array of keys to be used (null if the list is not valid)
	 */
	//public abstract OutputProperty[] validateAndFilterKeyProperties(int identificationMethod);
	public abstract OutputProperty[] validateAndFilterKeyProperties(OutputProperty[] outputKeyProperties);
}
