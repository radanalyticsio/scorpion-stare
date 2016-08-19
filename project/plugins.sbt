resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
    url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
        Resolver.ivyStylePatterns)

resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.2.1")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")
