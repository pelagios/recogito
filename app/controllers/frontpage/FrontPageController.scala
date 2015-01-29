package controllers.frontpage

import controllers.common.auth.Secured
import models._
import models.stats.CompletionStats
import play.api.db.slick._
import play.api.mvc.Controller

/** Encapsulates the information shown in one row of the landing page's document index **/
case class DocumentIndexRow(doc: GeoDocument, stats: CompletionStats, firstSource: Option[String], firstText: Option[Int], firstImage: Option[Int], signOffRatio: Double)

object FrontPageController extends Controller with Secured {
  
  /** Helper function to collapse multiple language versions of the same document into one  **/
  private def foldLanguageVersions(docs: Seq[DocumentIndexRow]): Seq[(Option[String], Seq[DocumentIndexRow])] = {
    val groupedByExtID = docs.filter(_.doc.externalWorkID.isDefined).groupBy(_.doc.externalWorkID)
        
    // Creates a list of [Ext. Work ID -> Documents] mappings
    docs.foldLeft(Seq.empty[(Option[String], Seq[DocumentIndexRow])])((collapsedList, document) => {
      if (document.doc.externalWorkID.isEmpty) {
        // No external work ID means we don't group this doc with other docs
        collapsedList :+ (None, Seq(document))
      } else {
        val workIDsInList = collapsedList.filter(_._1.isDefined).map(_._1.get)
        if (!workIDsInList.contains(document.doc.externalWorkID.get))
          // This is a doc that needs grouping, and it's not in the list yet
          collapsedList :+ (document.doc.externalWorkID, groupedByExtID.get(document.doc.externalWorkID).get)
        else
          // This doc is already in the list
          collapsedList
      }
    })
  }
  
  def index(collection: Option[String]) = DBAction { implicit rs =>    
    if (collection.isEmpty) {
      // If no collection is selected, redirect to the first in the list
      val allCollections = CollectionMemberships.listCollections :+ "other"
      Redirect(controllers.frontpage.routes.FrontPageController.index(Some(allCollections.head.toLowerCase)))
    } else {
      // IDs of all documents NOT assigned to a collection
      val unassigned = CollectionMemberships.getUnassignedGeoDocuments
      
      // Documents for the selected collection, with content IDs
      val gdocsWithcontent: Seq[(GeoDocument, Seq[Int], Seq[Int])] = {
        if (collection.get.equalsIgnoreCase("other"))
          GeoDocuments.findByIdsWithContent(unassigned)
        else
          GeoDocuments.findByIdsWithContent(CollectionMemberships.getGeoDocumentsInCollection(collection.get)) 
        }
        .sortBy(t => (t._1.date, t._1.author, t._1.title))
        
      val ids = gdocsWithcontent.map(_._1.id.get)
      
      // Get stats for each document
      val stats: Map[Int, CompletionStats] = 
        Annotations.getCompletionStats(ids)
        
      val parts: Map[Int, Seq[GeoDocumentPart]] =
        GeoDocumentParts.findByIds(ids)

      val textSignOffs = SignOffs.countForGeoDocumentTexts(gdocsWithcontent.flatMap(_._2))
      val imageSignOffs = SignOffs.countForGeoDocumentImages(gdocsWithcontent.flatMap(_._3))

      // Merge docs, stats & first content IDs to form the 'index row'
      val indexRows = gdocsWithcontent.map { case (gdoc, texts, images) => {
        val firstSource =
          if (gdoc.source.isDefined)
            gdoc.source
          else
            parts.get(gdoc.id.get).flatMap(_.filter(_.source.isDefined).headOption).flatMap(_.source)
        
        val signedTexts = textSignOffs.filter { case (id, numberOfSignOffs) => texts.contains(id) }.size
        val signedImages = imageSignOffs.filter { case (id, numberOfSignOffs) => images.contains(id) }.size
        val signOffRatio =
          if ((texts.size  + images.size) == 0)
            1.0
          else
            (signedTexts + signedImages).toDouble / (texts.size +  images.size)
        
        DocumentIndexRow(
          gdoc, 
          stats.get(gdoc.id.get).getOrElse(CompletionStats.empty), 
          firstSource, 
          texts.headOption, 
          images.headOption,
          signOffRatio)
      }}
                  
      // The information required for the collection selection widget
      val docsPerCollection: Seq[(String, Int)] = 
        CollectionMemberships.listCollections.map(collection =>
          (collection, CollectionMemberships.countGeoDocumentsInCollection(collection))) ++
          { if (unassigned.size > 0) Seq(("Other", unassigned.size)) else Seq.empty[(String, Int)] }
      
      val groupedDocs = foldLanguageVersions(indexRows)
            
      // Populate the correct template, depending on login state
      if (currentUser.isDefined && isAuthorized)      
        Ok(views.html.index(groupedDocs, docsPerCollection, collection.get, EditHistory.listHighscores(5), currentUser.get))
      else 
        Ok(views.html.publicIndex(groupedDocs, docsPerCollection, EditHistory.listHighscores(5), collection.get))
    }
  }  
  
}
