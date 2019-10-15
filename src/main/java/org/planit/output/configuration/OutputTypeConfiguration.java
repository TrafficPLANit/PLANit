package org.planit.output.configuration;

import java.util.Map;
import java.util.function.Function;

import org.planit.exceptions.PlanItException;
import org.planit.output.OutputType;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.property.BaseOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.output.property.OutputPropertyPriority;

/**
 * Configuration for a specific output type including the adapter allowing
 * access to the underlying raw data
 * 
 * @author markr
 *
 */
public abstract class OutputTypeConfiguration {

    /**
     * The output adapter for the output type which provides access the data when
     * needed to the one utilizing this configuration for persistence reasons
     */
    protected final OutputAdapter outputAdapter;
    
    /**
     * The output type being used with the current instance - this must be set in each concrete class which extends OutputTypeConfiguration
     */
    protected OutputType outputType;

   /**
     * OutputTypeconfiguration constructor
     * 
     * @param outputAdapter   to access data for output persistence
     * @param outputType  the output type being created
     */
    public OutputTypeConfiguration(OutputAdapter outputAdapter, OutputType outputType) {
        this.outputAdapter = outputAdapter;
        this.outputType = outputType;
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
		outputAdapter.addProperty(BaseOutputProperty.convertToBaseOutputProperty(propertyClassName));
	}

	/**
	 * Add an output property to be included in the output files
	 * 
	 * @param outputProperty enumeration value specifying which output property to
	 *                       be included in the output files
	 * @throws PlanItException thrown if there is an error
	 */
	public void addProperty(OutputProperty outputProperty) throws PlanItException {
		outputAdapter.addProperty(BaseOutputProperty.convertToBaseOutputProperty(outputProperty));
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
		return outputAdapter.removeProperty(BaseOutputProperty.convertToBaseOutputProperty(propertyClassName));
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
		return outputAdapter.removeProperty(BaseOutputProperty.convertToBaseOutputProperty(outputProperty));
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
		outputAdapter.removeAllProperties();
	}

	/**
	 * Returns the OutputAdapter being used for this configuration
	 * 
	 * @return the OutputAdapter being used
	 */
	public OutputAdapter getOutputAdapter() {
		return outputAdapter;
	}

	/**
	 * Returns an array of output properties for keys used in MemoryOutputFormatter
	 * 
	 * The output array can only contain output properties which are of ID_PRIORITY
	 * 
	 * @return array of output key properties used in the LinkOutputAdapter
	 */
	public OutputProperty[] getOutputKeyProperties() {
		return getOutputProperties(baseOutputProperty -> {
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
		return getOutputProperties(baseOutputProperty -> {
			return !baseOutputProperty.getColumnPriority().equals(OutputPropertyPriority.ID_PRIORITY);
		});
	}

	/**
	 * Filters output properties in the OutputAdapter and outputs them as an
	 * array
	 * 
	 * @param test lambda function to filter which output properties should be
	 *             included
	 * @return array containing the relevant OutputProperty objects
	 */
	public OutputProperty[] getOutputProperties(Function<BaseOutputProperty, Boolean> test) {
		OutputProperty[] outputProperties = outputAdapter.getOutputProperties()
				                                                                                  .stream()
				                                                                                  .filter(baseOutputProperty -> test.apply(baseOutputProperty))
				                                                                                  .map(BaseOutputProperty::getOutputProperty)
				                                                                                  .toArray(OutputProperty[]::new);
		return outputProperties;
	}

	public abstract int findIdentificationMethod(Map<OutputType, OutputProperty[]> outputKeyProperties);
}
