# Release Log

PlanIt Releases

This project contains core code which is used by all the interface projects.

## 0.0.1

First Release

## 0.0.2

* Added OD OutputType to capture and record Skim matrices
* Added writing of Skim matrices to output formatters
* Common approach to adding and removing output properties from output formatters
* Some code refactored, particularly in BaseOutputFormatter and FileOutputFormatter to reuse common code for managing output between different output formatters
* Added new OutputProperty values in enum to use external Ids
* Moved setting of OutputKeys to loop through into OutputTypeConfiguration
* Moved common logic for writing output into BaseOutputFormatter

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