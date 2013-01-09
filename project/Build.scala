import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appName         = "simple-dam"
  val appVersion      = "0.3.0"

  val appDependencies = Seq(
    "org.mockito" % "mockito-core" % "1.9.0",
    "org.scala-lang" % "scala-compiler" % "2.9.1"
  )


  // gzip all assets that we can
  val gzippableAssets = SettingKey[PathFinder]("gzippable-assets", "Defines the files to gzip")
  val gzipAssets = TaskKey[Seq[File]]("gzip-assets", "gzip all assets")

  lazy val gzipAssetsSetting = gzipAssets <<= gzipAssetsTask dependsOn (copyResources in Compile)
  lazy val gzipAssetsTask = (gzippableAssets, streams) map {
    case (finder: PathFinder, s: TaskStreams) => {
      var count = 0
      var files = finder.get.map { file =>
        val gzTarget = new File(file.getAbsolutePath + ".gz")
        IO.gzip(file, gzTarget)
        count += 1;
        gzTarget
      }
      s.log.info("Compressed " + count + " asset(s)")
      files
    }
  }

  
  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      gzippableAssets <<= (classDirectory in Compile)(dir => (dir ** ("*.js" || "*.css" || "*.html"))),
      gzipAssetsSetting,
      playPackageEverything <<= playPackageEverything dependsOn gzipAssets
  )

}
