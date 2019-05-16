# PLANit

This is the core module of the PLANit project. It contains all the algorithms and computational components which can be used to construct projects, traffic assignments, etc. The PLANit project promotes the use of its native I/O formats which are XML based, however it is equally well possible to define your own input format and/or poutput format. PLANit it is completely modular and open such that you can replace, add, include, or exclude modules any way you please. 

For more information on the natively supported XML formats we kindly refer to <https://github.sydney.edu.au/PLANit/PLANitXML>

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







