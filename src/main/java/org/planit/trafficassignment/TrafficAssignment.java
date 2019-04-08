package org.planit.trafficassignment;

import java.io.IOException;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.opengis.referencing.operation.TransformException;
import org.planit.cost.Cost;
import org.planit.demand.Demands;
import org.planit.dto.BprResultDto;
import org.planit.event.RequestAccesseeEvent;
import org.planit.exceptions.PlanItException;
import org.planit.exceptions.PlanItIncompatibilityException;
import org.planit.gap.GapFunction;
import org.planit.geo.utils.PlanitGeoUtils;
import org.planit.interactor.InteractorAccessor;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.transport.TransportNetwork;
import org.planit.network.virtual.ConnectoidSegment;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.supply.networkloading.NetworkLoading;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.builder.TrafficAssignmentBuilder;
import org.planit.userclass.Mode;
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
	
	// Private
	
	/**
	 * Physical network to use
	 */
	private PhysicalNetwork physicalNetwork;
	
	/**
	 * The zoning to use
	 */
	private Zoning zoning;
	
	/** Construct the transport network from the combined physical and virtual network (part of the zoning) and
	 * make it available for the derived classes
	 * @param physicalNetwork
	 * @param zoning
	 */
	private void createAndRegisterTransportNetwork(@Nonnull PhysicalNetwork physicalNetwork, @Nonnull Zoning zoning) {
		this.network = new TransportNetwork(physicalNetwork, zoning);
	}	
	
	// Protected
	
	/**
	 * The transport network to use which is an adaptor around the physical network and the zoning
	 */
	protected TransportNetwork network = null;	
	
	protected Cost<LinkSegment> physicalCost;
	
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
		
	/** check if any components are undefined, if so throw exception
	 * @throws PlanItException
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
	
	// Protected - getters- setters 
	
	protected TransportNetwork getTransportNetwork() {
		return network;
	}
		
	// Public
	
	private static final Logger LOGGER = Logger.getLogger(TrafficAssignment.class.getName());

	
	/** Constructor
	 */
	public TrafficAssignment() {
		this.id = IdGenerator.generateId(TrafficAssignment.class);
		createGapFunction();
	}
	
	/** Each traffic assignment class can have its own builder which reveals what components need to be registered on the traffic assignment
	 * instance in order to function properly.
	 * @return trafficAssignmentBuilder to use
	 */
	public abstract TrafficAssignmentBuilder getBuilder();
	
	
	/**
	 * Verify if the traffic assignment components are compatible and nonnull 
	 * @throws PlanItIncompatibilityException 
	 * @throws PlanItException 
	 */
	public void verifyComponentCompatibility() throws PlanItIncompatibilityException, PlanItException {
		//TODO
	}
	
	/**
	 * Allow all derived assignment classes to initialize members just before equilibration commences
	 */
	public abstract void initialiseBeforeEquilibration();
	
	/**
	 * Execute assignment
	 * @throws PlanItException 
	 * @throws PlanItIncompatibilityException 
	 * @throws TransformException 
	 */
	public SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>> execute(PlanitGeoUtils planitGeoUtils) throws PlanItException, PlanItIncompatibilityException, TransformException {
		checkForEmptyComponents();	
		verifyComponentCompatibility();
		createAndRegisterTransportNetwork(physicalNetwork, zoning);		
		network.integratePhysicalAndVirtualNetworks(planitGeoUtils); 			        // connect here since it marries the virtual and physical networks
		initialiseBeforeEquilibration();			
		SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>> results = executeEquilibration();						                                    // Actual algorithm execution
		LOGGER.info("Finished equilibration");
		network.removeVirtualNetworkFromPhysicalNetwork();		// disconnect here since the physical network might be reused in a different assignment
		LOGGER.info("Finished execution");
		return results;
	}
		
	public SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>> execute(double connectoidLength) throws PlanItException, PlanItIncompatibilityException, TransformException {
		checkForEmptyComponents();	
		verifyComponentCompatibility();
		createAndRegisterTransportNetwork(physicalNetwork, zoning);		
		network.integratePhysicalAndVirtualNetworks(connectoidLength); 			        // connect here since it marries the virtual and physical networks
		initialiseBeforeEquilibration();			
		SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>> results = executeEquilibration();						                                    // Actual algorithm execution
		LOGGER.info("Finished equilibration");
		network.removeVirtualNetworkFromPhysicalNetwork();		// disconnect here since the physical network might be reused in a different assignment
		LOGGER.info("Finished execution");
		return results;
	}

	/**
	 * Execute assignment
	 * @throws PlanItException 
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

	public void setPhysicalCost(Cost<LinkSegment> physicalCost) throws IOException {
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

	public void setVirtualCost(Cost<ConnectoidSegment> virtualCost) throws IOException {
		this.virtualCost = virtualCost;
		if (this.virtualCost instanceof InteractorAccessor) {
			// accessor requires accessee --> request accessee via event --> and listen back for result
			RequestAccesseeEvent event = new RequestAccesseeEvent((InteractorAccessor) this.virtualCost);
			eventManager.dispatchEvent(event);
		}
	}

}
