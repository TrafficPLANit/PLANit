package org.planit.path.choice;

import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignmentComponentFactory;
import org.planit.input.InputBuilderListener;
import org.planit.path.choice.logit.LogitChoiceModel;
import org.planit.utils.configurator.Configurator;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;

/**
 * All path choice instances are built using this or a derived version of this builder
 *
 * @author markr
 *
 */
public class StochasticPathChoiceBuilder extends PathChoiceBuilder<StochasticPathChoice> {

  /** the logger */
  protected static final Logger LOGGER = Logger.getLogger(StochasticPathChoiceBuilder.class.getCanonicalName());
  
  /**
   *  create the configurator that goes with this builder
   */
  @Override
  protected Configurator<StochasticPathChoice> createConfigurator() throws PlanItException {
    return new StochasticPathChoiceConfigurator();
  }  
  
  
  /** create a physical cost instance based on configuration
   * 
   * @return physical cost instance
   * @throws PlanItException thrown if error
   */  
  protected LogitChoiceModel createLogitChoiceModelInstance(StochasticPathChoiceConfigurator configurator) throws PlanItException {
    TrafficAssignmentComponentFactory<LogitChoiceModel> logitChoiceModelFactory = new TrafficAssignmentComponentFactory<LogitChoiceModel>(LogitChoiceModel.class);
    logitChoiceModelFactory.addListener(getInputBuilderListener(), TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);    
    return logitChoiceModelFactory.create(configurator.getLogitModel().getClassTypeToConfigure().getCanonicalName(), new Object[] { getGroupIdToken() });
  }  
  
  /**
   * call to build and configure all sub components of this builder
   * 
   * @param pathChoiceInstance the instance to build on
   * @throws PlanItException  thrown if error
   */
  @Override
  protected void buildSubComponents(StochasticPathChoice pathChoiceInstance) throws PlanItException {
    StochasticPathChoiceConfigurator configurator = ((StochasticPathChoiceConfigurator)getConfigurator());

    // build logit model
    if(configurator.getLogitModel() != null) {
      LogitChoiceModel logitModel = createLogitChoiceModelInstance(configurator);
      configurator.getLogitModel().configure(logitModel);
      pathChoiceInstance.setLogitModel(logitModel);
    }    
  }  


  // PUBLIC

  /**
   * Constructor
   * 
   * @param projectToken           idGrouping token
   * @param inputBuilderListener   the input builder listener
   * @throws PlanItException thrown if error
   */
  public StochasticPathChoiceBuilder(final IdGroupingToken projectToken, InputBuilderListener inputBuilderListener) throws PlanItException {
    super(StochasticPathChoice.class, projectToken, inputBuilderListener);    
  }

}