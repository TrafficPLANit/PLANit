# Release Log

PlanIt Releases

This project contains core code which is used by all the other PLANit modules/repositories (except PLANitUtils/XML).

## 0.5.0

**Enhancements**

#102 Add support for fixed step size smoothing implementation
#105 Add K-shortest path algorithm
#107 Add support for custom implementation for method of self regulating averages a(MSRA) as a smoothing approach
#108 Allow to normalise the scale parameter for additive based logit models (MNL/bounded MNL)
#110 Support attaching geometries to outputs so we can more easily visualise in GIS software
#111 Add support for odMulti-path outputs (as default) rather than only a single path per origin-destination
#122,#123 Add support for SIMULATION output (per time period/iteration) such as gaps and computation times for easy post simulation analysis
#124 Add support for relative scaling factor to cost/distance for additive logit models
#125 Add support for quadratic linear fundamental diagram
#130 Node model - Tampere - Add option to compute flow acceptance factors by turn including when turn has zero flow
#135 Add option to persist various link ids in addition to link segment ids for link out formatter
#99 (review) sLTM/prototype - Supporting stochastic path choice without fixed path set in sLTM
#100 (review) sLTM/prototype - Add support for Weibitt for path choice
#101 (review) sLTM/prototype - Add option to let user configure scale parameter of logit models via settings
#104 (review) sLTM/prototype - Add support for route filtering as component of SUE path choice
#106 (review) sLTM/prototype - Add support for bounded (MNL) choice model
#109 (review) sLTM/prototype - Support path and OD output (adapters) for sLTM
#114 (review) sLTM/prototype - Support inflow and outflow as general link properties for output
#115 (review) sLTM/prototype - Support initial costs on sLTM
#116 (review) sLTM/prototype - Add support for non-linear free flow branch FDs for path based sLTM
#117 (review) sLTM/prototype - Let user configure Path Generator on sLTM settings for path based runs
#118 (review) sLTM/prototype - Make network loading gap epsilon configurable for sLTM
#121 (review) sLTM/prototype - Make it configurable to what sLTM network loading solution scheme we use at the start
#126 (review) sLTM/prototype - Add support for concave free flow branch FDs
#131 (review) sLTM/prototype - Support Bush (link) level outputs
#132 (review) sLTM/prototype - Support computation of dcost/dflow in sLTM to take turn level acceptance factors into account

**Bug fixes**

#113 Internal ids are not recreated nor reset consistently when same traffic assignment instance is rerun, this is fixed
#136 QL fundamental diagram dSpeedDFlow not calculated correctly, this has been fixed

## 0.4.0

**Enhancements**

* #95 updated to JUnit 5
* #93 replace clone() methods with shallowClone() and deepClone() and avoid Cloneable interface
* #91 Added service network and routed services support
* #89 Improvements to transfer zoning memory model
* #88 Support CI through github actions by building and testing upon push
* #89 Add support for conjugate directed graphs (edge-to-vertex-duals)
* #86 Add support for all-to-one shortest path search (Dijkstra)
* #84,#79,#78,#77,#76,#75,#74,#71,#70 Working on/preparing for sLTM implementation (not done)
* #83 Enhancements to Tampere node model
* #81 Architecture refinements/refactoring in code base
* #80 log all settings of all traffic assignment components if requested (Default should be true)
* #49 Centroids should not longer be a vertex in the network.

**Bug fixes**

* #92 Cannot copy reference of factories in ManagedIdEntities implementations in copy constructor
* #73 Breaking edges on directed graphs no longer correctly updates the edge segments

## 0.3.0

**enhancements**

* refactored infrastructure layers (container and network): parameterise it and introduce a new TopologicalLayer with configurable vertices,edges, edgesegments containers #47
* support stops, platforms and other PT related infrastructure in PLANit memory model via service network and transfer zone implementations (PLANitOSM/#8)
* modifications to network, zoning or other core components that have managedids, are now handled via a dedicated modifier classes to ensure ids remain consistent #51
* breaking links causes XML ids to no longer be unique and no option to fix this. Now we can add listeners to fix this on the flye in a generic way #55
* improved choices for default outputproperties on output configurations #6
* outputProperty LinkCost renamed to LinkSegmentCost #9
* outputProperty LinkType not correctly defined, should be segment based
* updated artifact id to conform with how this generally is setup, i.e. <application>-<subrepo> #57
* creation of paths now supported through PathBuilder like other id dependent components #58
* refactored the way we can track read input entities by their (original) source id when parsing and converting into PLANit entities via converter readers. this was not proper and has been improved #63
* added service network that can be defined on top of a physical network to represent service legs and service nodes #62
* added physical cost implementation based on free flow travel time only #64
* added norm based gap function #65
* linkSegmentTypes should not define speed limits per mode within the access element but via an accessgroup. This has been changed (planitxml/#20)
* initialCosts are not properly implemented. They require more flexibility (period agnostic/specific parsing, registration on assignment separate and again timeperiod specific or agnostic and unrelated to parsing) (planit/#68)
* added new PhysicalCost imlementation: SteadyStateTravelTimeCost based on Raadsen and Bliemer 2019 for sLTM #67
* added support for overriding units in output type configuration (planitpy2j/#10)
* parameters in fired create event for each traffic assignment component needs documentation (handled by creating dedicated classes) #16
* add support for user based setting of the used gapfunction (planitpy2j/#9)
* update packages to conform to new domain org.goplanit.* #72

**bug fixes**

* Tampere node model input is not properly setup for checking making it possible for asymmetric intersections to fail verification if correctly specified. this has been fixed #48
* Copy constructor of edge and vertex does not copy inputproperties -> should be deep copied by default. this has been fixed  #50
* When a mode is not supported on a link its cost now defaults to maxvalue and not infinity #52
* When a link is not accessible to a mode, we now use Double.MAX_VALUE as cost and not infinity because infinity cost results in NaN when multiplied with zero flow #53
* collecting link segments by startnode id in core is WRONG! This has been removed #2

## 0.2.0

**enhancements**
* rename everything related to routes to path in the memory model for consistency reasons (planit/#29)
* rename RotueIdType to PathIdType (planitpy2j/#1)
* results of shortest path calculations now provided in separate class ShortestPathResults (planit/#30)
* A* shortest path algorithm implementation now available (planit/#27)
* support for a overarching Graph interface/implementation as a basic building block for networks (planit/#31)
* move eLTM code from separate repository to core as it is an assignment method we want to provide out-of-the-box (planit/#32)
* add LICENSE.TXT to each repository so it is clearly licensed (planit/#33)
* edge segments,edges, and vertices of the physical network should be connected while creating them (not when creating the final transport network) (planit/#35)
* flows per mode map and modeData in traditionalstatictrafficassignment seem to overlap. Refactored (planit/#15)
* refactored the configuration of all traffic assignment components using configurators (planit/#4), only network, demands, and zoning are treated differently still. This avoids user access to internals of algorithm classes.
* traffic assignment builder delays building the traffic assignment until build is triggered (planit/#17)
* costs using interactor no longer use convoluted event mechanism, this is simplified (planit/#36)
* Add support for predefined modes and physical and usability features (planitxmlgenerator #2)
* Refactor outputmanager and outputconfiguration especially on setting it up, this was not done correctly (planit/#12)     
* add generic support for parsing/persisting networks and using PLANit as a converter between formats (planit/#37)
* add maximum speed to mode so we have an upperbound regardless of any link(segment(types)) (planit/#39)
* create a number of predefined default modes + add basic features to these modes (planit/#38)
* add support for creation and transformation of coordinate reference systems on networks and its geometry (planit/#41)
* move geometry from link to edge to be consistent with vertex and make it available at a lower level (planitutils/#2)
* internal id is long, xml id is string, and external id is also string now, with their own base implementation class (planitxmlgenerator/#6) 
* change default properties of output configuration to use XML_ID rather than EXTERNAL_ID (planit/#42)
* support intermodal trips by creating an additional layer for networks where we allow for separate infrastructure networks and interactions between these networks (planit/#43)  
* add support for custom PLANit modes in network (planit/#1) 

**bug fixes**
* max speed (without mode) on link segment was never placed in memory model, this is fixed (planit/#34)
* setting of max speed for a particular mode via mode properties (after initial creation) was ignored bug (planit/#46) 

## 0.1.0

* moved to new repository (www.github.com/trafficplanit/PLANit
* Create website (#21) - https://trafficplanit.github.io/PLANitManual
* Add third party licenses to setup process and releases, as well as website (#25)
* Ids generated for PLANit objects can now be grouped in a generic way by using IdTokens (#24)
* Tampere node model available as stand-alone functionality (#1)

## 0.0.4

* Logger created for each class as private static member (PLANit JIRA Task #63)
* Logger configuration revised (PLANit JIRA Task #59)
* External Ids refactored and treated differently when parsing (PLANit JIRA Task #54)
* Time periods and user classes now registered on a traffic assignment component (PLANit JIRA Task #61)
* Iterator implementation on memory outputformatter refactored (PLANit JIRA Task #8)
* Added output property PathId which should be added by default (PLANit JIRA Task #66)
* Simplified collection of the position of output key and value properties on memoryoutputformatter (PLANit JIRA Task #77)
* Check if needed and if so update all project readme.MDs based on new features available (PLANit JIRA Task #47)
* Removed toList() methodson inner classes of main inputs. Made them iterable instead (PLANit JIRA Task #83)
* Created Maximum Density input property and Density output property (PLANit JIRA Task #80)
* Created documentation for integration tests (PLANit JIRA Task #69)
* Python can call time period, mode and link segment type by external Id (PLANit JIRA Task #81)
* Added defaults for traditional static assignment components upon creation of the assignment (PLANit JIRA Task #79)

## 0.0.3

* Refactor setting of OutputKeys and OutputProperties in BaseOutputFormatter (PLANit JIRA Task #29)
* Activate ODSkimOutputTypes before saving OD skim data (PLANit JIRA Task #34)
* Refactor OutputAdapter and OutputTypeConfiguration (PLANit JIRA Task #35)
* Create OD Path output type adapter and output type configuration (PLANit JIRA Task #37)
* Link costs were an iteration behind the flow values (PLANit JIRA Task #38)
* Computation of costs is inefficient -> store costs used in iteration i as costs of i-1 (PLANit JIRA Task #40)
* Create Path object to store OD path of LinkSegment objects (PLANit JIRA Task #41)
* Allow options for Path outputs (PLANit JIRA Task #42)
* Create ODPathMatrix object (PLANit JIRA Task #43)
* TraditionalStaticAssignmentPathOutputTypeAdapter: Construction of Strings from paths in the wrong place (PLANit JIRA Task #45)
* Common functionality in collecting outputPropertyValues from outputtypeadapters should be common (PLANit JIRA Task #46)
* Maximum speed on LinkSegment should be stored by Mode object (PLANit JIRA Task #48)
* When activating an outputtype it should return the created outputtypeconfiguration (PLANit JIRA Task #49)
* When creating a new traffic assignment, we should only gain access to the builder and not the assignment object itself (PLANit JIRA Task #50)
* Opening and closing of outputformatters is a general task and not one for TraditionalStaticAssignment (PLANit JIRA Task #51)
* SpeedConnectoidTravelTimeCost default should not be infinite speed (PLANit JIRA Task #52) (now use FixedConnectoidTravelTimeCost instead)
* Iterator functionality required for MemoryOutputFormatter (PLANit JIRA Task#55)
* BPR parameters parsed in TNTP should not be stored on network in core (PLANit JIRA Task #62)

## 0.0.2

* Added OD OutputType to capture and record Skim matrices
* Added writing of Skim matrices to output formatters
* Common approach to adding and removing output properties from output formatters
* Some code refactored, particularly in BaseOutputFormatter and FileOutputFormatter to reuse common code for managing output between different output formatters
* Added new OutputProperty values in enum to use external Ids
* Moved setting of OutputKeys to loop through into OutputTypeConfiguration
* Moved common logic for writing output into BaseOutputFormatter

## 0.0.1

First Release