package org.planit.input;

import java.util.logging.Logger;

import org.planit.cost.physical.PhysicalCost;
import org.planit.cost.physical.initial.InitialPhysicalCost;
import org.planit.cost.virtual.VirtualCost;
import org.planit.demand.Demands;
import org.planit.event.CreatedProjectComponentEvent;
import org.planit.event.Event;
import org.planit.event.listener.EventListener;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.supply.fundamentaldiagram.FundamentalDiagram;
import org.planit.supply.network.nodemodel.NodeModel;
import org.planit.supply.networkloading.NetworkLoading;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.zoning.Zoning;

/**
 * Listener which is invoked whenever a project component is created. To be used
 * to populate the project components from some data source
 * 
 * @author markr
 *
 */
public abstract class InputBuilderListener implements EventListener {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = Logger.getLogger(InputBuilderListener.class.getName());

	/**
	 * Process event and call the right onX method
	 * 
	 * @see org.planit.event.listener.EventListener#process(org.planit.event.Event)
	 * 
	 * @param event Event to be processed
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	public void process(Event event) throws PlanItException {
		try {
			if (event instanceof CreatedProjectComponentEvent<?>) {
				onCreateProjectComponent((CreatedProjectComponentEvent<?>) event);
			}
		} catch (Exception ex) {
			throw new PlanItException(ex);
		}
	}

	/**
	 * Empty event handler for population of PhysicalNetwork
	 * 
	 * Override this method if actions are required on creation of the physical network
	 * 
	 * @param projectComponent the physical network project component
	 * @throws PlanItException thrown if there is an exception
	 */
	protected void populatePhysicalNetwork(PhysicalNetwork projectComponent) throws PlanItException {
		LOGGER.info("Populating Physical Network - No actions on creation");
	}

	/**
	 * Empty event handler for population of Zoning object
	 * 
	 * Override this method if actions are required on creation of the zoning.
	 * 
	 * @param projectComponent zoning project component
	 * @throws PlanItException thrown if there is an error
	 */
	protected void populateZoning(Zoning projectComponent) throws PlanItException {
		LOGGER.info("Populating Zoning - No actions on creation");
	}

	/**
	 * Empty event handler for population of Demands object
	 * 
	 * Override this method if actions are required on creation of the demands.
	 * 
	 * @param projectComponent demands project component
	 * @throws PlanItException thrown if there is an error
	 */
	protected void populateDemands(Demands projectComponent) throws PlanItException {
		LOGGER.info("Populating Demands - No actions on creation");
	}

	/**
	 * Empty event handler for population of InitialPhysicalCost object
	 * 
	 * Override this method if actions are required on creation of initial physical costs
	 * 
	 * @param projectComponent initial physical costs project component
	 * @param parameter object used to pass in extra configuration information (usually the location of an input data file)
	 * @throws PlanItException thrown if there is an error
	 */
	protected void populateInitialPhysicalCost(InitialPhysicalCost projectComponent, Object parameter)
			throws PlanItException {
		LOGGER.info("Populating InitialPhysicalCost  - No actions on creation");
	}

	/**
	 * Empty event handler for population of PhysicalCost object
	 * 
	 * Override this method if actions are required on creation of the dynamic physical cost object
	 * 
	 * @param projectComponent physical cost object
	 * @throws PlanItException thrown if there is an error
	 */
	protected void populatePhysicalCost(PhysicalCost projectComponent) throws PlanItException {
		LOGGER.info("Populating PhysicalCost - No actions on creation");
	}

	/**
	 * Empty event handler for population of VirtualCost object
	 * 
	 *  Override this method if actions are required on creation of virtual cost object
	 * 
	 * @param projectComponent virtual cost project component
	 * @throws PlanItException thrown if there is an error
	 */
	protected void populateVirtualCost(VirtualCost projectComponent) throws PlanItException {
		LOGGER.info("Populating VirtualCost  - No actions on creation");
	}

	/**
	 * Empty event handler for population of Smoothing object
	 * 
	 *  Override this method if actions are required on creation of smoothing object
	 * 
	 * @param projectComponent smoothing project component
	 * @throws PlanItException thrown if there is an error
	 */
	protected void populateSmoothing(Smoothing projectComponent) throws PlanItException {
		LOGGER.info("Populating Smoothing  - No actions on creation");
	}

	/**
	 * Empty event handler for population of NetworkLoding object
	 * 
	 *  Override this method if actions are required on creation of network loading object
	 * 
	 * @param projectComponent network loading project component
	 * @throws PlanItException thrown if there is an error
	 */
	protected void populateNetworkLoading(NetworkLoading projectComponent) throws PlanItException {
		LOGGER.info("Populating Network Loading  - No actions on creation");
	}

	/**
	 * Empty event handler for population of NodeModel object
	 * 
	 *  Override this method if actions are required on creation of node model object
	 * 
	 * @param projectComponent node model project component
	 * @throws PlanItException thrown if there is an error
	 */
	protected void populateNodeModel(NodeModel projectComponent) throws PlanItException {
		LOGGER.info("Populating Node Model - No actions on creation");
	}

	/**
	 * Empty event handler for population of FundamentalDiagram object
	 * 
	 *  Override this method if actions are required on creation of fundamental diagram object
	 * 
	 * @param projectComponent fundamental diagram project component
	 * @throws PlanItException thrown if there is an error
	 */
	protected void populateFundamentalDiagram(FundamentalDiagram projectComponent) throws PlanItException {
		LOGGER.info("Populating FundamentalDiagram  - No actions on creation");
	}

	/**
	 * Whenever a project component is created this method will be invoked
	 * 
	 * @param event event containing the created (and empty) project component
	 * @throws PlanItException thrown if there is an error
	 */
	public void onCreateProjectComponent(CreatedProjectComponentEvent<?> event) throws PlanItException {
		Object projectComponent = event.getProjectComponent();
		if (projectComponent instanceof PhysicalNetwork) {
			populatePhysicalNetwork((PhysicalNetwork) projectComponent);
		} else if (projectComponent instanceof Zoning) {
			populateZoning((Zoning) projectComponent);
		} else if (projectComponent instanceof Demands) {
			populateDemands((Demands) projectComponent);
		} else if (projectComponent instanceof InitialPhysicalCost) {
			populateInitialPhysicalCost((InitialPhysicalCost) projectComponent, event.getParameter());
		} else if (projectComponent instanceof PhysicalCost) {
			populatePhysicalCost((PhysicalCost) projectComponent);
		} else if (projectComponent instanceof VirtualCost) {
			populateVirtualCost((VirtualCost) projectComponent);
		} else if (projectComponent instanceof Smoothing) {
			populateSmoothing((Smoothing) projectComponent);
		} else if (projectComponent instanceof TrafficAssignment) {
			populateNetworkLoading((NetworkLoading) projectComponent);
		} else if (projectComponent instanceof NodeModel) {
			populateNodeModel((NodeModel) projectComponent);
		} else if (projectComponent instanceof FundamentalDiagram) {
			populateFundamentalDiagram((FundamentalDiagram) projectComponent);
		} else {
			LOGGER.info("Event component is " + projectComponent.getClass().getCanonicalName()
					+ " which is not handled by PlanIt.");
		}
	}
}
