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