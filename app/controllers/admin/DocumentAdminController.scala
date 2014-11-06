package controllers.admin

import controllers.{ Secure, Secured }
import controllers.common.io.{ CSVParser, ZipExporter, ZipImporter }
import java.util.zip.ZipFile
import models._
import models.content._
import play.api.mvc.Controller
import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._
import play.api.Play.current
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import scala.io.Source

/** Controller for the 'Documents' section of the admin area.
  * 
  * @author Rainer Simon <rainer.simon@ait.ac.at> 
  */
object DocumentAdminController extends Controller with Secured {
   
  implicit def join(seq: Seq[String]): Option[String] = 
    if (seq.size > 0)
      None
    else
      Some(seq.mkString(", "))  
  
  /** The document form (used in both create and edit documents) **/
  private val documentForm = Form(
	mapping(
		"id" -> optional(number),
		"extWorkID" -> optional(text),
		"author" -> optional(text), 
		"title" -> text, 
		"date" -> optional(number), 
		"dateComment" -> optional(text),
		"openLicense" -> boolean,
		"language" -> optional(text),
		"description" -> optional(text),
		"source" -> optional(text),
		"primaryTopicOf" -> optional(text),
		"origin" -> optional(text),
		"findspot" -> optional(text),
		"authorLocation" -> optional(text),
		"comment" -> optional(text)
	)(GeoDocument.apply)(GeoDocument.unapply)
  )
  
  /** Index page listing all documents in the DB **/
  def listAll = adminAction { username => implicit session =>
    Ok(views.html.admin.documents(GeoDocuments.listAll()))
  }
  
  /** Download one specific document (with text and annotations) as a ZIP file **/
  def downloadDocument(gdocId: Int) = adminAction { username => implicit session =>
    val gdoc = GeoDocuments.findById(gdocId)
    if (gdoc.isDefined) {
      val exporter = new ZipExporter()
      val file = exporter.exportGDoc(gdoc.get)
      val bytes = Source.fromFile(file.file)(scala.io.Codec.ISO8859).map(_.toByte).toArray
      file.finalize()
      Ok(bytes).withHeaders(CONTENT_TYPE -> "application/zip", CONTENT_DISPOSITION -> ({ "attachment; filename=" + exporter.escapeTitle(gdoc.get.title) }))
    } else {
      NotFound
    }
  }
    
  /** Delete a document (and associated data) from the database **/
  def deleteDocument(id: Int) = adminAction { username => implicit session =>
    // Annotations
    Annotations.deleteForGeoDocument(id)
    
    // Collection memeberships
    CollectionMemberships.deleteForGeoDocument(id)
    
    // Content and signoffs
    GeoDocumentContent.findByGeoDocument(id).foreach { case (content, partName) => {
      content match {
        case text: GeoDocumentText => SignOffs.deleteForGeoDocumentText(text.id.get)
        case image: GeoDocumentImage => SignOffs.deleteForGeoDocumentText(image.id.get)
      }
    }}
    GeoDocumentTexts.deleteForGeoDocument(id)
    GeoDocumentImages.deleteForGeoDocument(id)
    
    // Document & parts
    GeoDocumentParts.deleteForGeoDocument(id)
    GeoDocuments.delete(id)
    
    Status(200)
  }
  
  /** Upload documents (with text and annotations) to the database, from a ZIP file **/
  def uploadDocuments = adminAction { username => implicit session =>
    val formData = session.request.body.asMultipartFormData
    if (formData.isDefined) {
      try {
        val f = formData.get.file("zip")
        if (f.isDefined) {
          val zipFile = f.get.ref.file
          val errors = ZipImporter.validateZip(zipFile)
          Logger.info(errors.toString)
          if (errors.size == 0) {
            val imported = ZipImporter.importZip(zipFile)
            Redirect(routes.DocumentAdminController.listAll).flashing("success" -> { "Uploaded " + imported + " document(s)." })
          } else {
            Redirect(routes.DocumentAdminController.listAll).flashing("error" -> { "<strong>Your file had " + errors.size + " errors:</strong>" + errors.map("<p>" + _ + "</p>").mkString("\n") })
          }
        } else {
          BadRequest
        }
      } catch {
        case t: Throwable => {
          t.printStackTrace()
          Redirect(routes.DocumentAdminController.listAll).flashing("error" -> { "Something went wrong with your import: " + t.getClass.getCanonicalName() + ": " + t.getMessage }) 
        }
      }
    } else {
      BadRequest
    }
  }

  /** Edit an existing document in the UI form **/
  def editDocument(id: Int) = adminAction { username => implicit session =>
    GeoDocuments.findById(id).map { doc =>
      val parts = GeoDocumentParts.findByGeoDocument(id)
      val collections = CollectionMemberships.findForGeoDocument(doc.id.get)
      Ok(views.html.admin.documentDetails(id, parts, collections, documentForm.fill(doc)))
    }.getOrElse(NotFound)
  }
  
  /** Save a document from the UI form **/
  def updateDocument(id: Int) = adminAction { username => implicit session =>
    documentForm.bindFromRequest.fold(
      formWithErrors => {
        val parts = GeoDocumentParts.findByGeoDocument(id)
        val collections = CollectionMemberships.findForGeoDocument(id)
	    BadRequest(views.html.admin.documentDetails(id, parts, collections, formWithErrors))
	  },
	  
      document => {
        // Update the document
        GeoDocuments.update(document)
        
        // Get contents of unmapped collections field (conveniently converted to Option[Seq[String]])
        val collections = (session.request.body.asFormUrlEncoded.get.get("collections").get match {
          case c if (c.size == 0) => None
          case c if (c.head.isEmpty) => None
          case c => Some(c.head)
        }).map(_.split(",").toSeq).map(_.toSet).getOrElse(Set.empty[String])

        val currentMemberships = CollectionMemberships.findForGeoDocument(document.id.get).toSet
        if (currentMemberships != collections) {
          // Change detected! Update collection memberships
          CollectionMemberships.deleteForGeoDocument(document.id.get)
          CollectionMemberships.insertAll(collections.toSeq.map(CollectionMembership(None, document.id.get, _)))
        }
        
        Redirect(routes.DocumentAdminController.listAll())
      }
    )
  }  
  
  /** Import annotations from a CSV file into the document with the specified ID **/
  def uploadAnnotations(doc: Int) = adminAction { username => implicit session =>
    val gdoc = GeoDocuments.findById(doc)
    if (gdoc.isDefined) {
      val formData = session.request.body.asMultipartFormData
      if (formData.isDefined) {
          formData.get.file("csv").map(filePart => {
          val parser = new CSVParser()
          val annotations = parser.parseAnnotations(filePart.ref.file.getAbsolutePath, gdoc.get.id.get)
          Logger.info("Importing " + annotations.size + " annotations to " + gdoc.get.title)
          Annotations.insertAll(annotations)
        })
      }
    }
    Redirect(routes.DocumentAdminController.listAll)
  }
  
  /** Drop all annotations from the document with the specified ID **/
  def deleteAnnotations(doc: Int) = adminAction { username => implicit session =>
    Annotations.deleteForGeoDocument(doc)
    Status(200)
  }
  
}
