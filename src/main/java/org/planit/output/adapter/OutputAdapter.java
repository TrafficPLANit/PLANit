package org.planit.output.adapter;

import java.util.HashMap;
import java.util.Map;

import org.planit.output.enums.OutputType;
import org.planit.trafficassignment.TrafficAssignment;

/**
 * Output Adapter which stores output type adapters for each Output Manager and defines top-level
 * method which apply to all output types
 * 
 * @author gman6028
 *
 */
public class OutputAdapter {

  /**
   * The traffic assignment this output adapter is drawing from
   */
  private TrafficAssignment trafficAssignment;

  /**
   * Map of OutputTypeAdapter objects
   */
  private Map<OutputType, OutputTypeAdapter> outputTypeAdapters;

  /**
   * Return the name of a Java object class as a short string
   * 
   * @param object the Java object
   * @return the name of the object
   */
  protected String getClassName(Object object) {
    String name = object.getClass().getCanonicalName();
    String[] words = name.split("\\.");
    return words[words.length - 1];
  }

  /**
   * Constructor
   * 
   * @param trafficAssignment TrafficAssignment object which this adapter wraps
   */
  public OutputAdapter(TrafficAssignment trafficAssignment) {
    this.trafficAssignment = trafficAssignment;
    outputTypeAdapters = new HashMap<OutputType, OutputTypeAdapter>();
  }

  /**
   * Return the id of this assignment run
   * 
   * @return id of this assignment run
   */
  public long getRunId() {
    return trafficAssignment.getId();
  }

  /**
   * Returns the name of the assignment class being used
   * 
   * @return the assignment class being used
   */
  public String getAssignmentClassName() {
    return getClassName(trafficAssignment);
  }

  /**
   * Returns the name of the physical cost class
   * 
   * @return the name of the physical cost class
   */
  public String getPhysicalCostClassName() {
    return getClassName(trafficAssignment.getPhysicalCost());
  }

  /**
   * Return the name of the virtual cost class
   * 
   * @return the name of the virtual cost class
   */
  public String getVirtualCostClassName() {
    return getClassName(trafficAssignment.getVirtualCost());
  }
  
  /**
   * Return the name of the smoothing class
   *  
   * @return the name of the smoothing class
   */
  public String getSmoothingClassName() {
    return getClassName(trafficAssignment.getSmoothing());
  }
  
  /**
   * Return the name of the gap function class
   * 
   * @return the name of the gap function class
   */
  public String getGapFunctionClassName() {
    return getClassName(trafficAssignment.getGapFunction());
  }

  /**
   * Return the name of the stopping criterion class
   * 
   * @return the name of the stopping criterion class
   */
  public String getStopCriterionClassName() {
    return getClassName(trafficAssignment.getGapFunction().getStopCriterion());
  }
  
  /**
   * Store an output type adapters for a specified output type
   * 
   * @param outputType the specified output type
   * @param outputTypeAdapter the output type adapter to be stored
   */
  public void registerOutputTypeAdapter(OutputType outputType, OutputTypeAdapter outputTypeAdapter) {
    outputTypeAdapters.put(outputType, outputTypeAdapter);
  }

  /**
   * Deregister an output type adapter for a specified output type
   * 
   * @param outputType the output type whose adapter is to be deregistered
   */
  public void deregisterOutputTypeAdapter(OutputType outputType) {
    outputTypeAdapters.remove(outputType);
  }

  /**
   * Retrieve an output type adapter for a specified output type
   * 
   * @param outputType the specified output type
   * @return the output type adapter for the specified output type
   */
  public OutputTypeAdapter getOutputTypeAdapter(OutputType outputType) {
    return outputTypeAdapters.get(outputType);
  }

}