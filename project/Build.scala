import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "stegosaur"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "com.twitter"        %% "finagle-core"              % "6.3.0",
    "com.twitter"        %% "finagle-http"              % "6.3.0",
    jdbc,
    anorm
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers += "twitter" at "http://maven.twttr.com/"
  )

}
