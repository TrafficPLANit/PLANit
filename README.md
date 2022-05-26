# PLANit

![Master Branch](https://github.com/TrafficPLANit/PLANit/actions/workflows/maven_master.yml/badge.svg?branch=master)
![Develop Branch](https://github.com/TrafficPLANit/PLANit/actions/workflows/maven_develop.yml/badge.svg?branch=develop)

This is the core module of the PLANit project. It contains algorithms and traffic assignment components which can be used to construct projects, traffic assignments, etc. The PLANit project promotes the use of its native I/O format which are CSV/XML based, however it is equally well possible to define your own input format and/or output format. PLANit it is completely modular and open such that you can replace, add, include, or exclude modules any way you please. 

For more information on PLANit such as the user the manual, licensing, installation, getting started, reference documentation, and more, please visit [www.goPLANit.org](http://www.goplanit.org)

### Maven build

PLANit core has the following PLANit specific dependencies (See pom.xml):

* planit-parentpom
* planit-utils

Dependencies (except parent-pom) will be automatically downloaded from the PLANit website, (www.repository.goplanit.org)[https://repository.goplanit.org], or alternatively can be checked-out locally for local development. The shared PLANit Maven configuration can be found in planit-parent-pom which is defined as the parent pom of each PLANit repository.

### Maven deploy

Distribution management is setup via the parent pom such that Maven deploys this project to the PLANit online repository (also specified in the parent pom). To enable deployment ensure that you setup your credentials correctly in your settings.xml as otherwise the deployment will fail.

### Git Branching model

We adopt GitFlow as per https://nvie.com/posts/a-successful-git-branching-model/









