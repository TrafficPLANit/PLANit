package org.planit.assignment.ltm;

import org.planit.assignment.TrafficAssignmentBuilder;
import org.planit.component.PlanitComponentFactory;
import org.planit.demands.Demands;
import org.planit.input.InputBuilderListener;
import org.planit.network.TransportLayerNetwork;
import org.planit.path.choice.PathChoice;
import org.planit.path.choice.PathChoiceBuilder;
import org.planit.path.choice.PathChoiceBuilderFactory;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.zoning.Zoning;

/**
 * An LTM traffic assignment builder is assumed to only support Link Transmission Model (LTM) traffic assignment instances. It is used to build the traffic assignment instance with
 * the proper configuration settings
 *
 * @author markr
 *
 */
public abstract class LtmTrafficAssignmentBuilder<T extends LtmAssignment> extends TrafficAssignmentBuilder<T> {

  /**
   * create a path choice instance based on configuration
   * 
   * @param configurator to extract path choice type from
   * @return path choice instance
   * @throws PlanItException thrown if error
   */
  protected PathChoice createPathChoiceInstance(LtmConfigurator<? extends LtmAssignment> configurator) throws PlanItException {
    PlanitComponentFactory<PathChoice> pathChoiceFactory = new PlanitComponentFactory<PathChoice>(PathChoice.class);
    pathChoiceFactory.addListener(getInputBuilderListener());
    return pathChoiceFactory.create(configurator.getPathChoice().getClassTypeToConfigure().getCanonicalName(), new Object[] { getGroupIdToken() });
  }

  /**
   * In addition to the super class sub components, we also construct the subcomponents specific to dynamic traffic assignment
   * 
   * @param ltmAssignmentInstance the instance to build on
   */
  protected void buildSubComponents(T ltmAssignmentInstance) throws PlanItException {
    /* delegate to super class for base sub components */
    super.buildSubComponents(ltmAssignmentInstance);

    /* path choice is added in all LTM assignments */
    LtmConfigurator<T> configurator = (LtmConfigurator<T>) getConfigurator();

    /*
     * path choice sub component... ...because it has sub components of its own, we must construct a builder for it instead of instantiating it directly here
     */
    if (configurator.getPathChoice() != null) {
      PathChoiceBuilder<? extends PathChoice> pathChoiceBuilder = PathChoiceBuilderFactory.createBuilder(configurator.getPathChoice().getClassTypeToConfigure().getCanonicalName(),
          getGroupIdToken(), getInputBuilderListener());
      PathChoice pathChoice = pathChoiceBuilder.build();
      ltmAssignmentInstance.setPathChoice(pathChoice);
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
  public LtmTrafficAssignmentBuilder(final Class<T> trafficAssignmentClass, IdGroupingToken groupId, final InputBuilderListener inputBuilderListener, final Demands demands,
      final Zoning zoning, final TransportLayerNetwork<?, ?> network) throws PlanItException {
    super(trafficAssignmentClass, groupId, inputBuilderListener, demands, zoning, network);
  }

}
