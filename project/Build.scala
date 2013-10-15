import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "log-analyzer"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    javaCore,
    javaJdbc,
    javaEbean,
    "commons-io" % "commons-io" % "2.4",
    "org.mongodb" % "mongo-java-driver" % "2.10.1",
    "com.google.inject" % "guice" % "3.0",
    "org.apache.httpcomponents" % "httpclient" % "4.2",
    "org.apache.commons" % "commons-email" % "1.3.1",
    "org.quartz-scheduler" % "quartz" % "2.1.7",
    "commons-lang" % "commons-lang" % "2.6"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
