
sbtPlugin := true

organization := "hms.sbt.plugins"

name := "sbt-app-assembler"

version := "0.1.1-SNAPSHOT"

scalaVersion := "2.10.0"

scalacOptions += "-deprecation"

scalacOptions += "-unchecked"

publishMavenStyle := true

publishTo <<= (version) { version: String =>
  val repo = "http://192.168.0.7:8080/archiva/repository/"
  if (version.trim.endsWith("SNAPSHOT"))
    Some("Repository Archiva Managed snapshots Repository" at repo + "snapshots/")
  else
    Some("Repository Archiva Managed internal Repository" at repo + "internal/")
}

credentials += Credentials("Repository Archiva Managed internal Repository", "192.168.0.7", "xxx", "xxx")

credentials += Credentials("Repository Archiva Managed snapshots Repository", "192.168.0.7", "xxx", "xxx")

