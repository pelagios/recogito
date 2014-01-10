package models

import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

/** Geospatial Document case class.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
case class GeoDocument(id: Option[Int] = None, title: String, description: Option[String] = None, source: Option[String] = None)

/** Geospatial Documents database table **/
object GeoDocuments extends Table[GeoDocument]("gdocuments") {
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def title = column[String]("title")
  
  def description = column[String]("description", O.Nullable)
  
  def source = column[String]("source", O.Nullable)
  
  def * = id.? ~ title ~ description.? ~ source.? <> (GeoDocument.apply _, GeoDocument.unapply _)
  
  def listAll()(implicit s: Session): Seq[GeoDocument] = Query(GeoDocuments).list
  
  def findById(id: Int)(implicit s: Session): Option[GeoDocument] =
    Query(GeoDocuments).where(_.id === id).firstOption
  
}