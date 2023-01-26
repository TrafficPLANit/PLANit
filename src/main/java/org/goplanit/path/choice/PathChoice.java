package org.goplanit.path.choice;

import java.io.Serializable;

import org.goplanit.component.PlanitComponent;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * The path choice traffic assignment component responsible for the configuration of the path choice methodology and the path associated with this procedure.
 *
 * All derived classes must adhere to this protected constructors signature as the factory expects a traffic component create listener only
 *
 * @author markr
 *
 */
public abstract class PathChoice extends PlanitComponent<PathChoice> implements Serializable {

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

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected PathChoice(final PathChoice other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract PathChoice clone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract PathChoice deepClone();

}
