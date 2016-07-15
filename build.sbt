organization := "com.redhat.daikon"

version := "0.0.1"

scalaVersion := "2.10.5"

crossScalaVersions := Seq("2.10.5", "2.11.8")

licenses += ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0"))

def commonSettings = Seq(
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
  scalacOptions in (Compile, doc) ++= Seq("-doc-root-content", baseDirectory.value+"/root-doc.txt"),
  seq(bintraySettings:_*),
  seq(bintrayPublishSettings:_*)
)

lazy val core = project.in(file("core"))
  .settings(commonSettings:_*)
  .settings(
    name := "core"
    )

lazy val oshinko = project.in(file("oshinko"))
  .dependsOn(core)
  .settings(commonSettings:_*)
  .settings(
    name := "oshinko"
    )
