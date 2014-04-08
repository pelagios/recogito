package models

import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

/** Associates a GeoDocument with a collection.
  *
  * @author Rainer Simon <rainer.simon@ait.ac.at>
  */
case class CollectionMembership(id: Option[Int], gdocId: Int, collection: String)

/** Collections database table **/
object Collections extends Table[CollectionMembership]("collection_memberships") {
  
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  
  def gdocId = column[Int]("gdoc", O.NotNull)
  
  def collection = column[String]("collection", O.NotNull)
  
  def * = id.? ~ gdocId ~ collection <> (CollectionMembership.apply _, CollectionMembership.unapply _)

  def listAll()(implicit s: Session): Set[String] = 
    Query(Collections).map(_.collection).list.toSet
  
  def countDocumentsInCollection(collection: String)(implicit s: Session): Int =
    Query(Collections).where(_.collection === collection).list.size
    
  def getCollectionMemberships(gdocs: Seq[GeoDocument])(implicit s: Session): Seq[(GeoDocument, List[String])] =
    gdocs.map(doc => (doc, Query(Collections).where(_.gdocId === doc.id.get).map(_.collection).list))
    
}