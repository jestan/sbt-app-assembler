
sbtPlugin := true

organization := "hms.sbt.plugin"

name := "sbt-app-assembler"

version := "0.1.1"

publishMavenStyle := true

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials-release")

publishTo <<= (version) { version: String =>
  val repo = "http://192.168.0.7:8080/archiva/repository/"
  if (version.trim.endsWith("SNAPSHOT"))
    Some("Repository Archiva Managed snapshots Repository" at repo + "snapshots/")
  else
    Some("Repository Archiva Managed internal Repository" at repo + "internal/")
}


