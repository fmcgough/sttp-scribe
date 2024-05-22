name         := "sttp-scribe"
organization := "software.purpledragon"

scalaVersion       := "2.13.14"
crossScalaVersions := Seq(scalaVersion.value, "2.11.12", "2.12.19")

libraryDependencies ++= Seq(
  "org.slf4j"                     % "slf4j-api"               % "1.7.36",
  "com.softwaremill.sttp.client" %% "core"                    % "2.3.0",
  "com.github.scribejava"         % "scribejava-apis"         % "6.9.0",
  "org.scala-lang.modules"       %% "scala-collection-compat" % "2.12.0",
  "com.github.bigwheel"          %% "util-backports"          % "2.1",
  "org.scalatest"                %% "scalatest"               % "3.2.18" % Test,
  "org.scalamock"                %% "scalamock"               % "5.2.0"  % Test,
  "commons-io"                    % "commons-io"              % "2.16.1" % Test,
  "org.apache.commons"            % "commons-lang3"           % "3.14.0" % Test,
  "ch.qos.logback"                % "logback-classic"         % "1.3.14" % Test
)

scalacOptions ++= {
  if (scalaVersion.value.startsWith("2.13")) {
    Seq("-Wunused:imports", "-deprecation")
  } else {
    Seq("-Ypartial-unification", "-Ywarn-unused-import", "-deprecation")
  }
}

semanticdbEnabled := true
semanticdbVersion := scalafixSemanticdb.revision

headerLicense        := Some(HeaderLicense.ALv2("2018", "Michael Stringer"))
licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0"))
developers           := List(
  Developer("stringbean", "Michael Stringer", "@the_stringbean", url("https://github.com/stringbean"))
)
organizationName     := "Purple Dragon Software"
organizationHomepage := Some(url("https://purpledragon.software"))
homepage             := Some(url("https://github.com/stringbean/sttp-scribe"))
scmInfo              := Some(
  ScmInfo(url("https://github.com/stringbean/sttp-scribe"), "https://github.com/stringbean/sttp-scribe.git")
)
publishTo            := sonatypePublishToBundle.value

import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

releaseCrossBuild             := true
releasePublishArtifactsAction := PgpKeys.publishSigned.value

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  releaseStepCommand("scalafmtCheckAll"),
  releaseStepCommand("headerCheckAll"),
  releaseStepCommand("scalafixAll --check"),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

mimaPreviousArtifacts := Set("software.purpledragon" %% "sttp-scribe" % "2.0.1")
