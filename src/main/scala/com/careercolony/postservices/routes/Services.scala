package com.careercolony.postservices.routes

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Directive1, Route}

import akka.stream.ActorMaterializer
import com.careercolony.postservices.factories.{DatabaseAccess, Post, GetPost, Comment, GetComment, PostLike, GetPostLike, User, User2, User3, Address, ResponseStatus, BioData, GetJobTitle, Experience,
Feeds, GetFeeds, Article, GetArticle, ArticleComment, GetArticleComment, ArticleLike}
import spray.json.DefaultJsonProtocol

import ch.megard.akka.http.cors.CorsDirectives._
import ch.megard.akka.http.cors.CorsSettings

import akka.http.scaladsl.model.HttpMethods._
import scala.collection.immutable

import scala.collection.mutable.MutableList;
import spray.json._;

import java.util.concurrent.TimeUnit





object PostJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  
  implicit val AddressFormats = jsonFormat2(Address)
  implicit val UserFormats = jsonFormat5(User)
  implicit val User2Formats = jsonFormat4(User2)
  implicit val User3Formats = jsonFormat7(User3)
  implicit val BiodataFormats = jsonFormat11(BioData)
  implicit val JobtitleFormats = jsonFormat1(GetJobTitle)
  implicit val ExpFormats = jsonFormat6(Experience)
  implicit val ResponseFormat = jsonFormat3(ResponseStatus.apply)

  implicit val PostFormats = jsonFormat10(Post)
  implicit val GetPostFormats = jsonFormat11(GetPost)
  implicit val CommentFormats = jsonFormat7(Comment)
  implicit val GetCommentFormats = jsonFormat8(GetComment)
  implicit val PostLikeFormats = jsonFormat2(PostLike)
  implicit val GetPostLikeFormats = jsonFormat3(GetPostLike)
  implicit val ArticleFormats = jsonFormat7(Article)
  implicit val GetArticleFormats = jsonFormat10(GetArticle)
  implicit val ArticleCommentFormats = jsonFormat7(ArticleComment)
  implicit val ArticleLikeFormats = jsonFormat2(ArticleLike)
  implicit val GetArticleCommentFormats = jsonFormat8(GetArticleComment)
  implicit val FeedsFormats = jsonFormat10(Feeds)
  implicit val GetFeedsFormats = jsonFormat11(GetFeeds)
  //implicit val ResponseFormat = jsonFormat3(ResponseStatus.apply)
}

trait Service extends DatabaseAccess {

  import PostJsonSupport._

  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  val logger = Logging(system, getClass)

  implicit def myExceptionHandler = {
    ExceptionHandler {
      case e: ArithmeticException =>
        extractUri { uri =>
          complete(HttpResponse(StatusCodes.InternalServerError,
            entity = s"Data is not persisted and something went wrong"))
        }
    }
  }


  val settings = CorsSettings.defaultSettings.copy(allowedMethods = immutable.Seq(GET, PUT, POST, HEAD, OPTIONS))
  val userRoutes: Route = cors(settings){
    post {
      path("new-post") {
        entity(as[Post]) { entity =>
          complete {
            try {
              val isPersisted: MutableList[GetPost] = newPost(entity)
              isPersisted match {
                case _: MutableList[_] =>
                  
                  var response: StringBuilder = new StringBuilder("[")
                  isPersisted.foreach(
                      x => response.append(x.toJson).append(",")
                    )
                  response.deleteCharAt(response.length - 1)
                  response.append("]"); 
                  
                  HttpResponse(StatusCodes.OK, entity = response.toString()) //data.toString())
                  case _ => HttpResponse(StatusCodes.BadRequest,
                   entity = s"User already exist")
              }
            } catch {
              case ex: Throwable =>
                logger.error(ex, ex.getMessage)
                HttpResponse(StatusCodes.InternalServerError,
                  entity = "Error while persisting data, please try again")
            }
          }
        }
      }
    } ~ path("get-post" / "memberID" / Segment) { (memberID: String) =>
      get {
        complete {
          try {
                val idAsRDD: MutableList[GetPost] = retrievePost(memberID.toInt)
                idAsRDD match {
                  case _: MutableList[_] =>
                    var response: StringBuilder = new StringBuilder("[")
                  idAsRDD.foreach(
                      x => response.append(x.toJson).append(",")
                    )

                    // If records exist
                    if(response.length > 1) response.deleteCharAt(response.length - 1);
                    response.append("]");  
                    HttpResponse(StatusCodes.OK, entity = response.toString()) 

                    case _ => HttpResponse(StatusCodes.InternalServerError,
                    entity = s"Error found for user")           
                }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for post")
          }
        }
      }
    } ~ path("get-one-post" / "memberID" / Segment / "postID" / Segment) { (memberID: String, postID: String) =>
      get {
        complete {
          try {
                val idAsRDD: MutableList[GetPost] = retrieveOnePost(memberID.toInt, postID.toInt)
                idAsRDD match {
                  case _: MutableList[_] =>
                    var response: StringBuilder = new StringBuilder("[")
                  idAsRDD.foreach(
                      x => response.append(x.toJson).append(",")
                    )

                    // If records exist
                    if(response.length > 1) response.deleteCharAt(response.length - 1);
                    response.append("]");  
                    HttpResponse(StatusCodes.OK, entity = response.toString()) 

                    case _ => HttpResponse(StatusCodes.InternalServerError,
                    entity = s"Error found for user")           
                }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for post")
          }
        }
      }
    } ~ path("all-posts") {
      get {
        complete {
          try {
            val idAsRDD: MutableList[GetPost] = retrieveallPosts()
              //val isPersisted: MutableList[User2] = login(entity)
              //val claims = setClaims("flavoursoft@yahoo.com", tokenExpiryPeriodInDays)
              idAsRDD match {
                case _: MutableList[_] =>
                  var response: StringBuilder = new StringBuilder("[")
                  idAsRDD.foreach(
                      x => response.append(x.toJson).append(",")
                    )
                  
                  // If records exist
                  if(response.length > 1) response.deleteCharAt(response.length - 1);
                  response.append("]"); 

                  HttpResponse(StatusCodes.OK, entity = response.toString()) //data.toString())
                  case _ => HttpResponse(StatusCodes.BadRequest,
                   entity = s"User does not exist")
              }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError,
                entity = s"Data is not fetched and something went wrong")
          }
        }
      }
    } ~ path("update-post") {
      put {
         entity(as[GetPost]) { entity =>
          complete {
            try {
              val isPersisted = updatePost(entity)
              isPersisted match {
                case true => HttpResponse(StatusCodes.Created,
                  entity = s"Data is successfully persisted")
                case false => HttpResponse(StatusCodes.InternalServerError,
                  entity = s"Error found for email ")
              }
            } catch {
              case ex: Throwable =>
                logger.error(ex, ex.getMessage)
                HttpResponse(StatusCodes.InternalServerError, entity = s"Error found for email ")
            }
          }
        }
      }
    } ~ path("delete" / Segment / Segment ) { (field: String, value:String) =>
      get {
        complete {
          try {
            val idAsRDD = deletePost(field, value)
            idAsRDD match {
              case 1 => HttpResponse(StatusCodes.OK, entity = "Data is successfully deleted")
              case 0 => HttpResponse(StatusCodes.InternalServerError,
                entity = s"Data is not deleted and something went wrong")
            }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for $field : $value")
          }
        }
      }
    } ~ path("new-comment") {
      post {
        entity(as[Comment]) { entity =>
          complete {
            try {
              val isPersisted: MutableList[GetComment] = newComment(entity)
              isPersisted match {
                case _: MutableList[_] =>
                  
                  var response: StringBuilder = new StringBuilder("[")
                  isPersisted.foreach(
                      x => response.append(x.toJson).append(",")
                    )
                  response.deleteCharAt(response.length - 1)
                  response.append("]"); 
                  
                  HttpResponse(StatusCodes.OK, entity = response.toString()) //data.toString())
                  case _ => HttpResponse(StatusCodes.BadRequest,
                   entity = s"Comment already exist")
              }
            } catch {
              case ex: Throwable =>
                logger.error(ex, ex.getMessage)
                HttpResponse(StatusCodes.InternalServerError,
                  entity = "Error while persisting data, please try again")
            }
          }
        }
      }
    } ~ path("get-comments" / Segment / Segment ) { (ID: String, idValue: String) =>
      get {
        complete {
          try {
                val idAsRDD: MutableList[GetComment] = retrieveComments(ID, idValue.toInt)
                idAsRDD match {
                  case _: MutableList[_] =>
                    var response: StringBuilder = new StringBuilder("[")
                  idAsRDD.foreach(
                      x => response.append(x.toJson).append(",")
                    )

                    // If records exist
                    if(response.length > 1) response.deleteCharAt(response.length - 1);
                    response.append("]");  
                    HttpResponse(StatusCodes.OK, entity = response.toString()) 

                    case _ => HttpResponse(StatusCodes.InternalServerError,
                    entity = s"Error found for user")           
                }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for post")
          }
        }
      }
    } ~ path("get-post-comments" / Segment / Segment ) { (ID: String, idValue: String) =>
      get {
        complete {
          try {
                val idAsRDD: MutableList[GetComment] = retrievePostComments(ID, idValue.toInt)
                idAsRDD match {
                  case _: MutableList[_] =>
                    var response: StringBuilder = new StringBuilder("[")
                  idAsRDD.foreach(
                      x => response.append(x.toJson).append(",")
                    )

                    // If records exist
                    if(response.length > 1) response.deleteCharAt(response.length - 1);
                    response.append("]");  
                    HttpResponse(StatusCodes.OK, entity = response.toString()) 

                    case _ => HttpResponse(StatusCodes.InternalServerError,
                    entity = s"Error found for user")           
                }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for post")
          }
        }
      }
    } ~ path("update-comment") {
      post {
         entity(as[GetComment]) { entity =>
          complete {
            try {
              val isPersisted = updateComment(entity)
              isPersisted match {
                case true => HttpResponse(StatusCodes.Created,
                  entity = s"Data is successfully persisted")
                case false => HttpResponse(StatusCodes.InternalServerError,
                  entity = s"Error found for email ")
              }
            } catch {
              case ex: Throwable =>
                logger.error(ex, ex.getMessage)
                HttpResponse(StatusCodes.InternalServerError, entity = s"Error found for email ")
            }
          }
        }
      }
    } ~ path("deletecmt" / Segment / Segment ) { (field: String, value:String) =>
      get {
        complete {
          try {
            val idAsRDD = deleteComment(field, value)
            idAsRDD match {
              case 1 => HttpResponse(StatusCodes.OK, entity = "Data is successfully deleted")
              case 0 => HttpResponse(StatusCodes.InternalServerError,
                entity = s"Data is not deleted and something went wrong")
            }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for $field : $value")
          }
        }
      }
    } ~ path("post-like") {
      put {
        entity(as[PostLike]) { entity =>
          complete {
            try {
              val isPersisted = likePost(entity)
              isPersisted match {
                case true => HttpResponse(StatusCodes.Created,
                  entity = s"Data is successfully persisted")
                case false => HttpResponse(StatusCodes.InternalServerError,
                  entity = s"Error found for email")
              }
            } catch {
              case ex: Throwable =>
                logger.error(ex, ex.getMessage)
                HttpResponse(StatusCodes.InternalServerError,
                  entity = "Error while persisting data, please try again")
            }
          }
        }
      }
    } ~ path("unlikepost" / "postID" / Segment / "memberID" / Segment) { (postID: String, memberID: String) =>
      get {
        complete {
          try {
            val isPersisted = unlikePost(postID.toInt, memberID.toInt)
            isPersisted match {
              case true => HttpResponse(StatusCodes.Created,
                entity = s"Data is successfully persisted")
              case false => HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for  : $postID")
            }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError, entity = s"Error found for member")
          }
        }
      }
    } ~ path("new-article") {
      post {
        entity(as[Article]) { entity =>
          complete {
            try {
              val isPersisted: MutableList[GetArticle] = newArticle(entity)
              isPersisted match {
                case _: MutableList[_] =>
                  
                  var response: StringBuilder = new StringBuilder("[")
                  isPersisted.foreach(
                      x => response.append(x.toJson).append(",")
                    )
                  response.deleteCharAt(response.length - 1)
                  response.append("]"); 
                  
                  HttpResponse(StatusCodes.OK, entity = response.toString()) //data.toString())
                  case _ => HttpResponse(StatusCodes.BadRequest,
                   entity = s"Article does not exist or deleted")
              }
            } catch {
              case ex: Throwable =>
                logger.error(ex, ex.getMessage)
                HttpResponse(StatusCodes.InternalServerError,
                  entity = "Error while persisting data, please try again")
            }
          }
        }
      }
    } ~ path("get-article" / "memberID" / Segment) { (memberID: String) =>
      get {
        complete {
          try {
                val idAsRDD: MutableList[GetArticle] = retrieveArticle(memberID.toInt)
                idAsRDD match {
                  case _: MutableList[_] =>
                    var response: StringBuilder = new StringBuilder("[")
                  idAsRDD.foreach(
                      x => response.append(x.toJson).append(",")
                    )

                    // If records exist
                    if(response.length > 1) response.deleteCharAt(response.length - 1);
                    response.append("]");  
                    HttpResponse(StatusCodes.OK, entity = response.toString()) 

                    case _ => HttpResponse(StatusCodes.InternalServerError,
                    entity = s"Error found for user")           
                }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for post")
          }
        }
      }
    } ~ path("get-one-article" / "memberID" / Segment / "articleID" / Segment) { (memberID: String, articleID: String) =>
      get {
        complete {
          try {
                val idAsRDD: MutableList[GetArticle] = retrieveOneArticle(memberID.toInt, articleID.toInt)
                idAsRDD match {
                  case _: MutableList[_] =>
                    var response: StringBuilder = new StringBuilder("[")
                  idAsRDD.foreach(
                      x => response.append(x.toJson).append(",")
                    )

                    // If records exist
                    if(response.length > 1) response.deleteCharAt(response.length - 1);
                    response.append("]");  
                    HttpResponse(StatusCodes.OK, entity = response.toString()) 

                    case _ => HttpResponse(StatusCodes.InternalServerError,
                    entity = s"Error found for user")           
                }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for post")
          }
        }
      }
    } ~ path("all-article") {
      get {
        complete {
          try {
            val idAsRDD: MutableList[GetArticle] = retrieveallArticles()
              //val isPersisted: MutableList[User2] = login(entity)
              //val claims = setClaims("flavoursoft@yahoo.com", tokenExpiryPeriodInDays)
              idAsRDD match {
                case _: MutableList[_] =>
                  var response: StringBuilder = new StringBuilder("[")
                  idAsRDD.foreach(
                      x => response.append(x.toJson).append(",")
                    )
                  
                  // If records exist
                  if(response.length > 1) response.deleteCharAt(response.length - 1);
                  response.append("]"); 

                  HttpResponse(StatusCodes.OK, entity = response.toString()) //data.toString())
                  case _ => HttpResponse(StatusCodes.BadRequest,
                   entity = s"User does not exist")
              }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError,
                entity = s"Data is not fetched and something went wrong")
          }
        }
      }
    } ~ path("update-article") {
      put {
         entity(as[GetArticle]) { entity =>
          complete {
            try {
              val isPersisted = updateArticle(entity)
              isPersisted match {
                case true => HttpResponse(StatusCodes.Created,
                  entity = s"Data is successfully persisted")
                case false => HttpResponse(StatusCodes.InternalServerError,
                  entity = s"Error found for email ")
              }
            } catch {
              case ex: Throwable =>
                logger.error(ex, ex.getMessage)
                HttpResponse(StatusCodes.InternalServerError, entity = s"Error found for email ")
            }
          }
        }
      }
    } ~ path("delete-article" / Segment / Segment ) { (field: String, value:String) =>
      get {
        complete {
          try {
            val idAsRDD = deleteArticle(field, value)
            idAsRDD match {
              case 1 => HttpResponse(StatusCodes.OK, entity = "Data is successfully deleted")
              case 0 => HttpResponse(StatusCodes.InternalServerError,
                entity = s"Data is not deleted and something went wrong")
            }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for $field : $value")
          }
        }
      }
    } ~ path("new-article-comment") {
      post {
        entity(as[ArticleComment]) { entity =>
          complete {
            try {
              val isPersisted: MutableList[GetArticleComment] = newArticleComment(entity)
              isPersisted match {
                case _: MutableList[_] =>
                  
                  var response: StringBuilder = new StringBuilder("[")
                  isPersisted.foreach(
                      x => response.append(x.toJson).append(",")
                    )
                  response.deleteCharAt(response.length - 1)
                  response.append("]"); 
                  
                  HttpResponse(StatusCodes.OK, entity = response.toString()) //data.toString())
                  case _ => HttpResponse(StatusCodes.BadRequest,
                   entity = s"Comment already exist")
              }
            } catch {
              case ex: Throwable =>
                logger.error(ex, ex.getMessage)
                HttpResponse(StatusCodes.InternalServerError,
                  entity = "Error while persisting data, please try again")
            }
          }
        }
      }
    } ~ path("get-article-comments" / Segment / Segment ) { (ID: String, idValue: String) =>
      get {
        complete {
          try {
                val idAsRDD: MutableList[GetArticleComment] = retrieveArticleComments(ID, idValue.toInt)
                idAsRDD match {
                  case _: MutableList[_] =>
                    var response: StringBuilder = new StringBuilder("[")
                  idAsRDD.foreach(
                      x => response.append(x.toJson).append(",")
                    )

                    // If records exist
                    if(response.length > 1) response.deleteCharAt(response.length - 1);
                    response.append("]");  
                    HttpResponse(StatusCodes.OK, entity = response.toString()) 

                    case _ => HttpResponse(StatusCodes.InternalServerError,
                    entity = s"Error found for user")           
                }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for post")
          }
        }
      }
    } ~ path("get-article-comments" / Segment / Segment ) { (ID: String, idValue: String) =>
      get {
        complete {
          try {
                val idAsRDD: MutableList[GetArticleComment] = retrieveArticleComment(ID, idValue.toInt)
                idAsRDD match {
                  case _: MutableList[_] =>
                    var response: StringBuilder = new StringBuilder("[")
                  idAsRDD.foreach(
                      x => response.append(x.toJson).append(",")
                    )

                    // If records exist
                    if(response.length > 1) response.deleteCharAt(response.length - 1);
                    response.append("]");  
                    HttpResponse(StatusCodes.OK, entity = response.toString()) 

                    case _ => HttpResponse(StatusCodes.InternalServerError,
                    entity = s"Error found for user")           
                }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for post")
          }
        }
      }
    } ~ path("update-article-comment") {
      post {
         entity(as[GetArticleComment]) { entity =>
          complete {
            try {
              val isPersisted = updateArticleComment(entity)
              isPersisted match {
                case true => HttpResponse(StatusCodes.Created,
                  entity = s"Data is successfully persisted")
                case false => HttpResponse(StatusCodes.InternalServerError,
                  entity = s"Error found for email ")
              }
            } catch {
              case ex: Throwable =>
                logger.error(ex, ex.getMessage)
                HttpResponse(StatusCodes.InternalServerError, entity = s"Error found for email ")
            }
          }
        }
      }
    } ~ path("deletecmt" / Segment / Segment ) { (field: String, value:String) =>
      get {
        complete {
          try {
            val idAsRDD = deleteComment(field, value)
            idAsRDD match {
              case 1 => HttpResponse(StatusCodes.OK, entity = "Data is successfully deleted")
              case 0 => HttpResponse(StatusCodes.InternalServerError,
                entity = s"Data is not deleted and something went wrong")
            }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for $field : $value")
          }
        }
      }
    } ~ path("like-article") {
      put {
        entity(as[ArticleLike]) { entity =>
          complete {
            try {
              val isPersisted =  likeArticle(entity)
              isPersisted match {
                case true => HttpResponse(StatusCodes.Created,
                  entity = s"Data is successfully persisted")
                case false => HttpResponse(StatusCodes.InternalServerError,
                  entity = s"Error found for email")
              }
            } catch {
              case ex: Throwable =>
                logger.error(ex, ex.getMessage)
                HttpResponse(StatusCodes.InternalServerError,
                  entity = "Error while persisting data, please try again")
            }
          }
        }
      }
    } ~ path("unlike-article" / "articleID" / Segment / "memberID" / Segment) { (articleID: String, memberID: String) =>
      get {
        complete {
          try {
            val isPersisted = unlikeArticle(articleID.toInt, memberID.toInt)
            isPersisted match {
              case true => HttpResponse(StatusCodes.Created,
                entity = s"Data is successfully persisted")
              case false => HttpResponse(StatusCodes.InternalServerError,
                entity = s"Error found for  : $articleID")
            }
          } catch {
            case ex: Throwable =>
              logger.error(ex, ex.getMessage)
              HttpResponse(StatusCodes.InternalServerError, entity = s"Error found for member")
          }
        }
      }
    }

  }
}
