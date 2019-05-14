package org.planit.trafficassignment;

import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.planit.cost.Cost;
import org.planit.demand.Demands;
import org.planit.event.RequestAccesseeEvent;
import org.planit.exceptions.PlanItException;
import org.planit.gap.GapFunction;
import org.planit.interactor.InteractorAccessor;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.transport.TransportNetwork;
import org.planit.network.virtual.ConnectoidSegment;
import org.planit.output.OutputManager;
import org.planit.output.OutputType;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.formatter.OutputFormatter;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.supply.networkloading.NetworkLoading;
import org.planit.trafficassignment.builder.TrafficAssignmentBuilder;
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
     * Output manager deals with all the output configurations for the registered traffic assignments
     */
    protected OutputManager outputManager;  
    
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
     * holds the count of segments in the transport network
     */
    protected int numberOfNetworkSegments;
    
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
        
    // protected getters and setters
    
/**
 * Get the TransportNetwork used in the current assignment
 * 
 * @return        TransportNetwork used in current assignment
 */
    public TransportNetwork getTransportNetwork() {
        return transportNetwork;
    }
        
    // Public
    
 /** 
  * Constructor
  */
    public TrafficAssignment() {
        this.id = IdGenerator.generateId(TrafficAssignment.class);
        outputManager = new OutputManager();
        createGapFunction();
    }
    
/** 
 * Method that allows one to activate specific output types for persistence which is passed on to the output manager
 * 
 * @param outputTypes             one or more OutputType objects to be used
 * @throws PlanItException       thrown if there is an error activating the output
 */
    public void activateOutput(OutputType ...outputTypes) throws PlanItException {
        // ask the traffic assignment specific instance to create the configuration (create base implementation in this class)
        // to allow for specific implementations.
        // pass on the output configuration to the manager for storing
        for (OutputType outputType : outputTypes) {
            LOGGER.info("Registering Output Type " + outputType);
            OutputAdapter outputAdapter = createOutputAdapter(outputType);
            outputManager.createAndRegisterOutputTypeConfiguration(outputType, outputAdapter);
        }
    }   
    

/** Each traffic assignment implementation has its own unique output adapter providing access to the data that it wants or allows to be persisted.
 *  therefore this factory method is required to be implemented by the concrete instances of a traffic assignment class
 *  
 * @param outputType              the type the output adapter should be suitable for
 * @return                                output adapter instance for the specified output type
 * @throws PlanItException     thrown if there is an error
 */
    protected abstract OutputAdapter createOutputAdapter(OutputType outputType) throws PlanItException;
    
/**
 * Verify if the traffic assignment components are compatible and nonnull 
 * 
 * @throws PlanItException    thrown if the components are not compatible
 */
    //TODO - This method is currently empty.  It original version could throw PlanItIncompatibilityException.  We need to check whether we still need it and whether it should throw PlanItIncompatibilityException.
    protected void verifyComponentCompatibility() throws PlanItException {
        //TODO
    }
    
/**
 * Allow all derived assignment classes to initialize members and open data resources before equilibration commences
 * 
 * @throws PlanItException    thrown if there is an error
 */
    protected abstract void initialiseBeforeEquilibration() throws PlanItException;    

/** 
 * Each traffic assignment class can have its own builder which reveals what components need to be registered on the traffic assignment instance in order to function properly.
 * 
 * @return            trafficAssignmentBuilder to use
 */
    public abstract TrafficAssignmentBuilder getBuilder();

/**
 * Allow assignment classes to close data resources after equilibration
 * 
 * @throws PlanItException      thrown if there is an error
 */
    protected abstract void finalizeAfterEquilibration() throws PlanItException;
        
/**
 * Execute assignment, including initializing resources, running equilibration and then closing resources
 * 
 * @throws PlanItException   thrown if there is an error
 */
    public void execute() throws PlanItException  {
        checkForEmptyComponents();  
        verifyComponentCompatibility();
        transportNetwork = new TransportNetwork(physicalNetwork, zoning);
        transportNetwork.integrateConnectoidsAndLinks();
        initialiseBeforeEquilibration();            
        executeEquilibration();
        finalizeAfterEquilibration();
        LOGGER.info("Finished equilibration");
        transportNetwork.removeVirtualNetworkFromPhysicalNetwork();     // disconnect here since the physical network might be reused in a different assignment
        LOGGER.info("Finished execution");
    }

/**
 * Run equilibration after resources initialized, including saving results
 * 
 * @throws PlanItException   thrown if there is an error
 */
    public abstract void executeEquilibration() throws PlanItException;

    // Getters - Setters

/** collect traffic assignment id
 * @return id
 */
    public long getId() {
        return id;
    }
    
/** Provide the output configuration for user access (via the output manager)
 * @return outputConfiguration for this traffic assignment
 */
    public OutputConfiguration getOutputConfiguration() {
        return outputManager.getOutputConfiguration();
    }

/**
 * Set the Smoothing object for the current assignment
 * 
 * @param smoothing       Smoothing object for the current assignment
 */
    public void setSmoothing(@Nonnull Smoothing smoothing) {
        this.smoothing = smoothing;
    }

/**
 * Set the PhysicalNetwork for the current assignment
 * 
 * @param physicalNetwork       the PhysicalNetwork object for the current assignment
 */
    public void setPhysicalNetwork(@Nonnull PhysicalNetwork physicalNetwork) {
        this.physicalNetwork = physicalNetwork;
    }
    
/** Collect the gap function which is to be implemented by a derived class of TrafficAssignment
 * @return gapFunction
 */
    public abstract GapFunction getGapFunction();
    
/** Create the gap function which is to be implemented by a derived class of TrafficAssignment
 * @return gapFunction
 */ 
    protected abstract GapFunction createGapFunction();

/**
 * Set the Demands object for the current assignment
 * 
 * @param demands       the Demands object for the current assignment
 */
    public void setDemands(@Nonnull Demands demands) {
        this.demands = demands;     
    }
    
/**
 * Set the zoning object for the current assignment
 * 
 * @param zoning        the Zoning object for the current assignment
 */
    public void setZoning(@Nonnull Zoning zoning) {
        this.zoning = zoning;       
    }   
    
/**
 * Get the physical cost object for the current assignment 
 * 
 * @return          the physical cost object for the current assignment
 */
    public Cost<LinkSegment> getPhysicalCost() {
        return physicalCost;
    }

/** 
 * Set the physical cost where in case the cost is an interactorCccessor will trigger an event to get access to the
 * required data via requesting an InteractorAccessee
 * 
 * @param physicalCost               the physical cost object for the current assignment 
 * @throws PlanItException          thrown if there is an error
 */
    public void setPhysicalCost(Cost<LinkSegment> physicalCost) throws PlanItException {
        this.physicalCost = physicalCost;
        if (this.physicalCost instanceof InteractorAccessor) {
            // accessor requires accessee --> request accessee via event --> and listen back for result
            RequestAccesseeEvent event = new RequestAccesseeEvent((InteractorAccessor) this.physicalCost);
            eventManager.dispatchEvent(event);
        }
    }

/**
 * Returns the virtual cost object for the current assignment
 * 
 * @return         the virtual cost object for the current assignments
 */
    public Cost<ConnectoidSegment> getVirtualCost() {
        return virtualCost;
    }

/** Set the virtual cost where in case the cost is an interactorCccessor will trigger an event to get access to the
  * required data via requesting an InteractorAccessee
  * 
  * @param virtualCost              the virtual cost object to be assigned
  * @throws PlanItException     thrown if there is an error
  */    
    public void setVirtualCost(Cost<ConnectoidSegment> virtualCost) throws PlanItException {
        this.virtualCost = virtualCost;
        if (this.virtualCost instanceof InteractorAccessor) {
            // accessor requires accessee --> request accessee via event --> and listen back for result
            RequestAccesseeEvent event = new RequestAccesseeEvent((InteractorAccessor) this.virtualCost);
            eventManager.dispatchEvent(event);
        }
    }

/** Register the output formatter on the assignment
 *
 * @param outputFormatter          OutputFormatter to be registered
 */
    public void registerOutputFormatter(OutputFormatter outputFormatter) {
        outputManager.registerOutputFormatter(outputFormatter);
    }
    
/**
 * Return the number of network segments in the current assignment
 * 
 * @return       number of network segments
 */
    public int getNumberOfNetworkSegments() {
        return numberOfNetworkSegments;
    }
   
}