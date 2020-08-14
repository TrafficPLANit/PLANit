package org.planit.supply.fundamentaldiagram;

import java.io.Serializable;

import org.planit.assignment.TrafficAssignmentComponent;
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
   * short hand for NEwell fundamental diagram class type
   */
  public static final String NEWELL = NewellFundamentalDiagram.class.getCanonicalName();

  /**
   * Base constructor
   * 
   * @param groupId token
   */
  public FundamentalDiagram(final IdGroupingToken groupId) {
    super(groupId, FundamentalDiagram.class);
  }

}
