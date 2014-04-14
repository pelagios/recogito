package models

import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag

/** Associates a GeoDocument with a collection.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
case class CollectionMembership(id: Option[Int], gdocId: Int, collection: String)

/** Collections database table **/
class CollectionMemberships(tag: Tag) extends Table[CollectionMembership](tag, "collection_memberships") {
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def gdocId = column[Int]("gdoc", O.NotNull)
  
  def collection = column[String]("collection", O.NotNull)
  
  def * = (id.?, gdocId, collection) <> (CollectionMembership.tupled, CollectionMembership.unapply)

}

object CollectionMemberships {
  
  private val query = TableQuery[CollectionMemberships]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def insertAll(collectionMemberships: Seq[CollectionMembership])(implicit s: Session) = query.insertAll(collectionMemberships:_*)
  
  def listAll()(implicit s: Session): Seq[CollectionMembership] =
    query.list
  
  /** Get the collections the specified gdoc is part of **/
  def findForDocument(gdocId: Int)(implicit s: Session): Seq[String] =
    query.where(_.gdocId === gdocId).map(_.collection).list
    
  /** Lists all collection names **/
  def listCollections()(implicit s: Session): Seq[String] = 
    query.map(_.collection).list.distinct

  /** Counts the number of documents in the specified collection **/
  def countDocumentsInCollection(collection: String)(implicit s: Session): Int =
    query.where(_.collection.toLowerCase === collection.toLowerCase).list.size
    
  /** Returns the IDs of the documents in the specified collection **/
  def getDocumentsInCollection(collection: String)(implicit s: Session): Seq[Int] =
    query.where(_.collection.toLowerCase === collection.toLowerCase).map(_.gdocId).list
        
  def getUnassignedGeoDocuments()(implicit s: Session): Seq[Int] = {
    val docsInCollections = query.map(_.gdocId).list.distinct
    GeoDocuments.findAllExcept(docsInCollections)
  }
    
}