package org.planit.assignment.ltm.sltm;

import java.util.HashMap;

import org.ojalgo.array.Array2D;
import org.planit.utils.network.layer.physical.Node;

/**
 * POJO to store the sLTM variables used for splitting rate updates (Step 1 and 5) in network loading. Note that because the node model implementation requires turn sending flows
 * rather than link sending flows and splitting rates we store turn sending flows instead of splitting rates. To allow for efficient updating of turn flows we keep track of turn
 * sending flows as a 2D array for each registered node (that is potentially blocking)
 * 
 * @author markr
 *
 */
public class SplittingRateData {

  /**
   * Most recent turn sending flows for all registered nodes
   */
  private final HashMap<Node, Array2D<Double>> turnSendingFlows = new HashMap<Node, Array2D<Double>>();

  /**
   * Constructor
   * 
   * @param emptySegmentArray empty array used to initialize data stores
   */
  public SplittingRateData() {
    super();
  }

}
