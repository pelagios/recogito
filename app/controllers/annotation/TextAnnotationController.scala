package controllers.annotation

import global.Global
import java.io.ByteArrayOutputStream
import java.sql.Timestamp
import java.util.{ Date, UUID }
import models._
import org.pelagios.gazetteer.Network
import play.api.Logger
import play.api.db.slick._
import play.api.Play.current
import play.api.libs.json.JsObject
import org.pelagios.Scalagios
import org.pelagios.api.Agent
import org.pelagios.api.annotation.{ Annotation => OAnnotation, AnnotatedThing, Transcription, TranscriptionType, SpecificResource }
import org.pelagios.api.annotation.selector.TextOffsetSelector
import scala.util.{ Try, Success, Failure }

trait TextAnnotationController extends AbstractAnnotationController {
  
  private val DARE_PREFIX = "http://www.imperium.ahlfeldt.se/"
    
  private val PLEIADES_PREFIX = "http://pleiades.stoa.org"
  
  private val UTF8 = "UTF-8"
    
  protected def createOneTextAnnotation(json: JsObject, username: String)(implicit s: Session): Try[Annotation] = {
    val jsonGdocId = (json\ "gdocId").asOpt[Int] 
    val jsonGdocPartId = (json \ "gdocPartId").asOpt[Int]  
    val jsonSource = (json \ "source").asOpt[String]
    
    if (jsonSource.isDefined) {
      createOneCTS(json, username)
    } else {
      val gdocPart = jsonGdocPartId.map(id => GeoDocumentParts.findById(id)).flatten
      val gdocId_verified = if (gdocPart.isDefined) Some(gdocPart.get.gdocId) else jsonGdocId.map(id => GeoDocuments.findById(id)).flatten.map(_.id).flatten
        
      if (!gdocPart.isDefined && !(jsonGdocId.isDefined && gdocId_verified.isDefined)) {
        // Annotation specifies neither valid GDocPart nor valid GDoc - invalid annotation
        Failure(new RuntimeException("Invalid GDoc or GDocPart ID"))
        
      } else {
        // Create new annotation
        val correctedToponym = (json \ "corrected_toponym").as[String]
        val correctedOffset = (json \ "corrected_offset").as[Int]   
        
        val automatch = { 
          val networks = Global.index.query(correctedToponym, true).map(Global.index.getNetwork(_))
          val matches = Network.conflateNetworks(networks.toSeq, 
            Some(PLEIADES_PREFIX), // prefer Pleiades URIs
            Some(DARE_PREFIX),     // prefer DARE for coordinates
            Some(PLEIADES_PREFIX)) // prefer Pleiades for descriptions
            
          if (matches.size > 0)
            Some(matches.head)
          else
            None
        }
        
        val annotation = 
          Annotation(Annotations.newUUID, gdocId_verified, gdocPart.map(_.id).flatten, 
                     AnnotationStatus.NOT_VERIFIED, None, None, None, automatch.map(_.uri), 
                     Some(correctedToponym), Some(correctedOffset))
          
        if (!isValid(annotation)) {
          // Annotation is mis-aligned with source text or has zero toponym length - something is wrong
          Logger.info("Invalid annotation error: " + correctedToponym + " - " + correctedOffset + " GDoc Part: " + gdocPart.map(_.id))
          Failure(new RuntimeException("Invalid annotation error (invalid offset or toponym)."))
          
        } else if (Annotations.getOverlappingAnnotations(annotation).size > 0) {
          // Annotation overlaps with existing ones - something is wrong
          Logger.info("Overlap error: " + correctedToponym + " - " + correctedOffset + " GDoc Part: " + gdocPart.get.id)
          Annotations.getOverlappingAnnotations(annotation).foreach(a => Logger.warn("Overlaps with " + a.uuid))
          Failure(new RuntimeException("Annotation overlaps with an existing one (details were logged)."))
          
        } else {
          Annotations.insert(annotation)
    
          // Record edit event
          EditHistory.insert(EditEvent(None, annotation.uuid, username, new Timestamp(new Date().getTime),
            None, Some(correctedToponym), None, None, None, None))
                                                      
          Success(annotation)
        }
      }
    }   
  }
  
  private def createOneCTS(json: JsObject, username: String)(implicit s: Session): Try[Annotation] = {
    val source = (json \ "source").as[String]
    val correctedToponym = (json \ "corrected_toponym").as[String]
    val correctedOffset = (json \ "corrected_offset").as[Int]        

    val annotation = 
      Annotation(Annotations.newUUID, None, None, 
                 AnnotationStatus.NOT_VERIFIED, None, None, None, None, 
                 Some(correctedToponym), Some(correctedOffset), None, source = Some(source))

    Annotations.insert(annotation)
    
    // Record edit event
    EditHistory.insert(EditEvent(None, annotation.uuid, username, new Timestamp(new Date().getTime),
      None, Some(correctedToponym), None, None, None, None))
                                                      
    Success(annotation)
  }
  
  /** Checks whether the annotation offset is properly aligned with the source text.
    * @param a the annotation 
    */
  private def isValid(a: Annotation)(implicit s: Session): Boolean = {
    val offset = if (a.correctedOffset.isDefined) a.correctedOffset else a.offset
    val toponym = if (a.correctedToponym.isDefined) a.correctedToponym else a.toponym

    if (offset.isDefined && toponym.isDefined) {
      if (toponym.get.trim.size == 0) {
        // If the toponym is a string with size 0 we'll discard immediately
        false
        
      } else {
        // Cross check against the source text, if available
        val text = GeoDocumentTexts.getTextForAnnotation(a).map(gdt => new String(gdt.text, UTF8))
        if (text.isDefined) {
          // Compare with the source text
          val referenceToponym = text.get.substring(offset.get, offset.get + toponym.get.size)
          referenceToponym.equals(toponym.get)
        } else {
          // We don't have a text for the annotation - so we'll just have to accept the offset
          true
        }
      }
    } else {
      // Annotation has no offset and/or toponym - so isn't tied to a text, and we're cool
      true
    }
  }
  
  protected def forCtsURI(source: String)(implicit s: DBSessionRequest[_]): String = { 
    // Convert Recogito annotations to OA
    val basePath = controllers.routes.ApplicationController.index(None).absoluteURL(false)
    val thing = AnnotatedThing(basePath + "egd", source)
    
    Annotations.findBySource(source).foreach(a => {
      val place =  { if (a.correctedGazetteerURI.isDefined) a.correctedGazetteerURI else a.gazetteerURI }
        .map(Seq(_)).getOrElse(Seq.empty[String])
              
      val serializedBy = Agent("http://pelagios.org/recogito#version1.0", Some("Recogito Annotation Tool"))
      val offset = if (a.correctedOffset.isDefined) a.correctedOffset else a.offset
      val toponym = if (a.correctedToponym.isDefined) a.correctedToponym else a.toponym
      val transcription = toponym.map(t => Transcription(t, TranscriptionType.Toponym))
      val selector = offset.map(offset => TextOffsetSelector(offset, toponym.get.size))
      val target = if (selector.isDefined) SpecificResource(thing, selector.get) else thing
      val uri = basePath + "api/annotations/" + a.uuid          
      
      val oa = OAnnotation(uri, target, place = place, transcription = transcription, serializedBy = serializedBy)
    })

    val out = new ByteArrayOutputStream()
    Scalagios.writeAnnotations(Seq(thing), out, Scalagios.RDFXML)
    new String(out.toString(UTF8))   
  }
    
  protected def updateOneTextAnnotation(json: JsObject, uuid: Option[UUID], username: String)(implicit s: Session): Try[Annotation] = {    
    val annotation = if (uuid.isDefined) {
        Annotations.findByUUID(uuid.get)        
      } else {
        (json \ "id").as[Option[String]].flatMap(uuid => Annotations.findByUUID(UUID.fromString(uuid)))
      }
      
    if (!annotation.isDefined) {
      // Someone tries to update an annotation that's not in the DB
      Failure(new RuntimeException("Annotation not found"))
      
    } else { 
      val correctedStatus = (json \ "status").asOpt[String].map(AnnotationStatus.withName(_))
      val correctedToponym = (json \ "corrected_toponym").asOpt[String]
      val correctedOffset = (json \ "corrected_offset").asOpt[Int]
      val correctedURI = (json \ "corrected_uri").asOpt[String]
      val correctedTags = (json \ "tags").asOpt[String].map(_.toLowerCase)
      val correctedComment = (json \ "comment").asOpt[String]
        
      val updatedStatus = correctedStatus.getOrElse(annotation.get.status)
      val updatedToponym = if (correctedToponym.isDefined) correctedToponym else annotation.get.correctedToponym
      val updatedOffset = if (correctedOffset.isDefined) correctedOffset else annotation.get.correctedOffset
      val updatedURI = if (correctedURI.isDefined) correctedURI else annotation.get.correctedGazetteerURI
      val updatedTags = if (correctedTags.isDefined) correctedTags else annotation.get.tags
      val updatedComment = if (correctedComment.isDefined) correctedComment else annotation.get.comment
   
      val toponym = if (updatedToponym.isDefined) updatedToponym else annotation.get.toponym
      val offset = if (updatedOffset.isDefined) updatedOffset else annotation.get.offset
                     
      val updated = 
        Annotation(annotation.get.uuid,
                   annotation.get.gdocId,
                   annotation.get.gdocPartId, 
                   updatedStatus,
                   annotation.get.toponym,
                   annotation.get.offset, 
                   annotation.get.anchor, 
                   annotation.get.gazetteerURI, 
                   updatedToponym,
                   updatedOffset,
                   annotation.get.correctedAnchor, 
                   updatedURI,
                   updatedTags,
                   updatedComment,
                   annotation.get.source,
                   { if (annotation.get.seeAlso.size > 0) Some(annotation.get.seeAlso.mkString(",")) else None })
                   
      // Important: if an annotation was created manually, and someone marks it as 'false detection',
      // We delete it instead!
      if (updated.status == AnnotationStatus.FALSE_DETECTION && !updated.toponym.isDefined)
        deleteAnnotation(updated)
      else
        Annotations.update(updated)
          
      // Remove all overlapping annotations
      Annotations.getOverlappingAnnotations(updated).foreach(deleteAnnotation(_))
        
      // Record edit event
      val user = Users.findByUsername(username) // The user is logged in, so we can assume the Option is defined
      EditHistory.insert(createDiffEvent(annotation.get, updated, user.get.username))
      
      Success(updated)
    }
  }
  
}