package org.planit.trafficassignment;

import java.util.SortedMap;
import java.util.SortedSet;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.planit.cost.Cost;
import org.planit.demand.Demands;
import org.planit.dto.BprResultDto;
import org.planit.event.RequestAccesseeEvent;
import org.planit.exceptions.PlanItException;
import org.planit.gap.GapFunction;
import org.planit.interactor.InteractorAccessor;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.transport.TransportNetwork;
import org.planit.network.virtual.ConnectoidSegment;
import org.planit.network.virtual.VirtualNetwork;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.supply.networkloading.NetworkLoading;
import org.planit.trafficassignment.builder.TrafficAssignmentBuilder;
import org.planit.userclass.Mode;
import org.planit.time.TimePeriod;
import org.planit.utils.IdGenerator;
import org.planit.zoning.Zoning;

/**
 * Traffic assignment class which simultaneously is responsible for the loading hence it is also considered as a traffic assignment component
 * of this type
 * 
 * @author markr
 *
 */
public abstract class TrafficAssignment extends NetworkLoading {
	
	private static final Logger LOGGER = Logger.getLogger(TrafficAssignment.class.getName());	
	
	// Private
	
/**
 * Physical network to use
 */
	private PhysicalNetwork physicalNetwork;
	
/**
 * The zoning to use
 */
	private Zoning zoning;
	
	// Protected
	
/**
 * The transport network to use which is an adaptor around the physical network and the zoning
 */
	protected TransportNetwork transportNetwork = null;	
	
/**
 * The physical link cost function
 */
	protected Cost<LinkSegment> physicalCost;
	
/**
 * The virtual link cost function
 */
	protected Cost<ConnectoidSegment> virtualCost;
	
	/**
	 * Unique id
	 */
	protected final long id;
	
	/**
	 * the smoothing to use
	 */
	protected Smoothing smoothing = null;		
		
	/**
	 * The demand to use
	 */
	protected Demands demands = null;
	
/** 
 * Check if any components are undefined, if so throw exception
 * 
 * @throws PlanItException      thrown if any components are undefined
 */
	protected void checkForEmptyComponents() throws PlanItException {
		if (demands == null) {
			throw new PlanItException("Demand is null");
		}
		if (physicalNetwork == null) {
			throw new PlanItException("Network is null");
		}
		if (smoothing == null) {
			throw new PlanItException("Smoothing is null");
		}
		if (zoning == null) {
			throw new PlanItException("Zoning is null");
		}
	}
	
/**
 * Creates the integrated TransportNetwork object
 * 
 * This method integrates the physical and virtual networks using the PlanitGeoUtils object to calculate link and connectoid lengths.
 * 
 * @param physicalNetwork				the physical network object
 * @param zoning								the zoning object (contain the virtual network)
 * @return											the integrated transport network
 * @throws PlanItException              thrown if there is an error
 */
	protected TransportNetwork integrateVirtualAndPhysicalNetworks(PhysicalNetwork physicalNetwork, Zoning zoning) throws PlanItException {
	    transportNetwork = new TransportNetwork(physicalNetwork, zoning);
		VirtualNetwork virtualNetwork = zoning.getVirtualNetwork();		
		transportNetwork.integrateConnectoidsAndLinks(virtualNetwork);
		return transportNetwork;
	}	
	
	// protected getters and setters
	
	protected TransportNetwork getTransportNetwork() {
		return transportNetwork;
	}
		
	// Public
	
/** 
 * Constructor
 */
	public TrafficAssignment() {
		this.id = IdGenerator.generateId(TrafficAssignment.class);
		createGapFunction();
	}
	
/** 
 * Each traffic assignment class can have its own builder which reveals what components need to be registered on the traffic assignment instance in order to function properly.
 * 
 * @return            trafficAssignmentBuilder to use
 */
	public abstract TrafficAssignmentBuilder getBuilder();
	
	
/**
 * Verify if the traffic assignment components are compatible and nonnull 
 * 
 * @throws PlanItException    thrown if the components are not compatible
 */
//TODO - This method is currently empty.  It original version could throw PlanItIncompatibilityException.  We need to check whether we still need it and whether it should throw PlanItIncompatibilityException.
	public void verifyComponentCompatibility() throws PlanItException {
		//TODO
	}
	
	/**
	 * Allow all derived assignment classes to initialize members just before equilibration commences
	 */
	public abstract void initialiseBeforeEquilibration();
	
/**
 * Execute assignment
 * 
 * @return                              SortedMap containing results
 * @throws PlanItException   thrown if there is an error
 */
	public SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>> execute() throws PlanItException  {
		checkForEmptyComponents();	
		verifyComponentCompatibility();
		integrateVirtualAndPhysicalNetworks(physicalNetwork, zoning);		
		initialiseBeforeEquilibration();			
		SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>> results = executeEquilibration();						                                    // Actual algorithm execution
		LOGGER.info("Finished equilibration");
		transportNetwork.removeVirtualNetworkFromPhysicalNetwork();		// disconnect here since the physical network might be reused in a different assignment
		LOGGER.info("Finished execution");
		return results;
	}

/**
 * Execute assignment
 * @return                              SortedMap containing results
 * @throws PlanItException   thrown if there is an error
 */
	public abstract  SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>> executeEquilibration() throws PlanItException;
	
	
	// Getters - Setters

	/** collect traffic assignment id
	 * @return id
	 */
	public long getId() {
		return id;
	}

	public void setSmoothing(@Nonnull Smoothing smoothing) {
		this.smoothing = smoothing;
	}

	public void setPhysicalNetwork(@Nonnull PhysicalNetwork physicalNetwork) {
		this.physicalNetwork = physicalNetwork;
	}
	
	public abstract GapFunction getGapFunction();
	
	protected abstract GapFunction createGapFunction();

	public void setDemands(@Nonnull Demands demands) {
		this.demands = demands;		
	}
	
	public void setZoning(@Nonnull Zoning zoning) {
		this.zoning = zoning;		
	}	
	
	public Cost<LinkSegment> getPhysicalCost() {
		return physicalCost;
	}

	public void setPhysicalCost(Cost<LinkSegment> physicalCost) throws PlanItException {
		this.physicalCost = physicalCost;
		if (this.physicalCost instanceof InteractorAccessor) {
			// accessor requires accessee --> request accessee via event --> and listen back for result
			RequestAccesseeEvent event = new RequestAccesseeEvent((InteractorAccessor) this.physicalCost);
			eventManager.dispatchEvent(event);
		}
	}

	public Cost<ConnectoidSegment> getVirtualCost() {
		return virtualCost;
	}

	public void setVirtualCost(Cost<ConnectoidSegment> virtualCost) throws PlanItException {
		this.virtualCost = virtualCost;
		if (this.virtualCost instanceof InteractorAccessor) {
			// accessor requires accessee --> request accessee via event --> and listen back for result
			RequestAccesseeEvent event = new RequestAccesseeEvent((InteractorAccessor) this.virtualCost);
			eventManager.dispatchEvent(event);
		}
	}

}