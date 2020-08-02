# PLANit

This is the core module of the PLANit project. It contains algorithms and traffic assignment components which can be used to construct projects, traffic assignments, etc. The PLANit project promotes the use of its native I/O format which are CSV/XML based, however it is equally well possible to define your own input format and/or output format. PLANit it is completely modular and open such that you can replace, add, include, or exclude modules any way you please. 

For more information on PLANit such as the user the manual, licensing, installation, getting started, reference documentation, and more, please visit [https://trafficplanit.github.io/PLANitManual/](https://trafficplanit.github.io/PLANitManual/)

## Maven parent

Projects need to be built from Maven before they can be run. The common maven configuration can be found in the PLANitAll project which acts as the parent for this project's pom.xml.

> Make sure you install the PLANitAll pom.xml before conducting a maven build (in Eclipse) on this project, otherwise it cannot find the references dependencies, plugins, and other resources.

## Maven and Eclipse - a quick introduction

The following notes explain how to run Maven builds for these projects in Eclipse.  They are aimed at readers who are not familiar with Maven.  Readers who are experienced in Maven or other IDEs are free to do their own configuration.

Firstly ensure that you are using a version of Eclipse which has the Maven plugin built in.  Most current versions of Eclipse include Maven.  If you are unsure, select File/New/Other.. and look at the list of available wizards which appears.  If there is a folder called "Maven" which includes a link called "Maven Project", Maven is included.

Right-click on the PLANit project in the Package Explorer and select Run As.  You will see a drop-down menu.  Often you can just click "Maven Install" and it will work, since it performs the following actions:-

* Collects dependencies
* Compiles the Java code;
* Runs the unit tests.

The results of the unit tests appear in the Console.  The "BUILD SUCCESS" message only appears if all the unit tests pass, which is usually what you want.

However there may be times when you do not wish to perform all these steps at once.  For example, you may have made some temporary changes which cause the unit tests to fail, and you just want to compile the code without them.  

The drop-down menu has other useful options, including:-

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










