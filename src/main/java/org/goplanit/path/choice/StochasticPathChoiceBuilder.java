package org.goplanit.path.choice;

import java.util.logging.Logger;

import org.goplanit.component.PlanitComponentFactory;
import org.goplanit.input.InputBuilderListener;
import org.goplanit.path.choice.logit.LogitChoiceModel;
import org.goplanit.path.choice.logit.LogitChoiceModelConfigurator;
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
   * @param logitConfigurator for the logit model that is to be created
   * @return created choice model
   * @throws PlanItException thrown if error
   */
  protected LogitChoiceModel createLogitChoiceModelInstance(LogitChoiceModelConfigurator<?> logitConfigurator) throws PlanItException {
    PlanitComponentFactory<LogitChoiceModel> logitChoiceModelFactory = new PlanitComponentFactory<>(LogitChoiceModel.class);
    logitChoiceModelFactory.addListener(getInputBuilderListener());
    return logitChoiceModelFactory.create(
            logitConfigurator.getClassTypeToConfigure().getCanonicalName(), new Object[] { getGroupIdToken() });
  }

  /**
   * call to build and configure all sub components of this builder
   * 
   * @param pathChoiceInstance the instance to build on
   * @throws PlanItException thrown if error
   */
  @Override
  protected void buildSubComponents(StochasticPathChoice pathChoiceInstance) throws PlanItException {
    StochasticPathChoiceConfigurator configurator = ((StochasticPathChoiceConfigurator) getConfigurator());

    // create logit model component
    if (configurator.getLogitModel() != null) {
      LogitChoiceModel logitModel = createLogitChoiceModelInstance(configurator.getLogitModel());
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

  /**
   * Constructor
   *
   * @param configurator        the configurator to adopt (copy by reference)
   * @param projectToken         idGrouping token
   * @param inputBuilderListener the input builder listener
   * @throws PlanItException thrown if error
   */
  public StochasticPathChoiceBuilder(final StochasticPathChoiceConfigurator configurator,
                                     final IdGroupingToken projectToken,
                                     InputBuilderListener inputBuilderListener) throws PlanItException {
    this(projectToken, inputBuilderListener);
    setConfigurator(configurator); // override and use this configurator instead of starting from scratch
  }

}