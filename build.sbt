name := "gatling-load-testing"

version := "0.1"
scalaVersion := "2.12.8"

enablePlugins(GatlingPlugin)

scalacOptions := Seq(
  "-encoding", "UTF-8", "-target:jvm-1.8", "-deprecation",
  "-feature", "-unchecked", "-language:implicitConversions", "-language:postfixOps")
libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.1.2" % "test,it"
libraryDependencies += "io.gatling"            % "gatling-test-framework"    % "3.1.2" % "test,it"
libraryDependencies += "com.google.code.gson" % "gson" % "1.7.1"
