## What is sbt-app-assembler
Its a sbt plugin to assemble the application and generate start up scripts

Please note that it a direct copy of akka-sbt-plugin :)



## Requirements
 sbt 0.11.x

## Installation

 sbt 0.11.x

 Build the plugin in and publish it locally.

 Add the following lines to plugins.sbt

> addSbtPlugin("hms.sbt.plugins" %% "sbt-app-assembler" % "0.1.1-SNAPSHOT")


Inject plugin settings into project in build.sbt:

> import SbtAppAssemblerPlugin._

> seq(appAssemblerSettings :_*)

> appAssemblerMainClass := "your app main class"

> appAssemblerJvmOptions := "-Xms1024M -Xmx1024M -Xss1M -XX:MaxPermSize=256M -XX:+UseParallelGC"


## Usage

  > ./sbt dist
