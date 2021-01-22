package org.planit.assignment;

import org.planit.demands.Demands;
import org.planit.input.InputBuilderListener;
import org.planit.network.InfrastructureNetwork;
import org.planit.path.choice.PathChoice;
import org.planit.path.choice.PathChoiceBuilder;
import org.planit.path.choice.PathChoiceBuilderFactory;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.zoning.Zoning;

/**
 * A dynamic traffic assignment builder is assumed to only support capacity constrained traffic assignment instances. It is used to build the traffic assignment instance with the
 * proper configuration settings
 *
 * @author markr
 *
 */
public abstract class DynamicTrafficAssignmentBuilder<T extends DynamicTrafficAssignment> extends CapacityConstrainedTrafficAssignmentBuilder<T> {

  // needed to allow path choice to register inputbuilder listener on its traffic components
  @SuppressWarnings("unused")
  private final InputBuilderListener trafficComponentCreateListener;

  /**
   * create a path choice instance based on configuration
   * 
   * @return path choice instance
   * @throws PlanItException thrown if error
   */
  protected PathChoice createPathChoiceInstance(DynamicAssignmentConfigurator<? extends DynamicTrafficAssignment> configurator) throws PlanItException {
    TrafficAssignmentComponentFactory<PathChoice> pathChoiceFactory = new TrafficAssignmentComponentFactory<PathChoice>(PathChoice.class);
    pathChoiceFactory.addListener(getInputBuilderListener(), TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
    return pathChoiceFactory.create(configurator.getPathChoice().getClassTypeToConfigure().getCanonicalName(), new Object[] { getGroupIdToken() });
  }

  /**
   * In addition to the super class sub components, we also construct the subcomponents specific to dynamic traffic assignment
   * 
   * @param trafficAssignmentInstance the instance to build on
   */
  protected void buildSubComponents(T trafficAssignmentInstance) throws PlanItException {
    super.buildSubComponents(trafficAssignmentInstance);

    DynamicAssignmentConfigurator<? extends DynamicTrafficAssignment> configurator = (DynamicAssignmentConfigurator<? extends DynamicTrafficAssignment>) getConfigurator();

    /*
     * path choice sub component... ...because it has sub components of its own, we must construct a builder for it instead of instantiating it directly here
     */
    if (configurator.getPathChoice() != null) {
      PathChoiceBuilder<? extends PathChoice> pathChoiceBuilder = PathChoiceBuilderFactory.createBuilder(configurator.getPathChoice().getClassTypeToConfigure().getCanonicalName(),
          getGroupIdToken(), getInputBuilderListener());
      PathChoice pathChoice = pathChoiceBuilder.build();
      trafficAssignmentInstance.setPathChoice(pathChoice);
    }
  }

  /**
   * Constructor
   *
   * @param trafficAssignmentClass the traffic assignment class we are building
   * @param groupId                the id generation group this builder is part of
   * @param inputBuilderListener   the listener for further traffic components that are created by the builder
   * @param demands                the demands
   * @param zoning                 the zoning
   * @param network                the network
   * @throws PlanItException thrown if there is an exception
   */
  public DynamicTrafficAssignmentBuilder(final Class<T> trafficAssignmentClass, IdGroupingToken groupId, final InputBuilderListener inputBuilderListener, final Demands demands,
      final Zoning zoning, final InfrastructureNetwork network) throws PlanItException {

    super(trafficAssignmentClass, groupId, inputBuilderListener, demands, zoning, network);
    this.trafficComponentCreateListener = inputBuilderListener;
  }

}
