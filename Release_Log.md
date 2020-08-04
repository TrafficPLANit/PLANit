# Release Log

PlanIt Releases

This project contains core code which is used by all the interface projects.

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