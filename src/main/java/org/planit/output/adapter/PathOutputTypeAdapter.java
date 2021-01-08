package org.planit.output.adapter;

import org.planit.od.odpath.ODPathIterator;
import org.planit.od.odpath.ODPathMatrix;
import org.planit.output.enums.PathOutputIdentificationType;
import org.planit.output.property.OutputProperty;
import org.planit.time.TimePeriod;
import org.planit.utils.mode.Mode;

public interface PathOutputTypeAdapter extends OutputTypeAdapter {

  /**
   * Retrieve an OD path matrix object for a specified mode
   * 
   * @param mode the specified mode
   * @return the OD path object
   */
  public ODPathMatrix getODPathMatrix(Mode mode);

  /**
   * Returns the specified output property values for the current cell in the ODPathIterator
   * 
   * @param outputProperty the specified output property
   * @param odPathIterator the iterator through the current ODPath object
   * @param mode the current mode
   * @param timePeriod the current time period
   * @param pathOutputType the type of objects in the path list
   * @return the value of the specified property (or an Exception if an error has occurred)
   */
  public Object getPathOutputPropertyValue(OutputProperty outputProperty, ODPathIterator odPathIterator, Mode mode, TimePeriod timePeriod, PathOutputIdentificationType pathOutputType);
}
