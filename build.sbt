name := "recogito"

version := "0.0.2"

play.Project.playScalaSettings

resolvers += "Open Source Geospatial Foundation Repository" at "http://download.osgeo.org/webdav/geotools/"

libraryDependencies ++= Seq(jdbc, cache)     

/** Runtime dependencies **/
libraryDependencies ++= Seq(
  "com.google.gdata" % "core" % "1.47.1",
  "org.apache.lucene" % "lucene-analyzers-common" % "4.4.0",
  "org.apache.lucene" % "lucene-queryparser" % "4.4.0",
  "org.jsoup" % "jsoup" % "1.7.2",
  "com.typesafe.play" %% "play-slick" % "0.5.0.8",
  "org.xerial" % "sqlite-jdbc" % "3.7.2"  
)

/** Transient dependencies required by Scalagios
  *
  * TODO: remove once Scalagios is included as managed dependency!
  */
libraryDependencies ++= Seq(
  "org.openrdf.sesame" % "sesame-rio-n3" % "2.7.5",
  "org.openrdf.sesame" % "sesame-rio-rdfxml" % "2.7.5",
  "com.vividsolutions" % "jts" % "1.13",
  "org.geotools" % "gt-geojson" % "10.0"
)

requireJs += "georesolution.js"

requireJsShim += "build.js"
