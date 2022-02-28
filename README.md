# PLANit

This is the core module of the PLANit project. It contains algorithms and traffic assignment components which can be used to construct projects, traffic assignments, etc. The PLANit project promotes the use of its native I/O format which are CSV/XML based, however it is equally well possible to define your own input format and/or output format. PLANit it is completely modular and open such that you can replace, add, include, or exclude modules any way you please. 

For more information on PLANit such as the user the manual, licensing, installation, getting started, reference documentation, and more, please visit [www.goPLANit.org](http://www.goplanit.org)


## Development

### Maven build

PLANit core has the following PLANit specific dependencies (See pom.xml):

* planit-parentpom
* planit-utils

Dependencies (except parent-pom) will be automatically downloaded from the PLANit website, (www.repository.goplanit.org)[https://repository.goplanit.org], or alternatively can be checked-out locally for local development. The shared PLANit Maven configuration can be found in planit-parent-pom which is defined as the parent pom of each PLANit repository.

Since the repo depends on the parent-pom to find its (shared) repositories, we must let Maven find the parent-pom first, either:

* localy clone the parent pom repo and run mvn install on it before conducting a Maven build, or
* add the parent pom repository to your maven (user) settings.xml by adding it to a profile like the following

```xml
  <profiles>
    <profile>
      <activation>
        <property>
          <name>!skip</name>
        </property>
      </activation>
    
      <repositories>
        <repository>
          <id>planit-repository.goplanit.org</id>
          <name>PLANit Repository</name>
          <url>http://repository.goplanit.org</url>
        </repository>     
      </repositories>
    </profile>
  </profiles>
```

### Maven deploy

Distribution management is setup via the parent pom such that Maven deploys this project to the PLANit online repository (also specified in the parent pom). To enable deployment ensure that you setup your credentials correctly in your settings.xml as otherwise the deployment will fail.

### Git Branching model

We adopt GitFlow as per https://nvie.com/posts/a-successful-git-branching-model/









