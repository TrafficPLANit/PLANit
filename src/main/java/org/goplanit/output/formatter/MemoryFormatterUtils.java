package org.goplanit.output.formatter;

import org.goplanit.output.property.OutputProperty;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utils for memory formatter class
 */
public class MemoryFormatterUtils {

  /**
   * To be used in combination with Memory formatter iterator to construct a pretty string for a given result entry
   *
   * @param keyProperties key information in order of values
   * @param valueProperties value information in order of values
   * @param keyValues key values for entry
   * @param valueValues value values for entry
   * @return pretty string
   */
  public static String toPrettyString(OutputProperty[] keyProperties, OutputProperty[] valueProperties, Object[] keyValues, Object[] valueValues) {
    var sb = new StringBuilder();
    //keys
    sb.append(IntStream.range(0,keyProperties.length).mapToObj(
        i -> String.format("%s: %5s",
            keyProperties[i].getOutputPropertyType().toString(), String.valueOf(keyValues[i]))).collect(Collectors.joining(", ")));

    sb.append(", ");

    // values
    sb.append(IntStream.range(0,valueProperties.length).mapToObj(
        i -> String.format("%s: %5s",
            valueProperties[i].getOutputPropertyType().toString(), String.valueOf(valueValues[i]))).collect(Collectors.joining(", ")));
    return sb.toString();
  }
}
