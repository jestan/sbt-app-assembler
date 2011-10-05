## What is sbt-app-assembler
Its a sbt plugin to assemble the application and generate start up scripts

Please note that it a direct copy of akka-sbt-plugin :)



## Requirements
 sbt 0.10.1

## Installation

 sbt 0.10.1

 Build the plugin in and publish it locally.

 Add the following lines to ~/.sbt/plugins/build.sbt or PROJECT_DIR/project/plugins/build.sbt

> libraryDependencies += "hms.sbt.plugins" %% "sbt-app-assembler" % "0.10.0"


Inject plugin settings into project in build.sbt:

> seq(appAssemblerSettings :_*)

> appAssemblerMainClass := "your app main class"

> appAssemblerJvmOptions := "-Xms1024M -Xmx1024M -Xss1M -XX:MaxPermSize=256M -XX:+UseParallelGC"


## Usage

  Use dist sbt task to create the application assembly