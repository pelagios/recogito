package models

import play.api.Play.current
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import models.stats.GeoDocumentStats

/** Geospatial Document case class.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
case class GeoDocument(
    
    /** Id **/
    id: Option[Int], 
    
    /** An external (URI) identifier for the 'Work' (in terms of FRBR terminology) **/
    externalWorkID: Option[String],
    
    /** Author (if known) **/
    author: Option[String], 
    
    /** Title **/
    title: String, 
    
    /** Year the document was dated (if known) - will be used for sorting **/
    date: Option[Int],
    
    /** Additional free-text date comment (e.g. "middle of 3rd century") - will be used for display **/
    dateComment: Option[String],
    
    /** Document language **/
    language: Option[String],
    
    /** Free-text description **/
    description: Option[String] = None, 
    
    /** Online or bibliographic source from where the text was obtained **/
    source: Option[String] = None) extends GeoDocumentStats

/** Geospatial Documents database table **/
object GeoDocuments extends Table[GeoDocument]("gdocuments") {
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def externalWorkID = column[String]("ext_work_id", O.Nullable)
  
  def author = column[String]("author", O.Nullable)
  
  def title = column[String]("title")
  
  def date = column[Int]("date", O.Nullable)
  
  def dateComment = column[String]("date_comment", O.Nullable)
  
  def language = column[String]("language", O.Nullable)

  def description = column[String]("description", O.Nullable)
  
  def source = column[String]("source", O.Nullable)
  
  def _collections = column[String]("collections", O.Nullable)

  def * = id.? ~ externalWorkID.? ~ author.? ~ title ~ date.? ~ dateComment.? ~ language.? ~ 
    description.? ~ source.? <> (GeoDocument.apply _, GeoDocument.unapply _)
  
  def listAll()(implicit s: Session): Seq[GeoDocument] = Query(GeoDocuments).list
  
  def findById(id: Int)(implicit s: Session): Option[GeoDocument] =
    Query(GeoDocuments).where(_.id === id).firstOption
    
  def findAll(ids: Seq[Int])(implicit s: Session): Seq[GeoDocument] =
    Query(GeoDocuments).where(_.id inSet ids).list
    
  def delete(id: Int)(implicit s: Session) =
    Query(GeoDocuments).where(_.id === id).delete
    
  /** Helper method to find all IDs except those provided as argument.
    *
    * This method is used by the CollectionMembership class to determine
    * documents that are not assigned to a collection. To avoid confusion,
    * the method is not exposed outside of this package.  
    */
  private[models] def findAllExcept(ids: Seq[Int])(implicit s: Session): Seq[Int] =
    Query(GeoDocuments).map(_.id).filter(id => !(id inSet ids)).list 
  
}