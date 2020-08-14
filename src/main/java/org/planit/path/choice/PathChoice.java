package org.planit.path.choice;

import org.planit.assignment.TrafficAssignmentComponent;
import org.planit.utils.id.IdGroupingToken;

/**
 * The path choice traffic assignment component responsible for the configuration of the path choice methodology and the path associated with this procedure.
 *
 * All derived classes must adhere to this protected constructors signature as the factory expects a traffic component create listener only
 *
 * @author markr
 *
 */
public class PathChoice extends TrafficAssignmentComponent<PathChoice> {

  /** generate UID */
  private static final long serialVersionUID = 6220514783786893944L;
  
  /**
   * short for stochastic path choice type
   */
  public static final String STOCHASTIC = StochasticPathChoice.class.getCanonicalName();

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  protected PathChoice(IdGroupingToken groupId) {
    super(groupId, PathChoice.class);
  }

}
