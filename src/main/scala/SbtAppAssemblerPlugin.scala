import _root_.sbt._
import sbt._
import Keys._
import Load.BuildStructure
import classpath.ClasspathUtilities
import Project.Initialize
import CommandSupport._

import java.io.File
import scala.collection.mutable.{Set => MutableSet}

object SbtAppAssemblerPlugin extends Plugin {

  case class DistConfig(outputDirectory: File,
                        configSourceDirs: Seq[File],
                        distJvmOptions: String,
                        distMainClass: String,
                        libFilter: File ⇒ Boolean,
                        additionalLibs: Seq[File])

  val Assemble = config("app-assembler") extend (Runtime)
  val dist = TaskKey[File]("assemble", "Builds the app assembly directory")
  val distClean = TaskKey[Unit]("clean", "Removes the app assembly directory")

  val outputDirectory = SettingKey[File]("app-assembler-output-directory")
  val configSourceDirs = TaskKey[Seq[File]]("app-assembler-conf-source-directories","Configuration files are copied from these directories")

  val appAssemblerJvmOptions = SettingKey[String]("app-assembler-jvm-options", "JVM parameters to use in start script")
  val appAssemblerMainClass = SettingKey[String]("app-assembler-main-class", "App main class to use in start script")

  val libFilter = SettingKey[File ⇒ Boolean]("app-assembler-lib-filter", "Filter of dependency jar files")
  val additionalLibs = TaskKey[Seq[File]]("app-assembleradditional-libs", "Additional dependency jar files")
  val distConfig = TaskKey[DistConfig]("app-assembler")

  val distNeedsPackageBin = dist <<= dist.dependsOn(packageBin in Compile)

  lazy val appAssemblerSettings: Seq[sbt.Project.Setting[_]] =
    inConfig(Assemble)(Seq(
      dist <<= packageBin.identity,
      packageBin <<= distTask,
      distClean <<= distCleanTask,
      dependencyClasspath <<= (dependencyClasspath in Runtime).identity,
      unmanagedResourceDirectories <<= (unmanagedResourceDirectories in Runtime).identity,
      outputDirectory <<= target / "dist",
      configSourceDirs <<= defaultConfigSourceDirs,
      libFilter := {
        f ⇒ true
      },
      additionalLibs <<= defaultAdditionalLibs,
      distConfig <<= (outputDirectory, configSourceDirs, appAssemblerJvmOptions, appAssemblerMainClass, libFilter, additionalLibs) map DistConfig)) ++
      Seq(dist <<= (dist in Assemble).identity, distNeedsPackageBin)

  private def distTask: Initialize[Task[File]] =
    (distConfig, sourceDirectory, crossTarget, dependencyClasspath, projectDependencies, allDependencies, buildStructure, state) map {
      (conf, src, tgt, cp, projDeps, allDeps, buildStruct, st) ⇒

        if (isKernelProject(allDeps)) {
          val log = logger(st)
          val distBinPath = conf.outputDirectory / "bin"
          val distConfigPath = conf.outputDirectory / "conf"
          val distLibPath = conf.outputDirectory / "lib"

          val subProjectDependencies: Set[SubProjectInfo] = allSubProjectDependencies(projDeps, buildStruct, st)

          log.info("Creating distribution %s ..." format conf.outputDirectory)
          IO.createDirectory(conf.outputDirectory)
          Scripts(conf.distJvmOptions, conf.distMainClass).writeScripts(distBinPath)
          copyDirectories(conf.configSourceDirs, distConfigPath)
          copyJars(tgt, distLibPath)

          copyFiles(libFiles(cp, conf.libFilter), distLibPath)
          copyFiles(conf.additionalLibs, distLibPath)
          for (subTarget <- subProjectDependencies.map(_.target)) {
            copyJars(subTarget, distLibPath)
          }
          log.info("Distribution created.")
        }
        conf.outputDirectory
    }

  private def distCleanTask: Initialize[Task[Unit]] =
    (outputDirectory, allDependencies, streams) map {
      (outDir, deps, s) ⇒

        if (isKernelProject(deps)) {
          val log = s.log
          log.info("Cleaning " + outDir)
          IO.delete(outDir)
        }
    }

  def isKernelProject(dependencies: Seq[ModuleID]): Boolean = true

  private def defaultConfigSourceDirs = (sourceDirectory, unmanagedResourceDirectories) map {
    (src, resources) ⇒
      Seq(src / "conf", src / "main" / "conf") ++ resources
  }

  private def defaultAdditionalLibs = (libraryDependencies) map {
    (libs) ⇒
      Seq.empty[File]
  }

  private case class Scripts(jvmOptions: String, mainClass: String) {

    def writeScripts(to: File) = {
      scripts.map {
        script ⇒
          val target = new File(to, script.name)
          IO.write(target, script.contents)
          setExecutable(target, script.executable)
      }.foldLeft(None: Option[String])(_ orElse _)
    }

    private case class DistScript(name: String, contents: String, executable: Boolean)

    private def scripts = Set(DistScript("start", distShScript, true), DistScript("start.bat", distBatScript, true))

    private def distShScript =
      """|#!/bin/sh
    |
    |APP_HOME="$(cd "$(cd "$(dirname "$0")"; pwd -P)"/..; pwd)"
    |APP_CLASSPATH="$APP_HOME/lib/*:$APP_HOME/conf"
    |JAVA_OPTS="%s"
    |
    |java $JAVA_OPTS -cp "$APP_CLASSPATH" -Dapp.home="$APP_HOME" %s
    |""".stripMargin.format(jvmOptions, mainClass)

    private def distBatScript =
      """|@echo off
    |set APP_HOME=%%~dp0..
    |set APP_CLASSPATH=%%APP_HOME%%\lib\*;%%APP_HOME%%\conf
    |set JAVA_OPTS=%s
    |
    |java %%JAVA_OPTS%% -cp "%%APP_CLASSPATH%%" -Dapp.home="%%APP_HOME%%" %s
    |""".stripMargin.format(jvmOptions, mainClass)

    private def setExecutable(target: File, executable: Boolean): Option[String] = {
      val success = target.setExecutable(executable, false)
      if (success) None else Some("Couldn't set permissions of " + target)
    }
  }

  private def copyDirectories(fromDirs: Seq[File], to: File) = {
    IO.createDirectory(to)
    for (from ← fromDirs) {
      IO.copyDirectory(from, to)
    }
  }

  private def copyJars(fromDir: File, toDir: File) = {
    val jarFiles = fromDir.listFiles.filter(f ⇒
      f.isFile &&
        f.name.endsWith(".jar") &&
        !f.name.contains("-sources") &&
        !f.name.contains("-docs"))

    copyFiles(jarFiles, toDir)
  }

  private def copyFiles(files: Seq[File], toDir: File) = {
    for (f ← files) {
      IO.copyFile(f, new File(toDir, f.getName))
    }
  }

  private def libFiles(classpath: Classpath, libFilter: File ⇒ Boolean): Seq[File] = {
    val (libs, directories) = classpath.map(_.data).partition(ClasspathUtilities.isArchive)
    libs.map(_.asFile).filter(libFilter)
  }

  private def allSubProjectDependencies(projDeps: Seq[ModuleID], buildStruct: BuildStructure, state: State): Set[SubProjectInfo] = {
    val buildUnit = buildStruct.units(buildStruct.root)
    val uri = buildStruct.root
    val allProjects = buildUnit.defined.map {
      case (id, proj) => (ProjectRef(uri, id) -> proj)
    }

    val projDepsNames = projDeps.map(_.name)
    def include(project: ResolvedProject): Boolean = projDepsNames.exists(_ == project.id)
    val subProjects: Seq[SubProjectInfo] = allProjects.collect {
      case (projRef, project) if include(project) => projectInfo(projRef, project, buildStruct, state, allProjects)
    }.toList

    val allSubProjects = subProjects.map(_.recursiveSubProjects).flatten.toSet
    allSubProjects
  }

  private def projectInfo(projectRef: ProjectRef, project: ResolvedProject, buildStruct: BuildStructure, state: State,
                          allProjects: Map[ProjectRef, ResolvedProject]): SubProjectInfo = {

    def optionalSetting[A](key: ScopedSetting[A]) = key in projectRef get buildStruct.data

    def setting[A](key: ScopedSetting[A], errorMessage: => String) = {
      optionalSetting(key) getOrElse {
        logger(state).error(errorMessage);
        throw new IllegalArgumentException()
      }
    }

    def evaluateTask[T](taskKey: sbt.Project.ScopedKey[sbt.Task[T]]) = {
      EvaluateTask.evaluateTask(buildStruct, taskKey, state, projectRef, false, EvaluateTask.SystemProcessors)
    }

    val projDeps: Seq[ModuleID] = evaluateTask(Keys.projectDependencies) match {
      case Some(Value(moduleIds)) => moduleIds
      case _ => Seq.empty
    }

    val projDepsNames = projDeps.map(_.name)
    def include(project: ResolvedProject): Boolean = projDepsNames.exists(_ == project.id)
    val subProjects = allProjects.collect {
      case (projRef, proj) if include(proj) => projectInfo(projRef, proj, buildStruct, state, allProjects)
    }.toList

    val target = setting(Keys.crossTarget, "Missing crossTarget directory")
    SubProjectInfo(project.id, target, subProjects)
  }

  private case class SubProjectInfo(id: String, target: File, subProjects: Seq[SubProjectInfo]) {

    def recursiveSubProjects: Set[SubProjectInfo] = {
      val flatSubProjects = for {
        x <- subProjects
        y <- x.recursiveSubProjects
      } yield y

      flatSubProjects.toSet + this
    }

  }

}

