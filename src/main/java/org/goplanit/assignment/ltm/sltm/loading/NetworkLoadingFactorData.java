package org.goplanit.assignment.ltm.sltm.loading;

import org.goplanit.assignment.ltm.sltm.LinkSegmentData;

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
   * @param numberOfLinkSegments number of link segments to be expected
   */
  public NetworkLoadingFactorData(int numberOfLinkSegments) {
    super(numberOfLinkSegments, 1.0);
    flowAcceptanceFactors = new double[2][numberOfLinkSegments];
    flowCapacityFactors = new double[2][numberOfLinkSegments];
    storageCapacityFactors = new double[2][numberOfLinkSegments];
    reset();
  }

  /**
   * Reset the flow acceptance factors for the coming iteration (alphas)
   */
  public void resetNextFlowAcceptanceFactors() {
    flowAcceptanceFactors[1] = this.createinitialStateLinkSegmentDoubleArray();
  }

  /**
   * Reset current flow acceptance factors (alphas)
   */
  public void resetCurrentFlowAcceptanceFactors() {
    flowAcceptanceFactors[0] = this.createinitialStateLinkSegmentDoubleArray();
  }

  /**
   * Reset the flow capacity factors for the coming iteration (betas)
   */
  public void resetNextFlowCapacityFactors() {
    flowCapacityFactors[1] = this.createinitialStateLinkSegmentDoubleArray();
  }

  /**
   * Reset current flow capacity factors (betas)
   */
  public void resetCurrentFlowCapacityFactors() {
    flowCapacityFactors[0] = this.createinitialStateLinkSegmentDoubleArray();
  }

  /**
   * Reset the storage capacity factors for the coming iteration (gammas)
   */
  public void resetNextStorageCapacityFactors() {
    storageCapacityFactors[1] = this.createinitialStateLinkSegmentDoubleArray();
  }

  /**
   * Reset current storage capacity factors (gammas)
   */
  public void resetCurrentStorageCapacityFactors() {
    storageCapacityFactors[0] = this.createinitialStateLinkSegmentDoubleArray();
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

  /**
   * Reset to initial state
   */
  public void reset() {
    resetCurrentFlowAcceptanceFactors();
    resetNextFlowAcceptanceFactors();
    resetCurrentFlowCapacityFactors();
    resetNextFlowCapacityFactors();
    resetCurrentStorageCapacityFactors();
    resetNextStorageCapacityFactors();
  }

}
