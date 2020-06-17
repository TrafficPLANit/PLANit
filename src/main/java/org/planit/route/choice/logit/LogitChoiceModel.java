package org.planit.route.choice.logit;

import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.utils.misc.IdGenerator;

/**
 * The logit choice model base class. Different logit choice models lead to different behaviour
 * regarding
 * choices.
 *
 * @author markr
 *
 */
public abstract class LogitChoiceModel extends TrafficAssignmentComponent<LogitChoiceModel> {

  /** generated UID */
  private static final long serialVersionUID = -4578323513280128464L;

  /**
   * unique identifier
   */
  protected final long id;

  /**
   * Constructor
   */
  protected LogitChoiceModel() {
    super();
    this.id = IdGenerator.generateId(LogitChoiceModel.class);
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public long getId() {
    return this.id;
  }

}
