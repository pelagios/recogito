package models

import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

/** Associates a GeoDocument with a collection.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
case class CollectionMembership(id: Option[Int], gdocId: Int, collection: String)

/** Collections database table **/
object CollectionMemberships extends Table[CollectionMembership]("collection_memberships") {
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def gdocId = column[Int]("gdoc", O.NotNull)
  
  def collection = column[String]("collection", O.NotNull)
  
  def collection_lowercase = column[String]("collection_lowercase", O.NotNull)
  
  def * = id.? ~ gdocId ~ collection <> (CollectionMembership.apply _, CollectionMembership.unapply _)

  /** Lists all collection names **/
  def listCollections()(implicit s: Session): Seq[String] = 
    (Query(CollectionMemberships).map(_.collection)).list.distinct

  /** Counts the number of documents in the specified collection **/
  def countDocumentsInCollection(collection: String)(implicit s: Session): Int =
    (Query(CollectionMemberships).where(_.collection.toLowerCase === collection.toLowerCase)).list.size
    
  /** Returns the IDs of the documents in the specified collection **/
  def getDocumentsInCollection(collection: String)(implicit s: Session): Seq[Int] =
    (Query(CollectionMemberships).where(_.collection.toLowerCase === collection.toLowerCase).map(_.gdocId)).list
        
  def getUnassignedGeoDocuments()(implicit s: Session): Seq[Int] = {
    val docsInCollections = Query(CollectionMemberships).map(_.id).list.distinct
    GeoDocuments.findAllExcept(docsInCollections)
  }
    
}