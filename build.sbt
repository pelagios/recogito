name := "pelagios-georesolution-correction-tool"

version := "0.0.1"

scalaVersion := "2.10.0"

resolvers += "Open Source Geospatial Foundation Repository" at "http://download.osgeo.org/webdav/geotools/"

resolvers += "Mandubian repository releases" at "https://github.com/mandubian/mandubian-mvn/raw/master/snapshots/"
  
/** Runtime dependencies **/
libraryDependencies ++= Seq(
  "com.google.gdata" % "core" % "1.47.1",
  "net.databinder" % "unfiltered-filter_2.10" % "0.6.7",
  "net.databinder" % "unfiltered-jetty_2.10" % "0.6.7",
  "org.apache.lucene" % "lucene-analyzers-common" % "4.4.0",
  "org.apache.lucene" % "lucene-queryparser" % "4.4.0",
  "org.slf4j" % "slf4j-simple" % "1.7.5",
  "org.jsoup" % "jsoup" % "1.7.2",
  "play" %% "play-json" % "2.2-SNAPSHOT"          
)

/** Test dependencies **/
libraryDependencies ++= Seq(
  "junit" % "junit" % "4.11" % "test",
  "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"
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
