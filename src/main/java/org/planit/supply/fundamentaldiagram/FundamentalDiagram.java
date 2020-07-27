package org.planit.supply.fundamentaldiagram;

import java.io.Serializable;

import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.utils.id.IdGroupingToken;

/**
 * Fundamental diagram traffic component
 *
 * @author markr
 *
 */
public abstract class FundamentalDiagram extends TrafficAssignmentComponent<FundamentalDiagram> implements Serializable {

  /** generated UID */
  private static final long serialVersionUID = 5815100111048623093L;

  /**
   * Base constructor
   */
  public FundamentalDiagram(final IdGroupingToken groupId) {
    super(groupId, FundamentalDiagram.class);
  }

}
