import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appName         = "simple-dam"
  val appVersion      = "0.1.0"

  val appDependencies = Seq(
    // "org.squeryl" %% "squeryl" % "0.9.5-2",
    // "postgresql" % "postgresql" % "9.1-901.jdbc4",
    "org.mockito" % "mockito-core" % "1.9.0"
  )

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    // Add your own project settings here      
  )

}
