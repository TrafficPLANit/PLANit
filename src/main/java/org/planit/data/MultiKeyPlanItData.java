package org.planit.data;

import java.util.logging.Logger;

import org.apache.commons.collections4.IterableMap;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.planit.exceptions.PlanItException;
import org.planit.output.enums.Type;
import org.planit.output.formatter.OutputFormatter;
import org.planit.output.property.BaseOutputProperty;
import org.planit.output.property.OutputProperty;

/**
 * Class which holds arrays of output property values, identified by arrays of output keys
 * 
 * This class is a wrapper for the MultiKeyMap object which is a Map with multiple keys. This class has input and output
 * methods which are specific to PlanIt output properties.
 * 
 * @author gman6028
 *
 */
public class MultiKeyPlanItData {

  private static final Logger LOGGER = Logger.getLogger(MultiKeyPlanItData.class.getCanonicalName());

  private MultiKeyMap<Object, Object[]> multiKeyMap;
  private IterableMap<Object, Object[]> singleKeyMap;
  private OutputProperty[] outputKeyProperties;
  private OutputProperty[] outputValueProperties;
  private Type[] valueTypes;
  private Type[] keyTypes;

  /**
   * Get the position of a property type in an output property array
   *
   * @param outputProperties the output property array
   * @param outputProperty   the output property
   * @return the position of the output property in the output property array
   * @throws PlanItException thrown if the output property type is not in the output property array
   */
  private int getPositionOfOutputProperty(final OutputProperty[] outputProperties, final OutputProperty outputProperty) throws PlanItException {
    for (int i = 0; i < outputProperties.length; i++) {
      if (outputProperties[i].equals(outputProperty)) {
        return i;
      }
    }
    throw new PlanItException(
        "Tried to locate a property of type " + BaseOutputProperty.convertToBaseOutputProperty(outputProperty).getName() + " which has not been registered in MultiKeyPlanItData");
  }

  /**
   * Gets an array of Types from a corresponding array of output properties
   *
   * @param outputProperties array of OutputProperty objects
   * @return array of Types
   * @throws PlanItException thrown if a Type cannot be derived from an OuputProperty object
   */
  private Type[] getTypes(final OutputProperty[] outputProperties) throws PlanItException {
    final Type[] types = new Type[outputProperties.length];
    for (int i = 0; i < outputProperties.length; i++) {
      final BaseOutputProperty property = BaseOutputProperty.convertToBaseOutputProperty(outputProperties[i].value());
      types[i] = property.getType();
    }
    return types;
  }

  /**
   * Validate type of a key object
   *
   * @param key  the key object
   * @param type the required type of the key object
   * @return true if the key is valid, false otherwise
   */
  private boolean isValueTypeCorrect(final Object key, final Type type) {
    switch (type) {
    case DOUBLE:
      return (key instanceof Double);
    case FLOAT:
      return (key instanceof Float);
    case INTEGER:
      return (key instanceof Integer);
    case LONG:
      return (key instanceof Long);
    case BOOLEAN:
      return (key instanceof Boolean);
    case SRSNAME:
      return (key instanceof String);
    case STRING:
      return (key instanceof String);
    default:
      return false;
    }
  }

  /**
   * Validate an array of key values
   *
   * @param keyValues array of key values
   * @return true if the array is valid, false otherwise
   */
  private boolean isKeyValuesValid(final Object... keyValues) {
    if (keyValues.length != outputKeyProperties.length) {
      LOGGER.warning("incorrect number of key values in call to RevisedMemoryOutputFormatter");
      return false;
    }
    for (int i = 0; i < outputKeyProperties.length; i++) {
      if (!isValueTypeCorrect(keyValues[i], keyTypes[i])) {
        LOGGER.warning("output key in position " + (i + 1) + " is of the wrong type.");
        return false;
      }
    }
    return true;
  }

  /**
   * Sets up the instance of the class (only called by the constructors)
   *
   * @param outputKeyProperties   OutputProperty types of keys
   * @param outputValueProperties OutputProperty types of values
   * @throws PlanItException thrown if there is an error
   */
  private void init(final OutputProperty[] outputKeyProperties, final OutputProperty[] outputValueProperties) throws PlanItException {
    PlanItException.throwIf(outputKeyProperties.length > 5, "Attempted to register too many output property keys.  The maximum number allowed is 5");

    multiKeyMap = new MultiKeyMap<Object, Object[]>();
    singleKeyMap = new HashedMap<Object, Object[]>();

    this.outputKeyProperties = outputKeyProperties;
    keyTypes = getTypes(outputKeyProperties);
    this.outputValueProperties = outputValueProperties;
    valueTypes = getTypes(outputValueProperties);
  }

  /**
   * Constructor
   *
   * @param outputKeyProperties   OutputProperty types of keys
   * @param outputValueProperties OutputProperty types of values
   * @throws PlanItException thrown if there is an error
   */
  public MultiKeyPlanItData(final OutputProperty[] outputKeyProperties, final OutputProperty... outputValueProperties) throws PlanItException {
    init(outputKeyProperties, outputValueProperties);
  }

  /**
   * Constructor
   *
   * @param outputKeyProperty1    first output key property
   * @param outputValueProperties OutputProperty types of values
   * @throws PlanItException thrown if there is an error
   */
  public MultiKeyPlanItData(final OutputProperty outputKeyProperty1, final OutputProperty... outputValueProperties) throws PlanItException {
    final OutputProperty[] outputKeyProperties = new OutputProperty[1];
    outputKeyProperties[0] = outputKeyProperty1;
    init(outputKeyProperties, outputValueProperties);
  }

  /**
   * Constructor
   *
   * @param outputKeyProperty1    first output key property
   * @param outputKeyProperty2    second output key property
   * @param outputValueProperties OutputProperty types of values
   * @throws PlanItException thrown if there is an error
   */
  public MultiKeyPlanItData(final OutputProperty outputKeyProperty1, final OutputProperty outputKeyProperty2, final OutputProperty... outputValueProperties)
      throws PlanItException {
    final OutputProperty[] outputKeyProperties = new OutputProperty[2];
    outputKeyProperties[0] = outputKeyProperty1;
    outputKeyProperties[1] = outputKeyProperty2;
    init(outputKeyProperties, outputValueProperties);
  }

  /**
   * Constructor
   *
   * @param outputKeyProperty1    first output key property
   * @param outputKeyProperty2    second output key property
   * @param outputKeyProperty3    third output key property
   * @param outputValueProperties OutputProperty types of values
   * @throws PlanItException thrown if there is an error
   */
  public MultiKeyPlanItData(final OutputProperty outputKeyProperty1, final OutputProperty outputKeyProperty2, final OutputProperty outputKeyProperty3,
      final OutputProperty... outputValueProperties) throws PlanItException {
    final OutputProperty[] outputKeyProperties = new OutputProperty[3];
    outputKeyProperties[0] = outputKeyProperty1;
    outputKeyProperties[1] = outputKeyProperty2;
    outputKeyProperties[2] = outputKeyProperty3;
    init(outputKeyProperties, outputValueProperties);
  }

  /**
   * Returns an array representing the row of data values defined by the specified array of key values
   *
   * @param keyValues array storing the key values
   * @return array storing the data values
   * @throws PlanItException thrown if the key values array is invalid
   */
  public Object[] getRowValues(final Object... keyValues) throws PlanItException {
    PlanItException.throwIf(keyValues.length != outputKeyProperties.length, "Call to getRowValues() has the wrong number of key values");
    PlanItException.throwIf(!isKeyValuesValid(keyValues), "Call to getRowValues() with one or more keys of the wrong type");

    switch (outputKeyProperties.length) {
    case 1:
      return singleKeyMap.get(keyValues[0]);
    case 2:
      return multiKeyMap.get(keyValues[0], keyValues[1]);
    case 3:
      return multiKeyMap.get(keyValues[0], keyValues[1], keyValues[2]);
    case 4:
      return multiKeyMap.get(keyValues[0], keyValues[1], keyValues[2], keyValues[3]);
    case 5:
      return multiKeyMap.get(keyValues[0], keyValues[1], keyValues[2], keyValues[3], keyValues[4]);
    }

    // this line should never be reached, but required for compilation
    return null;
  }

  /**
   * Get data value for a specified row and column
   *
   * @param outputProperty output property of the required column
   * @param keyValues      array storing the key values
   * @return the value of the specified cell
   * @throws PlanItException thrown if there is an error
   */
  public Object getRowValue(final OutputProperty outputProperty, final Object... keyValues) throws PlanItException {
    final Object[] rowValues = getRowValues(keyValues);
    final int pos = getPositionOfOutputValueProperty(outputProperty);
    return rowValues[pos];
  }

  /**
   * Puts the data values into a specified row
   *
   * @param outputValues array storing the data values
   * @param keyValues    array storing the key values to specify a row
   * @throws PlanItException thrown if there is an error
   */
  public void putRow(final Object[] outputValues, final Object... keyValues) throws PlanItException {
    PlanItException.throwIf(keyValues.length != outputKeyProperties.length, "Wrong number of keys used in call to MultiKeyPlanItData");
    PlanItException.throwIf(outputValues.length != outputValueProperties.length, "Wrong number of property values used in call to MultiKeyPlanItData");

    for (int i = 0; i < outputValueProperties.length; i++) {
      PlanItException.throwIf((!isValueTypeCorrect(outputValues[i], valueTypes[i])) && (!outputValues[i].equals(OutputFormatter.NOT_SPECIFIED)),
          "Property in position " + i + " in setRowValues() is of the wrong type");
    }
    PlanItException.throwIf(!isKeyValuesValid(keyValues), "Call to setRowValues() with one or more keys of the wrong type");

    switch (outputKeyProperties.length) {
    case 1:
      singleKeyMap.put(keyValues[0], outputValues);
      break;
    case 2:
      multiKeyMap.put(keyValues[0], keyValues[1], outputValues);
      break;
    case 3:
      multiKeyMap.put(keyValues[0], keyValues[1], keyValues[2], outputValues);
      break;
    case 4:
      multiKeyMap.put(keyValues[0], keyValues[1], keyValues[2], keyValues[3], outputValues);
      break;
    case 5:
      multiKeyMap.put(keyValues[0], keyValues[1], keyValues[2], keyValues[3], keyValues[4], outputValues);
      break;
    }
  }

  /**
   * Set the data value for an individual cell
   *
   * @param outputProperty output property value specifying the column
   * @param value          data value to be inserted
   * @param keyValues      array of key values specifying the row
   * @throws PlanItException thrown if there is an error
   */
  public void putRowValue(final OutputProperty outputProperty, final Object value, final Object... keyValues) throws PlanItException {
    PlanItException.throwIf(keyValues.length != outputKeyProperties.length, "Wrong number of keys used in call to MultiKeyPlanItData");

    Object[] outputValues = null;
    switch (outputKeyProperties.length) {
    case 1:
      outputValues = singleKeyMap.get(keyValues[0]);
      break;
    case 2:
      outputValues = multiKeyMap.get(keyValues[0], keyValues[1]);
      break;
    case 3:
      outputValues = multiKeyMap.get(keyValues[0], keyValues[1], keyValues[2]);
      break;
    case 4:
      outputValues = multiKeyMap.get(keyValues[0], keyValues[1], keyValues[2], keyValues[3]);
      break;
    case 5:
      outputValues = multiKeyMap.get(keyValues[0], keyValues[1], keyValues[2], keyValues[3], keyValues[4]);
      break;
    }
    if (outputValues == null) {
      outputValues = new Object[outputValueProperties.length];
    }
    final int pos = getPositionOfOutputValueProperty(outputProperty);
    outputValues[pos] = value;
    putRow(outputValues, keyValues);
  }

  /**
   * Returns a MultiKeyPlanItDataIterator for the contents of this map
   * 
   * @return MultiKeyPlanItDataIterator which loops through the keys and values of this map
   */
  public MultiKeyPlanItDataIterator getIterator() {
    boolean isSingleKey = (outputKeyProperties.length == 1);
    return MultiKeyPlanItDataIterator.getInstance(isSingleKey, singleKeyMap, multiKeyMap);
  }

  /**
   * Get the position of a property type in the output values property array
   *
   * @param outputValueProperty the output value property
   * @return the position of the output value property in the output values property array
   * @throws PlanItException thrown if the output property type is not in the output values property array
   */
  public int getPositionOfOutputValueProperty(final OutputProperty outputValueProperty) throws PlanItException {
    return getPositionOfOutputProperty(outputValueProperties, outputValueProperty);
  }

  /**
   * Get the position of a property type in the output keys property array
   *
   * @param outputKeyProperty the output value property
   * @return the position of the output key property in the output keys property array
   * @throws PlanItException thrown if the output property type is not in the output keys property array
   */
  public int getPositionOfOutputKeyProperty(final OutputProperty outputKeyProperty) throws PlanItException {
    return getPositionOfOutputProperty(outputKeyProperties, outputKeyProperty);
  }

}
