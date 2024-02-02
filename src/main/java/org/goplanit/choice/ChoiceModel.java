package org.goplanit.choice;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.goplanit.choice.weibit.Weibit;
import org.goplanit.component.PlanitComponent;
import org.goplanit.choice.logit.MultinomialLogit;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.reflection.ReflectionUtils;

/**
 * The logit choice model base class. Different logit choice models lead to different behaviour regarding choices.
 *
 * @author markr
 *
 */
public abstract class ChoiceModel extends PlanitComponent<ChoiceModel> implements Serializable {

  /** generated UID */
  private static final long serialVersionUID = -4578323513280128464L;

  /** the scaling factor used to scale the utilities/cost */
  private double scalingFactor = DEFAULT_SCALING_FACTOR;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  protected ChoiceModel(IdGroupingToken groupId) {
    super(groupId, ChoiceModel.class);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected ChoiceModel(ChoiceModel other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * apply the choice model and produce the results in raw array form
   *
   * @param alternativeCosts costs of each alternative
   * @return computed probabilities in order of alternative costs provided
   */
  public abstract double[] computeProbabilities(double[] alternativeCosts);

  /**
   * Compute perceived cost for the given choice model based on known absolute cost and the flow that is assumed to be allocated to the alternative
   *
   * @param absoluteCost to use
   * @param demand to use
   * @param applyExpTransform when true apply an exponent transform, otherwise do not
   * @return perceived cost, i.e., the adjustment to the absolute cost to account for the choice models inferred perception error
   */
  public abstract double computePerceivedCost(double absoluteCost, double demand, boolean applyExpTransform);

  /** Compute the derivative of perceived cost towards flow knowing the impact of dAbsoluteCost on a flow change as
   *  well as the absolute cost itself. We support an exp transformation as well to allow for small values of demand.
   *  todo: For now we are assuming here that the derivative of the absolute cost function yields a value rather than a function, which is not general yet
   *
   * @param dAbsoluteCostDFlow derivative of absolute cost towards flow
   * @param absoluteCost absolute cost itself
   * @param demand demand related to the logit model (usually path specific demand for example)
   * @param applyExpTransform when true consider exp transform of formulation, otherwise not
   * @return perceived dCost/dflow for a given (OD) flow, i.e., demand
   */
  public abstract double computeDPerceivedCostDFlow(double dAbsoluteCostDFlow, double absoluteCost, double demand, boolean applyExpTransform);


  /** SUPPORTED OPTIONS **/

  /**
   * shorthand for MNL class type
   */
  public static final String MNL = MultinomialLogit.class.getCanonicalName();

  /**
   * shorthand for WEIBIT class type
   */
  public static final String WEIBIT = Weibit.class.getCanonicalName();



  /** default scaling factor applied */
  public static final double DEFAULT_SCALING_FACTOR = 1.0;


  /** Scaling factor */
  public double getScalingFactor() {
    return scalingFactor;
  }

  /** override scaling factor to use
   *
   * @param scalingFactor to set
   * */
  public void setScalingFactor(double scalingFactor) {
    this.scalingFactor = scalingFactor;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract ChoiceModel shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract ChoiceModel deepClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    var keyValueMap = new HashMap<String, String>();
    var privateFieldNameValues = ReflectionUtils.declaredFieldsNameValueMap(this, i -> Modifier.isProtected(i) && !Modifier.isStatic(i));
    privateFieldNameValues.forEach((k, v) -> keyValueMap.put(k, v.toString()));
    return keyValueMap;
  }

}
