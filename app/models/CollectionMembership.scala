package models

import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag
import models.stats.CompletionStats
import play.api.Logger

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
  
  /** Foreign key constraints **/
  def gdocFk = foreignKey("gdoc_fk", gdocId, TableQuery[GeoDocuments])(_.id)

}

object CollectionMemberships {
  
  private val query = TableQuery[CollectionMemberships]
  
  def create()(implicit s: Session) = query.ddl.create
  
  def insertAll(collectionMemberships: Seq[CollectionMembership])(implicit s: Session) = query.insertAll(collectionMemberships:_*)
  
  def listAll()(implicit s: Session): Seq[CollectionMembership] =
    query.list
  
  /** Get the collections the specified gdoc is part of **/
  def findForGeoDocument(gdocId: Int)(implicit s: Session): Seq[String] =
    query.where(_.gdocId === gdocId).map(_.collection).list
   
  /** Delete all collection memberships for a specific document **/
  def deleteForGeoDocument(gdocId: Int)(implicit s: Session) =
    query.where(_.gdocId === gdocId).delete
    
  /** Lists all collection names **/
  def listCollections()(implicit s: Session): Seq[String] = 
    query.map(_.collection).list.distinct

  /** Counts the number of documents in the specified collection **/
  def countGeoDocumentsInCollection(collection: String)(implicit s: Session): Int =
    query.where(_.collection.toLowerCase === collection.toLowerCase).list.size
    
  /** Returns the IDs of the documents in the specified collection **/
  def getGeoDocumentsInCollection(collection: String)(implicit s: Session): Seq[Int] =
    query.where(_.collection.toLowerCase === collection.toLowerCase).map(_.gdocId).list
     
  /** Helper method that returns IDs of all documents that are not assigned to any collection **/
  def getUnassignedGeoDocuments()(implicit s: Session): Seq[Int] = {
    val allGDocsInCollections = query.groupBy(_.gdocId).map(_._1).list.distinct
    GeoDocuments.query.map(_.id).filter(id => !(id inSet allGDocsInCollections)).list 
  }
  
  /** Helper method to get the completion stats for a specific collection */
  def getCompletionStatsForCollection(collection: String)(implicit s: Session): CompletionStats = {
    val q = query.where(_.collection.toLowerCase === collection.toLowerCase)
                 .map(_.gdocId)
                 .innerJoin(Annotations.query).on(_ === _.gdocId)
                 .groupBy(_._2.status)
                 .map(t => (t._1, t._2.length))
                 
    CompletionStats(q.list.toMap)
  } 
    
}