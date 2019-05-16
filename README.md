# PLANit

This is the core module of the PLANit project. It contains all the algorithms and computational components which can be used to construct projects, traffic assignments, etc. The PLANit project promotes the use of its native I/O formats which are XML based, however it is equally well possible to define your own input format and/or poutput format. PLANit it is completely modular and open such that you can replace, add, include, or exclude modules any way you please. 

For more information on the natively supported XML formats we kindly refer to <https://github.sydney.edu.au/PLANit/PLANitXML>

For our ongoing work on providing a Python support we kindly refer to <https://github.sydney.edu.au/PLANit/PLANitPython>

## Core components

Each PLANit project consists of three core input components:

* a physical network, i.e., the supply side infrastructure consisting of roads and intersections (links and nodes)
* One or more travel demands (often referred to as OD-matrices) by time-of-day and user class (mode, traveller type), which are currently implemented as the trips between travel analysis zones
* one or more zoning structures, representing the interaction between demand and supply defining the (geospatial area of ) travel zone granularity and their point(s) of interaction with the physical network, i.e., how the demand can enter/exit the physical network via virtual connections (centroids, connectoids)

Each PLANit project can configure one or more traffic assignment scenarios which can pick and choose:

* which zoning structure to use
* which travel demand matrix or matrices to use, i.e., which time periods to model
* which modes to consider
* the traffic assignment configuration itself (what network loading method, what equilibration approach, etc.)

## Current limitations

* Currently we only support macroscopic traffic assignment approaches, meaning that the framework is optimised for aggregate flow based assignment methods
* Currently we only implemented the well known traditinoal static capacity restrained assignment model which uses link performance functions

## indicative example

Below you will find an indicative example of how one would configure a project with a single traditional static traffic assignment in Java

```java

import java.util.logging.Logger;

import org.planit.event.listener.InputBuilderListener;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.output.OutputType;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.formatter.OutputFormatter;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.demand.Demands;
import org.planit.project.PlanItProject;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.trafficassignment.DeterministicTrafficAssignment;
import org.planit.trafficassignment.TraditionalStaticAssignment;
import org.planit.trafficassignment.builder.CapacityRestrainedTrafficAssignmentBuilder;
import org.planit.zoning.Zoning;

/**
 * PLANit Example
 * 
 * @author markr
 *
 */
public class PLANitExample{
	
	

	/**
 	* Main method for the BasicCsvMain program.  Only used to start the program
 	* 
 	* @param args				main method args
 	*/
	public static void main(String[] args) {

		try {
			// PROJECT LEVEL -------------------------------------------------------------------------------
			// Initialse project with default native I/O
			PlanItProject project = new PlanItProject(new PLANitXMLInputBuilder("<my_path_to_project_dir>"));
        		outputFormatter = project.createAndRegisterOutputFormatter(PLANitXMLOutputFormatter.class.getCanonicalName());
		
			// Core input components available on the project level - parse data
			PhysicalNetwork physicalNetwork = project.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());
			Zoning zoning = project.createAndRegisterZoning();
			Demands demands = project.createAndRegisterDemands(); 							

			// ASSIGNMENT LEVEL -------------------------------------------------------------------------------
        		// Create on project 
			DeterministicTrafficAssignment assignment = project.createAndRegisterDeterministicAssignment(TraditionalStaticAssignment.class.getCanonicalName());		
			// Dedicated builder for each assignment instance --> simplify the user configuration by using it
			CapacityRestrainedTrafficAssignmentBuilder taBuilder = (CapacityRestrainedTrafficAssignmentBuilder) assignment.getBuilder();
 		
			// Choose traffic assignment inputs/outputs
			taBuilder.registerPhysicalNetwork(physicalNetwork);								
			taBuilder.registerZoning(zoning);
			taBuilder.registerDemands(demands);	
        		taBuilder.registerOutputFormatter(outputFormatter);

			// Choose assignment components
			taBuilder.createAndRegisterPhysicalTravelTimeCostFunction(BPRLinkTravelTimeCost.class.getCanonicalName());		//BPR for physical roads
			taBuilder.createAndRegisterVirtualTravelTimeCostFunction(FixedConnectoidTravelTimeCost.class.getCanonicalName());	//Fixed cost for virtual links 		
			taBuilder.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());						// MSA for equilibration smoothing	
		        
	    		// Configure assignment components
			assignment.getOutputConfiguration().setPersistOnlyFinalIteration(true)							// Only store final result
        		assignment.getGapFunction().getStopCriterion().setMaxIterations(maxIterations);						// Limit number of iterations
        		assignment.getGapFunction().getStopCriterion().setEpsilon(epsilon);							// Convergence criterium
        
			// Run it!
        		project.executeAllTrafficAssignments();
		} catch (PlanItException e) {
			e.printStackTrace();
		}
		
	}
}
```










