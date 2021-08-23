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
   * Flow acceptance factors (current and next) for all link segments by internal id
   */
  private double[][] flowAcceptanceFactors = null;

  /**
   * storage capacity factors (current and next) for all link segments by internal id
   */
  private double[][] storageCapacityFactors = null;

  /**
   * Flow capacity factors (current and next) for all link segments by internal id
   */
  private double[][] flowCapacityFactors = null;

  /**
   * Constructor
   * 
   * @param emptySegmentArray empty array used to initialize data stores
   */
  public NetworkLoadingFactorData(double[] emptySegmentArray) {
    super(emptySegmentArray);
    flowAcceptanceFactors = new double[2][emptySegmentArray.length];
    resetCurrentFlowAcceptanceFactors();
    resetNextFlowAcceptanceFactors();
    flowCapacityFactors = new double[2][emptySegmentArray.length];
    resetCurrentFlowCapacityFactors();
    resetNextFlowCapacityFactors();
    storageCapacityFactors = new double[2][emptySegmentArray.length];
    resetCurrentStorageCapacityFactors();
    resetNextStorageCapacityFactors();
  }

  /**
   * Reset the flow acceptance factors for the coming iteration (alphas)
   */
  public void resetNextFlowAcceptanceFactors() {
    flowAcceptanceFactors[1] = this.createEmptyLinkSegmentDoubleArray();
  }

  /**
   * Reset current flow acceptance factors (alphas)
   */
  public void resetCurrentFlowAcceptanceFactors() {
    flowAcceptanceFactors[0] = this.createEmptyLinkSegmentDoubleArray();
  }

  /**
   * Reset the flow capacity factors for the coming iteration (betas)
   */
  public void resetNextFlowCapacityFactors() {
    flowCapacityFactors[1] = this.createEmptyLinkSegmentDoubleArray();
  }

  /**
   * Reset current flow capacity factors (betas)
   */
  public void resetCurrentFlowCapacityFactors() {
    flowCapacityFactors[0] = this.createEmptyLinkSegmentDoubleArray();
  }

  /**
   * Reset the storage capacity factors for the coming iteration (gammas)
   */
  public void resetNextStorageCapacityFactors() {
    storageCapacityFactors[1] = this.createEmptyLinkSegmentDoubleArray();
  }

  /**
   * Reset current storage capacity factors (gammas)
   */
  public void resetCurrentStorageCapacityFactors() {
    storageCapacityFactors[0] = this.createEmptyLinkSegmentDoubleArray();
  }

  /**
   * Initialise all factor arrays with the given value
   * 
   * @param value to use
   */
  public void initialiseAll(double value) {
    Arrays.fill(this.flowAcceptanceFactors[0], value);
    Arrays.fill(this.flowCapacityFactors[0], value);
    Arrays.fill(this.storageCapacityFactors[0], value);
    Arrays.fill(this.flowAcceptanceFactors[1], value);
    Arrays.fill(this.flowCapacityFactors[1], value);
    Arrays.fill(this.storageCapacityFactors[1], value);
  }

  /**
   * Access to the current flow acceptance factors
   * 
   * @return flow acceptance factors
   */
  public double[] getCurrentFlowAcceptanceFactors() {
    return flowAcceptanceFactors[0];
  }

  /**
   * Access to the current flow capacity factors
   * 
   * @return flow capacity factors
   */
  public double[] getCurrentFlowCapacityFactors() {
    return this.flowCapacityFactors[0];
  }

  /**
   * Access to the current storage capacity factors
   * 
   * @return storage capacity factors
   */
  public double[] getCurrentStorageCapacityFactors() {
    return this.storageCapacityFactors[0];
  }

  /**
   * Access to the next storage capacity factors
   * 
   * @return storage capacity factors
   */
  public double[] getNextStorageCapacityFactors() {
    return storageCapacityFactors[1];
  }

  /**
   * Access to the next flow acceptance factors
   * 
   * @return flow acceptance factors
   */
  public double[] getNextFlowAcceptanceFactors() {
    return flowAcceptanceFactors[1];
  }

  /**
   * Access to the next flow capacity factors
   * 
   * @return flow capacity factors
   */
  public double[] getNextFlowCapacityFactors() {
    return this.flowCapacityFactors[1];
  }

  /**
   * equate the current storage capacity factors to the next (reference update, no copy)
   */
  public void swapCurrentAndNextStorageCapacityFactors() {
    swap(0, 1, this.storageCapacityFactors);
  }

  /**
   * equate the current flow capacity factors to the next (reference update, no copy)
   */
  public void swapCurrentAndNextFlowCapacityFactors() {
    swap(0, 1, this.flowCapacityFactors);
  }

  /**
   * equate the current flow acceptance factors to the next (reference update, no copy)
   */
  public void swapCurrentAndNextFlowAcceptanceFactors() {
    swap(0, 1, flowAcceptanceFactors);
  }

}
