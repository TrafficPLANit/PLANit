# BasicCsv

## 0.0.1

First Release

## 0.0.2

* Added OD OutputType to capture and record Skim matrices
* Added writing of Skim matrices to output formatters
* Common approach to adding and removing output properties from output formatters
* Some code refactored, particularly in BaseOutputFormatter and FileOutputFormatter to reuse common code for managing output between different output formatters

# MetroScan

## 0.0.1

First Release

## 0.0.2

* Bug Fix: There was an error in the setting of link speed and capacity properties.  Only one LinkSegmentType object was being defined, and assigned to all links.  Now each LinkSegment is given its own LinkSegmentType, which takes values from the network input JSON file.
* Bug Fix: The default values for alpha and beta in the BPR function were wrong.  These have now been set to the correct values (alpha=0.87, beta=4.0).
* Added OUTPUTTIMEUNIT to argument list to allow user to specify whether output link costs should be presented in hours, minutes or seconds.
* Created demandOneMode.json input file, which only uses Mode 1.  This generates the output file results.csv, which is also included as an example output.
* Added OD OutputType to capture and record Skim matrices
* Added writing of Skim matrices to output formatters
* Common approach to adding and removing output properties from output formatters
* Some code refactored, particularly in BaseOutputFormatter and FileOutputFormatter to reuse common code for managing output between different output formatters

## 0.0.3

* Bug Fix: Demands JSON file now correctly read in by MetroScan (MetroScan JIRA Task #7)

# PLANit

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
* Create Path object to store OD path of LinkSegment objects (PLANit JIRA Task #41)
* Computation of costs is inefficient -> store costs used in iteration i as costs of i-1 (PLANit JIRA Task #40)
* Allow options for Path outputs (PLANit JIRA Task #42)
* Create ODPathMatrix object (PLANit JIRA Task #43)
* Opening and closing of outputformatters is a general task and not one for TraditionalStaticAssignment (PLANit JIRA Task #51)
* When creating a new traffic assignment, we should only gain access to the builder and not the assignment object itself (PLANit JIRA Task #50)
* When activating an outputtype it should return the created outputtypeconfiguration (PLANit JIRA Task #49)
* Common functionality in collecting outputPropertyValues from outputtypeadapters should be common (PLANit JIRA Task #46)
* TraditionalStaticAssignmentPathOutputTypeAdapter: Construction of Strings from paths in the wrong place (PLANit JIRA Task #45)
* Maximum speed on LinkSegment should be stored by Mode object (PLANit JIRA Task #48)
* SpeedConnectoidTravelTimeCost default should not be infinite speed (PLANit JIRA Task #52) (now use FixedConnectoidTravelTimeCost instead)

# PLANitIO

## 0.0.1

First Release

## 0.0.2

* Renamed PLANitXML to PLANitIO
* Refactored package names 
* Added OD OutputType to capture and record Skim matrices
* Added writing of Skim matrices to output formatters
* Common approach to adding and removing output properties from output formatters
* Some code refactored, particularly in BaseOutputFormatter and FileOutputFormatter to reuse common code for managing output between different output formatters
* Updated standard results files in unit tests to match updated output property names

## 0.0.3

* Restructure resources directory to create easier to understand hierarchy (PLANitIO JIRA Task #1)