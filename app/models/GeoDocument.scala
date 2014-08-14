package models

import models.content._
import models.stats.GeoDocumentStats
import play.api.Play.current
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag

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
    source: Option[String] = None,
    
    /** Online resources that contain additional information about the document (Wikipedia page, etc.) **/ 
    private val _primaryTopicOf: Option[String],
    
    /** The geographical origin of the source (gazetteer URI) **/
    origin: Option[String] = None,
    
    /** The findspot of the document or source (gazetteer URI) **/
    findspot: Option[String] = None,
    
    /** A geographical location associated with the author (gazetteer URI) **/
    authorLocation: Option[String]) extends GeoDocumentStats {
  
  /** Wraps the comma-separated URL list to a proper Seq **/
  val primaryTopicOf = _primaryTopicOf.map(_.split(",").toSeq.map(_.trim)).getOrElse(Seq.empty[String])
  
}

/** Geospatial Documents database table **/
class GeoDocuments(tag: Tag) extends Table[GeoDocument](tag, "gdocuments") {
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def externalWorkID = column[String]("ext_work_id", O.Nullable)
  
  def author = column[String]("author", O.Nullable)
  
  def title = column[String]("title")
  
  def date = column[Int]("date", O.Nullable)
  
  def dateComment = column[String]("date_comment", O.Nullable)
  
  def language = column[String]("language", O.Nullable)

  def description = column[String]("description", O.Nullable, O.DBType("text"))
  
  def source = column[String]("source", O.Nullable)
  
  def primaryTopicOf = column[String]("primary_topic_of", O.Nullable)
  
  def origin = column[String]("geo_origin", O.Nullable)
  
  def findspot = column[String]("geo_findspot", O.Nullable)
  
  def authorLocation = column[String]("geo_author_location", O.Nullable)
  
  def _collections = column[String]("collections", O.Nullable)

  def * = (id.?, externalWorkID.?, author.?, title, date.?, dateComment.?, language.?,
    description.?, source.?, primaryTopicOf.?, origin.?, findspot.?, authorLocation.?) <> (GeoDocument.tupled, GeoDocument.unapply)
    
}
    
object GeoDocuments {
  
  private[models] val query = TableQuery[GeoDocuments]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def insert(geoDocument: GeoDocument)(implicit s: Session) =
    query returning query.map(_.id) += geoDocument
    
  def update(geoDocument: GeoDocument)(implicit s: Session) =
    query.where(_.id === geoDocument.id).update(geoDocument)
      
  def listAll()(implicit s: Session): Seq[GeoDocument] = query.list
  
  def findById(id: Int)(implicit s: Session): Option[GeoDocument] =
    query.where(_.id === id).firstOption

  def findByIds(ids: Seq[Int])(implicit s: Session): Seq[GeoDocument] =
    query.where(_.id inSet ids).list
    
  def findByTitle(title: String)(implicit s: Session): Seq[GeoDocument] =
    query.where(_.title === title).list
    
  def findByIdsWithContent(ids: Seq[Int])(implicit s: Session): Seq[(GeoDocument, Seq[Int], Seq[Int])] = {
	val q = for {
      ((gdoc, text), image) <- query where (_.id inSet ids) leftJoin GeoDocumentTexts.query on (_.id === _.gdocId) leftJoin GeoDocumentImages.query on (_._1.id === _.gdocId)
    } yield (gdoc, text.id.?, image.id.?)

    q.list.groupBy(_._1).map { case (gdoc, allValues) => 
      (gdoc, 
       allValues.filter(_._2.isDefined).map(_._2.get),
       allValues.filter(_._3.isDefined).map(_._3.get)) } toSeq
  }
    
  def delete(id: Int)(implicit s: Session) =
    query.where(_.id === id).delete
    
  /** Helper method to find all IDs except those provided as argument.
    *
    * This method is used by the CollectionMembership class to determine
    * documents that are not assigned to a collection. To avoid confusion,
    * the method is not exposed outside of this package.  
    */
  private[models] def findAllExcept(ids: Seq[Int])(implicit s: Session): Seq[Int] =
    query.map(_.id).filter(id => !(id inSet ids)).list 
  
}
