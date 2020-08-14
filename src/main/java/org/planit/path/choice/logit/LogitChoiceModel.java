package org.planit.path.choice.logit;

import org.planit.assignment.TrafficAssignmentComponent;
import org.planit.utils.id.IdGroupingToken;

/**
 * The logit choice model base class. Different logit choice models lead to different behaviour regarding choices.
 *
 * @author markr
 *
 */
public abstract class LogitChoiceModel extends TrafficAssignmentComponent<LogitChoiceModel> {

  /** generated UID */
  private static final long serialVersionUID = -4578323513280128464L;
  
  /**
   * short hand for MNL class type
   */
  public static final String MNL = MultinomialLogit.class.getCanonicalName();

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  protected LogitChoiceModel(IdGroupingToken groupId) {
    super(groupId, LogitChoiceModel.class);
  }

}
