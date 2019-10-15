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

* Currently we only support macroscopic traffic assignment approaches, meaning that the framework is optimized for aggregate flow based assignment methods
* Currently we only implemented the well known traditional static capacity restrained assignment model which uses link performance functions

## Indicative example

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
 	* Main method running the example
 	* 
 	* @param args	main method args
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

## Building code and running unit tests in Eclipse

Projects need to be built from Maven before they can be run.  The PLANit project should be built first since this is the core which all other projects use.  Then build whichever other project(s) you are working on (e.g. PLANitXML, MetroScan etc).

The Maven builds are perform any setup actions which projects may require.  In the specific case of PLANitXML, Java code is generated from XSD classes using a Maven plugin (see the Readme.md of that project for more details).

The following notes explain how to run Maven builds for these projects in Eclipse.  They are aimed at readers who are not familiar with Maven.  Readers who are experienced in Maven or other IDEs are free to do their own configuration.

Firstly ensure that you are using a version of Eclipse which has the Maven plugin built in.  Most current versions of Eclipse include Maven.  If you are unsure, select File/New/Other.. and look at the list of available wizards which appears.  If there is a folder called "Maven" which includes a link called "Maven Project", Maven is included.

Right-click on the PLANit project in the Package Explorer and select Run As.  You will see a drop-down menu.  Often you can just click "Maven Install" and it will work, since it performs the following actions:-

* Generates the Java code from the XSD files, if required;
* Compiles the Java code;
* Runs the unit tests.

The results of the unit tests appear in the Console.  The "BUILD SUCCESS" message only appears if all the unit tests pass, which is usually what you want.

However there may be times when you do not wish to perform all these steps at once.  For example, you may have made some temporary changes which cause the unit tests to fail, and you just want to compile the code without them.  

The drop-down menu has other useful options, including:-

* "Maven generate-sources", which simply creates the Java code from the XSD files;
* "Maven clean", which removes previously-created .class files from the target directory.

However the most configurable approach is to click the "Run Configurations.." and use the resulting dialog box, as follows:-

* Select "Maven Build" from the left window on the dialog box, and then click the "New Configuration" icon at the top-left of the dialog box.  This brings up a configuration window in the right-hand side of the dialog box;
* Use the "Name" box at the top to enter a name for the configuration.  This can be anything which makes sense to you.  I recommend "Maven clean install" if you follow the steps below;
* Use the "Workspace..." button under the "Base directory" box to select the project you wish to compile (e.g. PLANit, PLANitXML etc);
* Enter whichever Maven goals you want to use in the "Goals" box.  If you know nothing about Maven goals, I recommend entering "clean install" in this box (Maven goals is a large topic, see the Maven documentation for more details).

Leave the other entries at their defaults in the first instance, and click Run.  This will perform the build and run as above.  

The Run Configuration dialog box disappears as the run begins, but the configuration is still saved.  If you right-click the project and select Run/Run Configuration... again, you will see the configuration you just created under the "Maven Build" heading in the left window. You can click on it and run it again as you require.

You can change the configuration at any time as required by your code changes.  Two changes which are particularly useful are:

* Use the "Workspace" button under the "Base directory" box to change which project you are running;
* Checking or unchecking the "Skip test" box can stop or reinstate unit tests being run as part of the build.

Whenever you click Run, the configuration dialog box closes.  Its setting on its closure will be retained for the next time it is opened.  If you use this dialog box often on several projects, do not forget to check its settings on opening are appropriate for the build you are doing.

It is a matter of personal taste whether you run unit tests directly (by right-clicking on a test suite and selecting Run As/JUnit Test) or run them as part of this build process.  Running them directly will not generate Java classes from XSD files, so if you have made changes to the XSD files you must rebuild.  Running directly is fractionally quicker, but the builds only take a few seconds so the difference is negligible.











