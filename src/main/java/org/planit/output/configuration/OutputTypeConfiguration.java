package org.planit.output.configuration;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.logging.Logger;

import org.planit.output.enums.OutputType;
import org.planit.output.enums.SubOutputTypeEnum;
import org.planit.output.property.OutputProperty;
import org.planit.output.property.OutputPropertyPriority;
import org.planit.output.property.OutputPropertyType;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.unit.Unit;

/**
 * Configuration for a specific output type including the adapter allowing access to the underlying raw data
 * 
 * @author markr
 *
 */
public abstract class OutputTypeConfiguration {

  /** the logger */
  protected static final Logger LOGGER = Logger.getLogger(OutputTypeConfiguration.class.getCanonicalName());

  /**
   * Filters output properties in the OutputAdapter and outputs them as an array
   * 
   * @param test lambda function to filter which output properties should be included
   * @return array containing the relevant OutputProperty objects
   */
  private OutputProperty[] getOutputPropertyArray(Function<OutputProperty, Boolean> test) {
    OutputProperty[] outputPropertyArray = outputProperties.stream().filter(baseOutputProperty -> test.apply(baseOutputProperty)).toArray(OutputProperty[]::new);
    return outputPropertyArray;
  }

  /**
   * The output type being used with the current instance - this must be set in each concrete class which extends OutputTypeConfiguration
   */
  protected OutputType outputType;

  /**
   * Stores all active sub output types (if any). some output types are broken down further in sub output types which can be accounted for via this set. Can remain empty if not
   * used.
   */
  protected Set<SubOutputTypeEnum> activeSubOutputTypes;

  /**
   * Output properties to be included in the CSV output files
   */
  protected SortedSet<OutputProperty> outputProperties;

  /**
   * Activate a SubOutputTypeEnum for this output type configuration
   * 
   * @param subOutputTypeEnum SubOutputTypeEnum to be activated
   */
  protected void activateSubOutputType(SubOutputTypeEnum subOutputTypeEnum) {
    activeSubOutputTypes.add(subOutputTypeEnum);
  }

  /**
   * Deactivate a SubOutputTypeEnum for this output type configuration
   * 
   * @param subOutputTypeEnum SubOutputTypeEnum to be deactivated
   */
  protected void deactivateSubOutputType(SubOutputTypeEnum subOutputTypeEnum) {
    activeSubOutputTypes.remove(subOutputTypeEnum);
  }

  /**
   * Checks the output property type being added in valid for the current output type configuration
   * 
   * @param baseOutputProperty the output property type being added
   * @return true if the output property is valid, false otherwise
   */
  public abstract boolean isOutputPropertyValid(OutputProperty baseOutputProperty);

  /**
   * Validate whether the specified list of keys is valid, and if it is return only the keys which will be used
   * 
   * @param outputKeyProperties array of output key property types
   * @return array of keys to be used (null if the list is not valid)
   */
  public abstract OutputProperty[] validateAndFilterKeyProperties(OutputProperty[] outputKeyProperties);

  /**
   * OutputTypeconfiguration constructor
   * 
   * @param outputType the output type being created
   * @throws PlanItException thrown if there is an exception
   */
  public OutputTypeConfiguration(OutputType outputType) throws PlanItException {
    this.outputType = outputType;
    outputProperties = new TreeSet<OutputProperty>();
    activeSubOutputTypes = new TreeSet<SubOutputTypeEnum>();
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
   * Indicates if sub output types are present or not
   * 
   * @return true if present, false otherwise
   */
  public boolean hasActiveSubOutputTypes() {
    return !activeSubOutputTypes.isEmpty();
  }

  /**
   * Add an output property to be included in the output files
   * 
   * @param outputProperty enumeration value specifying which output property to be included in the output files
   * @throws PlanItException thrown if there is an error
   */
  public void addProperty(OutputPropertyType outputProperty) throws PlanItException {
    OutputProperty baseOutputProperty = OutputProperty.of(outputProperty);
    if (isOutputPropertyValid(baseOutputProperty)) {
      outputProperties.add(baseOutputProperty);
    }
  }

  /**
   * Remove an output property from the list of properties to be included in the output file
   * 
   * @param propertyClassName class name of the property to be removed
   * @return true if the property is successfully removed, false if it was not in the List of output properties
   * @throws PlanItException thrown if there is an error removing the property
   */
  public boolean removeProperty(String propertyClassName) throws PlanItException {
    return outputProperties.remove(OutputProperty.of(propertyClassName));
  }

  /**
   * Remove an output property from the list of properties to be included in the output file
   * 
   * @param outputProperty enumeration value specifying which output property is to be removed
   * @return true if the property is successfully removed, false if it was not in the List of output properties
   * @throws PlanItException thrown if there is an error removing the property
   */
  public boolean removeProperty(OutputPropertyType outputProperty) throws PlanItException {
    OutputProperty baseOutputProperty = OutputProperty.of(outputProperty);
    if (outputProperties.contains(baseOutputProperty)) {
      return outputProperties.remove(baseOutputProperty);
    }
    return true;
  }

  /**
   * Remove all properties from the current output list
   */
  public void removeAllProperties() {
    outputProperties.clear();
  }

  /**
   * Collect the registered output property by its type
   * 
   * @param outputPropertyType to collect for
   * @return the output property itself, null if not registered
   */
  public OutputProperty getOutputProperty(OutputPropertyType outputPropertyType) {
    return outputProperties.stream().dropWhile(prop -> !prop.getOutputPropertyType().equals(outputPropertyType)).findFirst().orElseGet(null);
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
   * Returns an array of output properties for values used in MemoryOutputFormatter
   * 
   * The output array can only contain output properties which are not of ID_PRIORITY
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
  public SortedSet<OutputProperty> getOutputProperties() {
    return outputProperties;
  }

  /**
   * Returns a set of activated sub output types (if any)
   * 
   * @return Set of activated sub output types (if any)
   */
  public Set<SubOutputTypeEnum> getActiveSubOutputTypes() {
    return activeSubOutputTypes;
  }

  /**
   * Indicate a certain output property is to use a different unit for its result than the default (if permitted)
   * 
   * @param outputPropertyType to alter units for
   * @param overrideUnits      the to be applied units
   */
  public void overrideOutputPropertyUnits(OutputPropertyType outputPropertyType, Unit overrideUnits) {
    OutputProperty outputProperty = getOutputProperty(outputPropertyType);
    if (outputProperty != null) {
      if (!outputProperty.supportsUnitOverride()) {
        LOGGER.warning(String.format("IGNORE: Output property %s does not (yet) support overriding its units", outputProperty.getName()));
        return;
      }
      outputProperty.setUnitOverride(overrideUnits);
    }
  }

}
