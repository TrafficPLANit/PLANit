package org.planit.output.adapter;

import org.planit.od.odmatrix.ODMatrixIterator;
import org.planit.od.odmatrix.skim.ODSkimMatrix;
import org.planit.output.enums.ODSkimSubOutputType;
import org.planit.output.property.OutputProperty;
import org.planit.utils.time.TimePeriod;
import org.planit.utils.mode.Mode;

/**
 * Interface defining the methods required for an Origin-Destination output adapter
 * 
 * @author gman6028
 *
 */
public interface ODOutputTypeAdapter extends OutputTypeAdapter {

  /**
   * Retrieve an OD skim matrix for a specified OD skim output type and mode
   * 
   * @param odSkimOutputType the specified OD skim output type
   * @param mode the specified mode
   * @return the OD skim matrix
   */
  public ODSkimMatrix getODSkimMatrix(ODSkimSubOutputType odSkimOutputType, Mode mode);

  /**
   * Returns the specified output property values for the current cell in the OD Matrix Iterator
   * 
   * @param outputProperty the specified output property
   * @param odMatrixIterator the iterator through the current OD Matrix
   * @param mode the current mode
   * @param timePeriod the current time period
   * @param timeUnitMultiplier the multiplier for time units
   * @return the value of the specified property (or an Exception if an error has occurred)
   */
  public Object getODOutputPropertyValue(OutputProperty outputProperty, ODMatrixIterator odMatrixIterator, Mode mode,
      TimePeriod timePeriod, double timeUnitMultiplier);

}
