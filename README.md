# PLANit

This is the core module of the PLANit project. It contains algorithms and traffic assignment components which can be used to construct projects, traffic assignments, etc. The PLANit project promotes the use of its native I/O format which are CSV/XML based, however it is equally well possible to define your own input format and/or output format. PLANit it is completely modular and open such that you can replace, add, include, or exclude modules any way you please. 

For more information on PLANit such as the user the manual, licensing, installation, getting started, reference documentation, and more, please visit [www.goPLANit.org](http://www.goplanit.org)


## Development

### Maven build 

Projects need to be built from Maven before they can be run. The common maven configuration can be found in the PLANitParentPom project which acts as the parent for this project's pom.xml.

> Make sure you install the PLANitParentPom pom.xml before conducting a maven build (in Eclipse) on this project, otherwise it cannot find the references dependencies, plugins, and other resources.

### Maven deploy

Distribution management is setup via the parent pom such that Maven deploys this project to the PLANit online repository (also specified in the parent pom). To enable deployment ensure that you setup your credentials correctly in your settings.xml as otherwise the deployment will fail.

### Git Branching model

We adopt GitFlow as per https://nvie.com/posts/a-successful-git-branching-model/









