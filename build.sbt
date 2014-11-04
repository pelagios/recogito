name := "recogito"

version := "0.0.2"

play.Project.playScalaSettings

resolvers += "Open Source Geospatial Foundation Repository" at "http://download.osgeo.org/webdav/geotools/"

libraryDependencies ++= Seq(jdbc, cache)     

/** Runtime dependencies **/
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "0.6.1",
  "commons-codec" % "commons-codec" % "1.9",
  "org.xerial" % "sqlite-jdbc" % "3.7.2",
  "postgresql" % "postgresql" % "9.1-901.jdbc4",
  "org.zeroturnaround" % "zt-zip" % "1.8",
  "rome" % "rome" % "1.0"          
)

/** Transient dependencies required by Scalagios
  *
  * TODO: remove once Scalagios is included as managed dependency!
  */
libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-analyzers-common" % "4.9.0",
  "org.apache.lucene" % "lucene-queryparser" % "4.9.0",
  "org.openrdf.sesame" % "sesame-rio-n3" % "2.7.5",
  "org.openrdf.sesame" % "sesame-rio-rdfxml" % "2.7.5",
  "com.vividsolutions" % "jts" % "1.13",
  "org.geotools" % "gt-geojson" % "10.0"
)

requireJs ++= Seq("georesolution.js", "imageannotation.js", "imageview.js")

requireJsShim += "build.js"

