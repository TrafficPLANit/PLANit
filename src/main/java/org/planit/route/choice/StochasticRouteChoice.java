package org.planit.route.choice;

import java.util.logging.Logger;

import org.planit.exceptions.PlanItException;
import org.planit.input.InputBuilderListener;
import org.planit.od.odroute.ODRouteMatrix;
import org.planit.route.choice.logit.LogitChoiceModel;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;
import org.planit.utils.id.IdGroupingToken;

/**
 * Stochastic route choice component. Stochasticity is reflected by the fact that the route choice is applied by means of a logit model, to be configured here. Also, due to being
 * stochastic the route can/must be provided beforehand. This is also configured via this class
 *
 * @author markr
 *
 */
public class StochasticRouteChoice extends RouteChoice {

  /** generated UID */
  private static final long serialVersionUID = 6617920674217225019L;

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(StochasticRouteChoice.class.getCanonicalName());

  /**
   * logit choice model factory to create logit models to direct the probabilities of choosing paths
   */
  protected final TrafficAssignmentComponentFactory<LogitChoiceModel> logitChoiceModelFactory;

  /**
   * The registered logit choice model
   */
  protected LogitChoiceModel logitChoiceModel = null;

  /**
   * The registered od route set instance
   */
  protected ODRouteMatrix odRouteSet = null;

  /**
   * Constructor
   * 
   * @param groupId                        contiguous id generation within this group for instances of this class
   * @param trafficComponentCreateListener thelistener to fire notify of create events when we create a logit choice model
   */
  public StochasticRouteChoice(final IdGroupingToken groupId, final InputBuilderListener trafficComponentCreateListener) {
    super(groupId);
    logitChoiceModelFactory = new TrafficAssignmentComponentFactory<LogitChoiceModel>(LogitChoiceModel.class);
    logitChoiceModelFactory.addListener(trafficComponentCreateListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
  }

  /**
   * create and register the logit model of choice
   * 
   * @param canonicalName name of the class to be instantiated
   * @return the logit choice model that is registered
   * @throws PlanItException thrown if error
   */
  public LogitChoiceModel createAndRegisterLogitModel(final String canonicalName) throws PlanItException {
    this.logitChoiceModel = logitChoiceModelFactory.create(canonicalName);
    return this.logitChoiceModel;
  }

  /**
   * Register a fixed od route set to use in the form of an ODPathMatrix
   *
   * @param odRouteSet the fixed od route set in the shape of an od path matrix
   */
  public void RegisterODRouteMatrix(final ODRouteMatrix odRouteSet) {
    this.odRouteSet = odRouteSet;
  }

}
