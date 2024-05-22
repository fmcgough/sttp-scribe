// code style
addSbtPlugin("de.heikoseeberger" % "sbt-header"   % "5.10.0")
addSbtPlugin("ch.epfl.scala"     % "sbt-scalafix" % "0.12.1")

// testing
addSbtPlugin("org.scoverage" % "sbt-scoverage"   % "2.0.12")
addSbtPlugin("com.typesafe"  % "sbt-mima-plugin" % "1.1.3")

// artifact publishing
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.10.0")
addSbtPlugin("com.github.sbt" % "sbt-pgp"      % "2.2.1")
addSbtPlugin("com.github.sbt" % "sbt-release"  % "1.4.0")
