
sbtPlugin := true

organization := "hms.sbt.plugins"

name := "sbt-app-assembler"

version := "0.1.1"

publishMavenStyle := true
      
publishTo <<= (version) {version: String =>
    val repo = "http://192.168.0.7:8080/archiva/repository/"
    if (version.trim.endsWith("SNAPSHOT"))
       Some("Archiva Managed snapshots Repository" at repo + "snapshots/")
    else
       Some("Archiva Managed internal Repository" at repo + "internal/")
    }

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials-release")

