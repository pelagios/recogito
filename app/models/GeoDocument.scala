package models

import models.stats.GeoDocumentStats
import play.api.Play.current
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag
import play.api.Logger

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
  
  implicit val stringSeqMapper = MappedColumnType.base[Seq[String],String](
    seq => seq.mkString(","),
    str => str.split(',').toList)
  
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
    
  def findByTitle(title: String)(implicit s: Session): Seq[GeoDocument] =
    query.where(_.title === title).list
    
  def findAll(ids: Seq[Int])(implicit s: Session): Seq[(GeoDocument, Option[Int], Option[Int])] = {
	val q = for {
      ((gdocId, firstText), firstImage) <- query leftJoin GeoDocumentTexts.query on (_.id === _.gdocId) leftJoin GeoDocumentImages.query on (_._1.id === _.gdocId)
    } yield (gdocId, firstText.id.?, firstImage.id.?)
    
    q.list
  }
    
  def findAllWithStats(ids: Seq[Int])(implicit s: Session): Seq[(GeoDocument, Int, Int, Int, Option[Int], Option[Int])] = {
    // Step 1 - get stats for all docs with annotation in a single query    
    // val unidentifiables = 
    //  Set(AnnotationStatus.AMBIGUOUS, AnnotationStatus.NO_SUITABLE_MATCH, AnnotationStatus.MULTIPLE, AnnotationStatus.NOT_IDENTIFYABLE)
      
    val q = for {
      (gdocId, status, numberOfAnnotations) <- Annotations.query.where(_.gdocId inSet ids).groupBy(t => (t.gdocId, t.status)).map(t => (t._1._1, t._1._2, t._2.length))
      // gdoc <- query.where(_.id === gdocId)
    } yield (gdocId, status, numberOfAnnotations)
    
    /*
    val subq = for {
      (((gdocId, status, numberOfAnnotations), firstText), firstImage) <- q leftJoin GeoDocumentTexts.query on (_._1 === _.gdocId) leftJoin GeoDocumentImages.query on (_._1._1 === _.gdocId)
    } yield (gdocId, status, numberOfAnnotations, firstText.id.?, firstImage.id.?)
    */
    
    /*
    val flattenedResult = subq.list.groupBy(t => (t._1, t._2, t._3)).map { case ((gdocId, status, numberOfAnnotations), allVals) => 
      (gdocId, status, numberOfAnnotations, allVals(0)._4, allVals(0)._5) } groupBy (_._1)
     */
     
    val stats = q.list.groupBy(_._1).map { case (gdocId, statusDistribution) => {
      val verified = statusDistribution.find(_._2 == AnnotationStatus.VERIFIED).map(_._3).getOrElse(0)
      val unidentifiable = statusDistribution.find(t => 
        Set(AnnotationStatus.AMBIGUOUS, 
            AnnotationStatus.NO_SUITABLE_MATCH, 
            AnnotationStatus.MULTIPLE,
            AnnotationStatus.NOT_IDENTIFYABLE).contains(t._2)).map(_._3).getOrElse(0)
      val total = statusDistribution.foldLeft(0)((count, tuple) => count + tuple._3)
      
      (gdocId, verified, unidentifiable, total)      
    }}
    
    /*
    val stats = flattenedResult.map { case (gdocId, joinedValues) => {
      val joinedValSeq = joinedValues.toSeq
      
      val verified = joinedValSeq.find(_._2 == AnnotationStatus.VERIFIED).map(_._3).getOrElse(0)
      val unidentifiable = joinedValSeq.find(t => 
        Set(AnnotationStatus.AMBIGUOUS, 
            AnnotationStatus.NO_SUITABLE_MATCH, 
            AnnotationStatus.MULTIPLE,
            AnnotationStatus.NOT_IDENTIFYABLE).contains(t._2)).map(_._3).getOrElse(0)
      val total = joinedValSeq.foldLeft(0)((count, tuple) => count + tuple._3)
      
      val firstTextId = joinedValSeq(0)._4
      val firstImageId = joinedValSeq(0)._5
      
      (gdocId, verified, unidentifiable, total, firstTextId, firstImageId)      
    }} toSeq
    */
    
    findAll(ids).map { case (doc, firstText, firstImage) => {
	  val s = stats.find(_._1 == doc.id.get)
	  if (s.isDefined)
	    (doc, s.get._2, s.get._3, s.get._4, firstText, firstImage)
	  else
	    (doc, 0, 0, 0, firstText, firstImage)
	}}
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
