package controllers.api

import controllers.api.writer.{ ImageAnnotationWriter, TextAnnotationWriter }
import controllers.common.auth.{ Secure, Secured }
import controllers.common.io.JSONSerializer
import java.sql.Timestamp
import java.util.{ Date, UUID }
import models._
import play.api.Logger
import play.api.db.slick._
import play.api.libs.json.{ Json, JsArray, JsObject }
import play.api.mvc.{ AnyContent, Controller, EssentialAction, RequestHeader, SimpleResult }
import scala.util.{ Try, Success, Failure }

object AnnotationAPIController extends Controller with ImageAnnotationWriter with TextAnnotationWriter with Secured {
  
  private def getParam(request: RequestHeader, name: String): Option[String] =
    request.queryString
      .filter(_._1.toLowerCase.equals(name.toLowerCase))
      .headOption.flatMap(_._2.headOption)
 
  def create = protectedDBAction(Secure.REJECT) { username => implicit requestWithSession =>
    val user = Users.findByUsername(username)    
    val body = requestWithSession.request.body.asJson    
    if (!body.isDefined) {    
      // No JSON body - bad request
      BadRequest(Json.parse("{ \"success\": false, \"message\": \"Missing JSON body\" }"))
      
    } else {
      if (body.get.isInstanceOf[JsArray]) {
        // Use recursion to insert until we get the first error message  
        def insertNext(toInsert: List[JsObject], username: String): Option[String] = {
          if (toInsert.size == 0) {
            None
          } else {
            createOne(toInsert.head, username) match {
              case Success(annotation) => insertNext(toInsert.tail, username)
              case Failure(exception) => Some(exception.getMessage) 
            }
          }
        }
 
        // Insert until error message
        val errorMsg = insertNext(body.get.as[List[JsObject]], username)
        if (errorMsg.isDefined)
          BadRequest(Json.parse(errorMsg.get))
        else
          Ok(Json.parse("{ \"success\": true }"))
      } else {
        val json = body.get.as[JsObject]
        try {
          createOne(json, user.get.username) match {
            case Success(annotation) => Ok(new JSONSerializer().toJson(annotation, false, false, true))
            case Failure(exception) => BadRequest(Json.parse("{ \"success\": false, \"message\": \"" + exception.getMessage + "\" }"))
          }
        } catch {
          case t: Throwable => {
            Logger.error("Error creating annotation: " + json)
            t.printStackTrace
            BadRequest("{ \"error\": \"" + t.getMessage + "\" }")
          }
        }
      }
    }  
  }
  
  private def createOne(json: JsObject, username: String)(implicit s: Session): Try[Annotation] = {
    // For the time being, we simply distinguish between text- & image-annotation based on the fact 
    // that the latter includes an 'shapes' property in the JSON
    val jsonShapes = (json \ "shapes").asOpt[JsArray]
    if (jsonShapes.isDefined)
      createOneImageAnnotation(json, username)
    else    
      createOneTextAnnotation(json, username)
  }
  
  def get(uuid: UUID) = DBAction { implicit session =>
    val annotation = Annotations.findByUUID(uuid)
    if (annotation.isDefined) {          
      Ok(new JSONSerializer().toJson(annotation.get, true, true, true))
    } else {
      NotFound
    }
  }
  
  def update(uuid: UUID): EssentialAction = protectedDBAction(Secure.REJECT) { username => implicit requestWithSession =>
    update(Some(uuid), username)
  }
  
  def updateAll() = protectedDBAction(Secure.REJECT) { username => implicit requestWithSession =>
    update(None, username)
  }
  
  private def update(uuid: Option[UUID], username: String)(implicit requestWithSession: DBSessionRequest[AnyContent]): SimpleResult = {
    val body = requestWithSession.request.body.asJson    
    if (!body.isDefined) {    
      // No JSON body - bad request
      BadRequest(Json.parse("{ \"success\": false, \"message\": \"Missing JSON body\" }"))
      
    } else {
      if (body.get.isInstanceOf[JsArray]) {
        // Use recursion to update until we get the first error message
        def updateNext(toUpdate: List[JsObject], username: String): Option[String] = {
          if (toUpdate.size == 0) {
            None
          } else {
            updateOne(toUpdate.head, None, username) match {
              case Success(annotation) => updateNext(toUpdate.tail, username)
              case Failure(exception) => Some(exception.getMessage)
            }
          }
        }
 
        // Insert until error message
        val errorMsg = updateNext(body.get.as[List[JsObject]], username)
        if (errorMsg.isDefined)
          BadRequest(Json.parse(errorMsg.get))
        else
            Ok(Json.parse("{ \"success\": true }"))
      } else {
        if (uuid.isEmpty) {
          // Single annotation in JSON body, but no UUID provided - bad request
          BadRequest(Json.parse("{ \"success\": false, \"message\": \"Missing JSON body\" }"))
        } else {
          updateOne(body.get.as[JsObject], uuid, username) match {
            case Success(annotation) => Ok(new JSONSerializer().toJson(annotation, false, false, true))
            case Failure(exception) => BadRequest(Json.parse("{ \"success\": false, \"message\": \"" + exception.getMessage + "\" }"))
          }
        }
      }
    } 
  }
  
  private def updateOne(json: JsObject, uuid: Option[UUID], username: String)(implicit s: Session): Try[Annotation] = {
    // For the time being, we simply distinguish between text- & image-annotation based on the fact 
    // that the latter includes a 'shapes' property in the JSON
    val jsonShapes = (json \ "shapes").asOpt[JsArray]
    if (jsonShapes.isDefined)
      updateOneImageAnnotation(json, uuid, username)
    else
      updateOneTextAnnotation(json, uuid, username)
  }
  
  def delete(uuid: UUID) = protectedDBAction(Secure.REJECT) { username => implicit requestWithSession =>
    val annotation = Annotations.findByUUID(uuid)
    if (!annotation.isDefined) {
      // Someone tries to delete an annotation that's not in the DB
      NotFound(Json.parse("{ \"success\": false, \"message\": \"Annotation not found\" }"))
      
    } else {
      val user = Users.findByUsername(username) // The user is logged in, so we can assume the Option is defined
      val updated = deleteAnnotation(annotation.get)
        
      // Record edit event
      if (updated.isDefined)
        EditHistory.insert(createDiffEvent(annotation.get, updated.get, user.get.username))
        
      Ok(Json.parse("{ \"success\": true }"))
    } 
  }
  
  def listAnnotations() = DBAction { implicit session =>
    val gdocPartId = getParam(session.request, "gdocPart")
    val gdocId = getParam(session.request, "gdoc")
    val ctsURI = getParam(session.request, "ctsURI")
    
    if (ctsURI.isDefined) {
      // Bit of a hack - we only support this for text annotations
      Ok(forCtsURI(ctsURI.get)).withHeaders(CONTENT_TYPE -> "application/rdf+xml", CONTENT_DISPOSITION -> ("attachment; filename=pelagios-egd.rdf"))
    } else if (gdocPartId.isDefined) {
      val annotations = Annotations.findByGeoDocumentPart(gdocPartId.get.toInt)
      Ok(Json.toJson(new JSONSerializer().toJson(annotations)))
    } else if (gdocId.isDefined) {
      val annotations = Annotations.findByGeoDocument(gdocId.get.toInt)
      Ok(Json.toJson(new JSONSerializer().toJson(annotations)))
    } else {
      BadRequest
    }
  }
  
}
