import sbt._

name := "nppush"

version := "0.0.1"

scalaVersion := "2.11.5"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= Seq(
    "com.typesafe"              %  "config"            % "1.2.1",
    "org.scala-lang.modules"    %%  "scala-xml"         % "1.0.3",
    "io.spray"                  %%  "spray-client"      % "1.3.2",
    "io.spray"                  %%  "spray-json"        % "1.3.1",
    "com.typesafe.akka"         %%  "akka-actor"        % "2.3.6",
    "com.typesafe.akka"         %%  "akka-testkit"      % "2.3.6"   % "test",
    "org.specs2"                %%  "specs2-core"       % "2.3.11"  % "test"
  )
