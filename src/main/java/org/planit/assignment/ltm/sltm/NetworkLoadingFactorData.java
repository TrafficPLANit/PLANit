package org.planit.assignment.ltm.sltm;

import java.util.Arrays;

/**
 * Track the sLTM variables representing the various factors used:
 * <ul>
 * <li>flow acceptance factors (alphas)</li>
 * <li>flow capacity factors (betas)</li>
 * <li>storage capacity factors (gammas)</li>
 * </ul>
 * 
 * @author markr
 *
 */
public class NetworkLoadingFactorData extends LinkSegmentData {

  /**
   * Flow acceptance factors for all link segments by internal id
   */
  private double[] currentFlowAcceptanceFactors = null;

  /**
   * Newly computed flow acceptance factors for all link segments by internal id
   */
  private double[] nextFlowAcceptanceFactors = null;

  /**
   * storage capacity factors for all link segments by internal id
   */
  private double[] currentStorageCapacityFactors = null;

  /**
   * Newly computed storage capacity factors for all link segments by internal id
   */
  private double[] nextStorageCapacityFactors = null;

  /**
   * Flow capacity factors for all link segments by internal id
   */
  private double[] currentFlowCapacityFactors = null;

  /**
   * Newly computed flow capacity factors for all link segments by internal id
   */
  private double[] nextFlowCapacityFactors = null;

  /**
   * Constructor
   * 
   * @param emptySegmentArray empty array used to initialize data stores
   */
  public NetworkLoadingFactorData(double[] emptySegmentArray) {
    super(emptySegmentArray);
    resetCurrentFlowAcceptanceFactors();
    resetNextFlowAcceptanceFactors();
    resetCurrentFlowCapacityFactors();
    resetNextFlowCapacityFactors();
    resetCurrentStorageCapacityFactors();
    resetNextStorageCapacityFactors();
  }

  /**
   * Reset the flow acceptance factors for the coming iteration (alphas)
   */
  public void resetNextFlowAcceptanceFactors() {
    nextFlowAcceptanceFactors = this.createEmptyLinkSegmentDoubleArray();
  }

  /**
   * Reset current flow acceptance factors (alphas)
   */
  public void resetCurrentFlowAcceptanceFactors() {
    currentFlowAcceptanceFactors = this.createEmptyLinkSegmentDoubleArray();
  }

  /**
   * Reset the flow capacity factors for the coming iteration (betas)
   */
  public void resetNextFlowCapacityFactors() {
    nextFlowCapacityFactors = this.createEmptyLinkSegmentDoubleArray();
  }

  /**
   * Reset current flow capacity factors (betas)
   */
  public void resetCurrentFlowCapacityFactors() {
    currentFlowCapacityFactors = this.createEmptyLinkSegmentDoubleArray();
  }

  /**
   * Reset the storage capacity factors for the coming iteration (gammas)
   */
  public void resetNextStorageCapacityFactors() {
    nextFlowCapacityFactors = this.createEmptyLinkSegmentDoubleArray();
  }

  /**
   * Reset current storage capacity factors (gammas)
   */
  public void resetCurrentStorageCapacityFactors() {
    currentFlowCapacityFactors = this.createEmptyLinkSegmentDoubleArray();
  }

  /**
   * Initialise all factor arrays with the given value
   * 
   * @param value to use
   */
  public void initialiseAll(double value) {
    Arrays.fill(this.currentFlowAcceptanceFactors, value);
    Arrays.fill(this.currentFlowCapacityFactors, value);
    Arrays.fill(this.currentStorageCapacityFactors, value);
    Arrays.fill(this.nextFlowAcceptanceFactors, value);
    Arrays.fill(this.nextFlowCapacityFactors, value);
    Arrays.fill(this.nextStorageCapacityFactors, value);
  }

  /**
   * Access to the current flow acceptance factors
   * 
   * @return flow acceptance factors
   */
  public double[] getCurrentFlowAcceptanceFactors() {
    return currentFlowAcceptanceFactors;
  }

  /**
   * Access to the current flow capacity factors
   * 
   * @return flow capacity factors
   */
  public double[] getCurrentFlowCapacityFactors() {
    return this.currentFlowCapacityFactors;
  }

  /**
   * Access to the current storage capacity factors
   * 
   * @return storage capacity factors
   */
  public double[] getCurrentStorageCapacityFactors() {
    return this.currentStorageCapacityFactors;
  }

  /**
   * Access to the next storage capacity factors
   * 
   * @return storage capacity factors
   */
  public double[] getNextStorageCapacityFactors() {
    return nextStorageCapacityFactors;
  }

  /**
   * Access to the next flow acceptance factors
   * 
   * @return flow acceptance factors
   */
  public double[] getNextFlowAcceptanceFactors() {
    return this.nextFlowAcceptanceFactors;
  }

  /**
   * Access to the next flow capacity factors
   * 
   * @return flow capacity factors
   */
  public double[] getNextFlowCapacityFactors() {
    return this.nextFlowCapacityFactors;
  }

  /**
   * equate the current storage capacity factors to the next (reference update, no copy)
   */
  public void setNextStorageCapacityFactorsAsCurrent() {
    this.currentStorageCapacityFactors = nextStorageCapacityFactors;
  }

  /**
   * equate the current flow capacity factors to the next (reference update, no copy)
   */
  public void setNextFlowCapacityFactorsAsCurrent() {
    this.currentFlowCapacityFactors = nextFlowCapacityFactors;
  }

}
