package org.goplanit.path.choice;

import java.util.logging.Logger;

import org.goplanit.component.PlanitComponentFactory;
import org.goplanit.input.InputBuilderListener;
import org.goplanit.path.choice.logit.LogitChoiceModel;
import org.goplanit.utils.builder.Configurator;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;

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
   * {@inheritDoc}
   */
  @Override
  protected Configurator<StochasticPathChoice> createConfigurator() throws PlanItException {
    return new StochasticPathChoiceConfigurator();
  }

  /**
   * create a logit model instance based on passed in configurator
   * 
   * @param configurator for the path choice
   * @return created choice model
   * @throws PlanItException thrown if error
   */
  protected LogitChoiceModel createLogitChoiceModelInstance(StochasticPathChoiceConfigurator configurator) throws PlanItException {
    PlanitComponentFactory<LogitChoiceModel> logitChoiceModelFactory = new PlanitComponentFactory<>(LogitChoiceModel.class);
    logitChoiceModelFactory.addListener(getInputBuilderListener());
    return logitChoiceModelFactory.create(
            configurator.getLogitModel().getClassTypeToConfigure().getCanonicalName(), new Object[] { getGroupIdToken() });
  }

  /**
   * call to build and configure all sub components of this builder
   * 
   * @param pathChoiceInstance the instance to build on
   * @throws PlanItException thrown if error
   */
  @Override
  protected void buildSubComponents(StochasticPathChoice pathChoiceInstance) throws PlanItException {
    //TODO: BUG -> configurator not available, because when this builder is created a new configurator for the
    //      builder is created unrelated to the configruator that was actually configured as part of the assignment.
    //      Therefore, logitmodel configured on assignment path choice configator is not present here --> FAIL
    //      Solution -> pass in configurator from assignment to when the builder is created. UGLY but acceptable for now
    StochasticPathChoiceConfigurator configurator = ((StochasticPathChoiceConfigurator) getConfigurator());

    // build logit model
    if (configurator.getLogitModel() != null) {
      LogitChoiceModel logitModel = createLogitChoiceModelInstance(configurator);
      configurator.getLogitModel().configure(logitModel);
      pathChoiceInstance.setLogitModel(logitModel);
    }
  }

  // PUBLIC

  /**
   * Constructor
   * 
   * @param projectToken         idGrouping token
   * @param inputBuilderListener the input builder listener
   * @throws PlanItException thrown if error
   */
  public StochasticPathChoiceBuilder(final IdGroupingToken projectToken, InputBuilderListener inputBuilderListener) throws PlanItException {
    super(StochasticPathChoice.class, projectToken, inputBuilderListener);
  }

}