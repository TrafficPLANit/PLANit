package org.goplanit.output.enums;

/**
 * Marker interface to allow us to store outputType and suboutputTypeEnums in the same collection
 * without issue while still being able to distinguish between the two
 * 
 * @author markr
 *
 */
public interface OutputTypeEnum {

  public String value();
}
