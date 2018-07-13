package com.careercolony.postservices.factories

import scala.concurrent.{ ExecutionContext, Future, Await }

import scala.concurrent.duration._

import reactivemongo.api.{ DefaultDB, MongoConnection, MongoDriver }

import reactivemongo.bson.{
  BSONDateTime, BSONDocument, BSONDocumentWriter, BSONDocumentReader, Macros, document
}
import reactivemongo.api.collections.bson.BSONCollection

import org.neo4j.driver.v1._

import com.typesafe.config.ConfigFactory
import java.security.MessageDigest

import scala.collection.mutable.MutableList;

import scala.util.{Failure, Success}

import akka.actor.ActorSystem
import akka.kafka.{ConsumerMessage, ProducerSettings}
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import akka.stream.ActorMaterializer

import akka.Done

import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}


case class Counter(_id: String, seq: Int)
case class Post(memberID:Int, title:Option[String], description:Option[String], post_type:Option[String], thumbnail_url:Option[String], post_url:Option[String], provider_url:Option[String], provider_name:Option[String], html:Option[String], post_date:String)
case class GetPost(memberID:Int, postID:Int, title:String, description: String, post_type:String, thumbnail_url:String, post_url:String, likes:Option[List[Int]], provider_url:Option[String], provider_name:Option[String], html:Option[String])
case class Comment(memberID:Int, postID:Int, text:String, thumbnail_url:Option[String], post_url:String, author:String, avatar:Option[String])
case class GetComment(memberID:Int, postID:Int, commentID:Int, text:String, thumbnail_url:Option[String], post_url:String, author:String, avatar:Option[String])
case class PostLike(memberID:Int, postID:Int)
case class GetPostLike(postlikeID: Int, memberID:Int, postID:Int)
case class CommentLike(memberID:Int, commentID:Int, postID:Int)
case class GetCommentLike(likeID: Int, commentID:Int, memberID:Int, postID:Int)

case class Article(memberID:Int, title:String, article_text:String, doc_type:String, thumbnail_url:String, article_url:String, author:String)
case class GetArticle(memberID:Int, articleID:Int, title:String, article_text: String, doc_type:String, thumbnail_url:String, article_url:String, author:String, article_likes:Option[List[Int]], article_comments:Option[List[Int]])
case class ArticleComment(memberID:Int, articleID:Int, text:String, thumbnail_url:Option[String], article_url:String, author:String, avatar:Option[String])
case class GetArticleComment(memberID:Int, articleID:Int, commentID:Int, text:String, thumbnail_url:Option[String], article_url:String, author:String, avatar:Option[String])
case class ArticleLike(memberID:Int, articleID:Int)
case class GetArticleLike(articlelikeID: Int, memberID:Int, articleID:Int)

case class ArticleCommentLike(memberID:Int, commentID:Int, articleID:Int)
case class ArticleGetCommentLike(likeID: Int, commentID:Int, memberID:Int, articleID:Int)

case class Address(city: String, state: String)
case class User2(memberID: Int, firstname: String, lastname: String, email: String)
case class User3(memberID: Int, firstname: String, lastname: String, email: String, interest: String, employmentstatus: String, avatar: String)
case class User(firstname: String, lastname: String, email: String, password: String, address: Option[Address])
case class ListUser(companies: MutableList[User2])
case class ResponseStatus(status: Int, message: String, details: String)
case class GetJobTitle(position: String)
case class BioData(memberID: Int, userIP: Option[String], email:String, country:String, interest:String, employmentstatus:String, employer_name:Option[String], position:Option[String], industry:Option[String], degree:Option[String], school_name:Option[String])
case class Experience(expID: Int, memberID: Int, employer: String, position: String, industry: String, updated_date: String)
case class Education(eduID: Int, memberID: Int, school_name: String, degree: String, updated_date: String)


case class Feeds( memberID: Int, feedType:String, text:String, title:Option[String], media_type: String, media_url: String, postID: Int, post_url:String, articleID: Int, postDate: String)
case class GetFeeds(feedID:Int, memberID: Int, feedType:String, text:String, title:Option[String], media_type: String, media_url: String, postID: Int,post_url:String,articleID: Int,postDate: String)

trait DatabaseAccess {
  
  val config = ConfigFactory.load("application.conf")

  implicit val kafkasys: ActorSystem = ActorSystem("Post-Akka-Service")
  implicit val kafkamaterializer = ActorMaterializer.create(kafkasys)

  
  // Neo4j configs
  val neo4jUrl = config.getString("neo4j.url")
  val userName = config.getString("neo4j.userName")
  val userPassword = config.getString("neo4j.userPassword")

  // Mongo configs
  // My settings (see available connection options)
  val mongoUri = config.getString("mongo.url")
  val username = config.getString("mongo.username")
  val password = config.getString("mongo.password")
  val database = config.getString("mongo.database")
  val database_article = config.getString("mongo.database_article")
  val database_feeds = config.getString("mongo.database_feeds")

  import ExecutionContext.Implicits.global // use any appropriate context

  // Connect to the database: Must be done only once per application
  val driver = MongoDriver()
  val parsedUri = MongoConnection.parseURI(mongoUri)
  val connection = parsedUri.map(driver.connection(_))

  // Database and collections: Get references
  val futureConnection = Future.fromTry(connection)
  def db: Future[DefaultDB] = futureConnection.flatMap(_.database(database))
  def db1: Future[DefaultDB] = futureConnection.flatMap(_.database(database_article))
  def db2: Future[DefaultDB] = futureConnection.flatMap(_.database(database_feeds))
  //def db3: Future[DefaultDB] = futureConnection.flatMap(_.database(database_activities))

  def userCollection = db.map(_.collection("user"))
  def experienceCollection = db.map(_.collection("experience"))
  def educationCollection = db.map(_.collection("education"))

  def counterCollection = db.map(_.collection("counters"))
  def postCollection = db.map(_.collection("post"))
  def articleCollection = db1.map(_.collection("article"))
  def commentCollection = db.map(_.collection("comments"))
  def postlikeCollection = db.map(_.collection("postlikes"))
  def feedCollection = db2.map(_.collection("feed"))
  //def activityCollection = db3.map(_.collection("activity"))
  
  // Write Documents: insert or update
  implicit def postWriter: BSONDocumentWriter[Post] = Macros.writer[Post]
  implicit def getpostWriter: BSONDocumentWriter[GetPost] = Macros.writer[GetPost]
  implicit def articleWriter: BSONDocumentWriter[Article] = Macros.writer[Article]
  implicit def getarticleWriter: BSONDocumentWriter[GetArticle] = Macros.writer[GetArticle]
  implicit def commentWriter: BSONDocumentWriter[Comment] = Macros.writer[Comment]
  implicit def getcommentWriter: BSONDocumentWriter[GetComment] = Macros.writer[GetComment]

  implicit def articlecommentWriter: BSONDocumentWriter[ArticleComment] = Macros.writer[ArticleComment]
  implicit def getarticlecommentWriter: BSONDocumentWriter[GetArticleComment] = Macros.writer[GetArticleComment]
  implicit def likeWriter: BSONDocumentWriter[PostLike] = Macros.writer[PostLike]
  implicit def getlikeWriter: BSONDocumentWriter[GetPostLike] = Macros.writer[GetPostLike]

  implicit def articlelikeWriter: BSONDocumentWriter[ArticleLike] = Macros.writer[ArticleLike]
  implicit def getarticlelikeWriter: BSONDocumentWriter[GetArticleLike] = Macros.writer[GetArticleLike]
  
  
  implicit def feedWriter: BSONDocumentWriter[Feeds] = Macros.writer[Feeds]

  implicit def addressWriter: BSONDocumentWriter[Address] = Macros.writer[Address]
  implicit def userWriter: BSONDocumentWriter[User] = Macros.writer[User]

  implicit def experienceWriter: BSONDocumentWriter[Experience] = Macros.writer[Experience]
  implicit def educationWriter: BSONDocumentWriter[Education] = Macros.writer[Education]
  
  // or provide a custom one
  implicit def counterReader: BSONDocumentReader[Counter] = Macros.reader[Counter]
  implicit def postReader: BSONDocumentReader[Post] = Macros.reader[Post]
  implicit def getpostReader: BSONDocumentReader[GetPost] = Macros.reader[GetPost]
  implicit def articleReader: BSONDocumentReader[Article] = Macros.reader[Article]
  implicit def getarticleReader: BSONDocumentReader[GetArticle] = Macros.reader[GetArticle]
  implicit def commentReader: BSONDocumentReader[Comment] = Macros.reader[Comment]
  implicit def getcommentReader: BSONDocumentReader[GetComment] = Macros.reader[GetComment]
  implicit def articlecommentReader: BSONDocumentReader[ArticleComment] = Macros.reader[ArticleComment]
  implicit def getarticlecommentReader: BSONDocumentReader[GetArticleComment] = Macros.reader[GetArticleComment]

  implicit def likeReader: BSONDocumentReader[PostLike] = Macros.reader[PostLike]
  implicit def getlikeReader: BSONDocumentReader[GetPostLike] = Macros.reader[GetPostLike]
  implicit def getfeedReader: BSONDocumentReader[GetFeeds] = Macros.reader[GetFeeds]

  implicit def addressReader: BSONDocumentReader[Address] = Macros.reader[Address]
  implicit def userReader: BSONDocumentReader[User] = Macros.reader[User]
  implicit def user2Reader: BSONDocumentReader[User2] = Macros.reader[User2]
  implicit def user3Reader: BSONDocumentReader[User3] = Macros.reader[User3]

  implicit def experienceReader: BSONDocumentReader[Experience] = Macros.reader[Experience]
  // or provide a custom one

  def getNextSequence(idKey: String) = {

    val f = counterCollection.flatMap(
      _.findAndUpdate(BSONDocument("_id" -> idKey), BSONDocument (
        "$inc" -> BSONDocument("seq" -> 1 )
        ), 
        fetchNewObject = true).map(_.result[Counter]))


    val result = Await.result(f, 5000 millis)

    val ret:Int = result match {
      case None => -1
      case Some(c: Counter) => c.seq
    }

    ret
  }

  

  def newPost(p: Post): MutableList[GetPost] = {

    val records = MutableList[GetPost]()
   
      val postDoc = BSONDocument (
        "memberID" -> p.memberID,
        "title" -> p.title, 
        "description" -> p.description, 
        "post_type" -> p.post_type, 
        "thumbnail_url" -> p.thumbnail_url,
        "post_url" -> p.post_url, 
        "post_date" -> p.post_date,
        "postID" -> getNextSequence("postID"),
        "provider_url" -> p.provider_url,
        "provider_name"-> p.provider_name,
        "html"-> p.html
      )

      println(postDoc.getAs[Int]("postID"))

      val postID = postDoc.getAs[Int]("postID")

      val inst = postCollection.flatMap(_.insert(postDoc).map(_.n)) //.map(_ => {})) // use userWriter

      var n: Int = 0
      inst.onComplete {
        case Success(value) => {
          n = value
        }
        case Failure(e) => e.printStackTrace
      }

      Await.result(inst, 10000 millis)  

      // Insert data to feed collection
      val feedDoc = BSONDocument (
        "memberID" -> p.memberID,
        "title" -> p.title, 
        "feedType" -> p.post_type, 
        "text" -> p.description, 
        "media_type"-> p.thumbnail_url,
        "media_url" -> p.thumbnail_url,
        "post_url" -> p.post_url, 
        "postDate" -> BSONDateTime(System.currentTimeMillis),
        "feedID" -> getNextSequence("feedID"),
        "postID" -> postID,
        "provider_url" -> p.provider_url,
        "provider_name"-> p.provider_name,
        "html"-> p.html
      )
      val feedinst = feedCollection.flatMap(_.insert(feedDoc).map(_.n)) //.map(_ => {})) // use userWriter

      // Insert feed to neo4j
       val driver = GraphDatabase.driver(neo4jUrl, AuthTokens.basic(userName, userPassword))
       val session = driver.session
       val script = s"CREATE (s:feeds {memberID:${p.memberID}, title:'${p.title}', post_date: TIMESTAMP()})"
       val result: StatementResult = session.run(script)
       session.close()
       driver.close()
       result.consume().counters().containsUpdates()
      
      // Retreive posts from post collection
      val f = postCollection.flatMap(_.find(document(
        "postID" -> postID
        )). // query builder
        cursor[GetPost]().collect[List]()) // collect using the result cursor

      f.onComplete {
        case Success(result) => 
          result.foreach (
            record => {
            val post: GetPost = new GetPost(
              record.memberID,
              record.postID, 
              record.title,
              record.description,
              record.post_type,
              record.thumbnail_url,
              record.post_url,
              record.likes,
              record.provider_url,
              record.provider_name,
              record.html
              //record.post_date
            )
            
            records += post
          }) 
        case Failure(e) => { 
          e.printStackTrace
          println("Error in getting post...")
        }
      }

      Await.result(f, 10000 millis)

    records
  }

  def retrievePost(memberID: Int): MutableList[GetPost] = {

    val f = postCollection.flatMap(_.find(document(
      "memberID" -> memberID
      )). // query builder
      cursor[GetPost]().collect[List]()) // collect using the result cursor
    
    val records = MutableList[GetPost]()
    println(memberID)

    f.onComplete {
        case Success(result) => 
          result.foreach(
            record => {

              val post: GetPost = new GetPost(
                record.memberID,
                record.postID, 
                record.title,
                record.description,
                record.post_type,
                record.thumbnail_url,
                record.post_url,
                record.likes,
                record.provider_url,
                record.provider_name,
                record.html
                //record.post_date 
              )

              records += post
            }
          )
        case Failure(e) => { 
          e.printStackTrace
          println("Error in retrieveRecord..")
        }
    }

    Await.result(f, 10000 millis);
    records
  }

   def retrieveOnePost(memberID: Int, postID: Int): MutableList[GetPost] = {

    val f = postCollection.flatMap(_.find(document(
      "memberID" -> memberID, "postID" -> postID
      )). // query builder
      cursor[GetPost]().collect[List]()) // collect using the result cursor
    
    val records = MutableList[GetPost]()

    f.onComplete {
        case Success(result) => 
          result.foreach(
            record => {

              val post: GetPost = new GetPost(
                record.memberID,
                record.postID, 
                record.title,
                record.description,
                record.post_type,
                record.thumbnail_url,
                record.post_url,
                record.likes,
                record.provider_url,
                record.provider_name,
                record.html
                //record.post_date 
              )

              records += post
            }
          )
        case Failure(e) => { 
          e.printStackTrace
          println("Error in retrieveRecord..")
        }
    }

    Await.result(f, 1000 millis);
    records
  }
  def retrieveallPosts() =  {

    val f = postCollection.flatMap(_.find(document(
      )). // query builder
      cursor[GetPost]().collect[List]()) // collect using the result cursor
    
    val records = MutableList[GetPost]()

    f.onComplete {
        case Success(result) => 
          result.foreach (
            record => {
              val post: GetPost = new GetPost(
                record.memberID,
                record.postID, 
                record.title,
                record.description,
                record.post_type,
                record.thumbnail_url,
                record.post_url,
                record.likes,
                record.provider_url,
                record.provider_name,
                record.html
                //record.post_date 
              )
              records += post
            }
          )
        case Failure(e) => { 
          e.printStackTrace
          None
        }
    }

    Await.result(f, 1000 millis);
    println("Returning: " + records)
    records
  }

  def updatePost(p: GetPost): Boolean = {
    val selector = BSONDocument("postID" -> p.postID)

    val modifier = BSONDocument ( "$set" ->BSONDocument (
      "memberID" -> p.memberID,
      "postID" -> p.postID,
      "title" -> p.title, 
      "description" -> p.description, 
      "post_type" -> p.post_type, 
      "thumbnail_url" -> p.thumbnail_url,
      "post_url" -> p.post_url 
    ))
    
    println(p.postID)
    val f = postCollection.flatMap(
      _.update(selector, modifier).map(_.n))

    var ret:Int = 0
    f.map(n => {
        ret = n
      }
    )
    

    Await.result(f, 1000 millis)

    if(ret > 0) true else false

  }



  def deletePost(field: String, value: String): Int = {

    val selector = if(value forall Character.isDigit) BSONDocument(field -> value.toInt) else BSONDocument(field -> value) 
    val f = postCollection.flatMap(_.remove(selector).map(_.n)) // collect using the result cursor

   
    
    val i = Await.result(f, 5000 millis)

    println("Del count: " + i)

    i    
  }


 def newComment(c: Comment): MutableList[GetComment] = {

    val records = MutableList[GetComment]()
   
      val commentDoc = BSONDocument (
        "memberID" -> c.memberID,
        "text" -> c.text, 
        "author" -> c.author, 
        "thumbnail_url" -> c.thumbnail_url,
        "post_url" -> c.post_url, 
        "avatar" -> c.avatar,
        "comment_date" -> BSONDateTime(System.currentTimeMillis),
        "postID" -> c.postID,
        "commentID" -> getNextSequence("commentID")
      )

      println(commentDoc.getAs[Int]("commentID"))

      val commentID = commentDoc.getAs[Int]("commentID")

      val inst = commentCollection.flatMap(_.insert(commentDoc).map(_.n)) //.map(_ => {})) // use userWriter

      var n: Int = 0
      inst.onComplete {
        case Success(value) => {
          n = value
        }
        case Failure(e) => e.printStackTrace
      }

      Await.result(inst, 5000 millis)  

      val f = commentCollection.flatMap(_.find(document(
        "commentID" -> commentID
        )). // query builder
        cursor[GetComment]().collect[List]()) // collect using the result cursor

      f.onComplete {
        case Success(result) => 
          result.foreach (
            record => {
            val comment: GetComment = new GetComment(
              record.memberID,
              record.postID,
              record.commentID,
              record.text,
              record.thumbnail_url,
              record.post_url,
              record.author,
              record.avatar
              //record.comment_date
            )

            records += comment
          }) 
        
        // Add to comment list on post collection
        val modifier = BSONDocument("$addToSet" -> BSONDocument("comments"->c.memberID))
        val selector = BSONDocument("postID" -> c.postID)
        val l = postCollection.flatMap(_.update(selector, modifier).map(_.n))
        
        case Failure(e) => { 
          e.printStackTrace
          println("Error in getting post...")
        }
      }

      Await.result(f, 5000 millis)

    records
  }

  def retrieveComments(ID: String, idValue: Int): MutableList[GetComment] = {

    val f = commentCollection.flatMap(_.find(document(
      ID -> idValue
      )). // query builder
      cursor[GetComment]().collect[List]()) // collect using the result cursor
    
    val records = MutableList[GetComment]()
    println(idValue)

    f.onComplete {
        case Success(result) => 
          result.foreach(
            record => {

              val comment: GetComment = new GetComment(
                record.memberID,
                record.postID,
                record.commentID,
                record.text,
                record.thumbnail_url,
                record.post_url,
                record.author,
                record.avatar
                //record.comment_date
              )

              records += comment
            }
          )
        case Failure(e) => { 
          e.printStackTrace
          println("Error in retrieveRecord..")
        }
    }

    Await.result(f, 1000 millis);
    records
  }

  def retrievePostComments(ID: String, idValue: Int): MutableList[GetComment] = {

    val f = commentCollection.flatMap(_.find(document(
      ID -> idValue,
      "parentCommentID" -> -1
      )). // query builder
      cursor[GetComment]().collect[List]()) // collect using the result cursor
    
    val records = MutableList[GetComment]()
    println(idValue)

    f.onComplete {
        case Success(result) => 
          result.foreach(
            record => {

              val comment: GetComment = new GetComment(
                record.memberID,
                record.postID,
                record.commentID,
                record.text,
                record.thumbnail_url,
                record.post_url,
                record.author,
                record.avatar
                //record.comment_date
              )

              records += comment
            }
          )
        case Failure(e) => { 
          e.printStackTrace
          println("Error in retrieveRecord..")
        }
    }

    Await.result(f, 1000 millis);
    records
  }

  def updateComment(c: GetComment): Boolean = {
    val selector = BSONDocument("commentID" -> c.commentID)

    val modifier = BSONDocument("$set" ->BSONDocument (
      "postID" ->c.postID,
      "text" -> c.text, 
      "author" -> c.author, 
      "avatar" -> c.avatar, 
      "author" -> c.author, 
      "thumbnail_url" -> c.thumbnail_url,
      "post_url" -> c.post_url 
    ))
    
    println(c.commentID)
    val f = commentCollection.flatMap(
      _.update(selector, modifier).map(_.n))

    var ret:Int = 0
    f.map(n => {
        ret = n
      }
    )
    

    Await.result(f, 1000 millis)

    if(ret > 0) true else false

  }


  def deleteComment(field: String, value: String): Int = {

    val selector = if(value forall Character.isDigit) BSONDocument(field -> value.toInt) else BSONDocument(field -> value) 
    val f = commentCollection.flatMap(_.remove(selector).map(_.n)) // collect using the result cursor
    
    val i = Await.result(f, 5000 millis)

    println("Del count: " + i)

    i    
  }

  def likePost(p:PostLike): Boolean = {
    val modifier = BSONDocument("$addToSet" -> BSONDocument("likes"->p.memberID))

    val selector = BSONDocument("postID" -> p.postID)

    val f = postCollection.flatMap(
      _.update(selector, modifier).map(_.n))

    var ret:Int = 0
    f.map(n => {
      ret = n
    }
    )


    Await.result(f, 1000 millis)

    if(ret > 0) true else false

  }
  

  def unlikePost(postID: Int, memberID: Int): Boolean = {
    val modifier = BSONDocument("$pull" -> BSONDocument("likes"->memberID))

    val selector = BSONDocument("postID"->postID)


    val f = postCollection.flatMap(
      _.update(selector, modifier).map(_.n))

    var ret:Int = 0
    f.map(n => {
      ret = n
    }
    )


    Await.result(f, 1000 millis)

    if(ret > 0) true else false
  }
  
  // Articles 

  def newArticle(p: Article): MutableList[GetArticle] = {

    val records = MutableList[GetArticle]()
   
      val articleDoc = BSONDocument (
        "memberID" -> p.memberID,
        "title" -> p.title, 
        "article_text" -> p.article_text, 
        "doc_type" -> p.doc_type, 
        "author" -> p.author, 
        "thumbnail_url" -> p.thumbnail_url,
        "article_url" -> p.article_url, 
        "post_date" -> BSONDateTime(System.currentTimeMillis),
        "articleID" -> getNextSequence("articleID")
      )

      println(articleDoc.getAs[Int]("articleID"))

      val articleID = articleDoc.getAs[Int]("articleID")

      val inst = articleCollection.flatMap(_.insert(articleDoc).map(_.n)) //.map(_ => {})) // use userWriter

      val articleID_Val : Int = articleID match {case Some(str) => str} // Strip Some()

      //create article comment in neo4j
       val driver = GraphDatabase.driver(neo4jUrl, AuthTokens.basic(userName, userPassword))
       val session = driver.session
       val script = s"CREATE (s:Feeds {memberID:${p.memberID}, type:'article', feed_date: TIMESTAMP()}) "
       val result: StatementResult = session.run(script)
       session.close()
       driver.close()
       result.consume().counters().containsUpdates()
      
      var n: Int = 0
      inst.onComplete {
        case Success(value) => {
          n = value
        }
        case Failure(e) => e.printStackTrace
      }

      Await.result(inst, 5000 millis)  


      // Insert data to feed collection
      val feedDoc = BSONDocument (
        "memberID" -> p.memberID,
        "title" -> p.title, 
        "feedType" -> p.doc_type, 
        "text" -> p.article_text, 
        "media_type"-> p.thumbnail_url,
        "media_url" -> p.thumbnail_url,
        "article_url" -> p.article_url, 
        "postDate" -> BSONDateTime(System.currentTimeMillis),
        "feedID" -> getNextSequence("feedID"),
        "articleID" -> articleID
      )
      val feedinst = feedCollection.flatMap(_.insert(feedDoc).map(_.n)) //.map(_ => {})) // use userWriter

      // Retreive posts from post collection
      val f = articleCollection.flatMap(_.find(document(
        "articleID" -> articleID
        )). // query builder
        cursor[GetArticle]().collect[List]()) // collect using the result cursor

      f.onComplete {
        case Success(result) => 
          result.foreach (
            record => {
            val article: GetArticle = new GetArticle(
              record.memberID,
              record.articleID, 
              record.title,
              record.article_text,
              record.doc_type,
              record.thumbnail_url,
              record.article_url,
              record.author,
              record.article_likes,
              record.article_comments
              //record.post_date
            )
            
            records += article
          }) 
        case Failure(e) => { 
          e.printStackTrace
          println("Error in getting post...")
        }
      }

      Await.result(f, 5000 millis)

    records
  }

  def retrieveArticle(memberID: Int): MutableList[GetArticle] = {

    val f = articleCollection.flatMap(_.find(document(
      "memberID" -> memberID
      )). // query builder
      cursor[GetArticle]().collect[List]()) // collect using the result cursor
    
    val records = MutableList[GetArticle]()
    println(memberID)

    f.onComplete {
        case Success(result) => 
          result.foreach(
            record => {

              val article: GetArticle = new GetArticle(
                record.memberID,
                record.articleID, 
                record.title,
                record.article_text,
                record.doc_type,
                record.thumbnail_url,
                record.article_url,
                record.author,
                record.article_likes,
                record.article_comments
                //record.post_date 
              )

              records += article
            }
          )
        case Failure(e) => { 
          e.printStackTrace
          println("Error in retrieveRecord..")
        }
    }

    Await.result(f, 1000 millis);
    records
  }

   def retrieveOneArticle(memberID: Int, articleID: Int): MutableList[GetArticle] = {

    val f = articleCollection.flatMap(_.find(document(
      "memberID" -> memberID, "articleID" -> articleID
      )). // query builder
      cursor[GetArticle]().collect[List]()) // collect using the result cursor
    
    val records = MutableList[GetArticle]()

    f.onComplete {
        case Success(result) => 
          result.foreach(
            record => {

              val article: GetArticle = new GetArticle(
                record.memberID,
                record.articleID, 
                record.title,
                record.article_text,
                record.doc_type,
                record.thumbnail_url,
                record.article_url,
                record.author,
                record.article_likes,
                record.article_comments
                //record.post_date 
              )

              records += article
            }
          )
        case Failure(e) => { 
          e.printStackTrace
          println("Error in retrieveRecord..")
        }
    }

    Await.result(f, 1000 millis);
    records
  }
  def retrieveallArticles() =  {

    val f = articleCollection.flatMap(_.find(document(
      )). // query builder
      cursor[GetArticle]().collect[List]()) // collect using the result cursor
    
    val records = MutableList[GetArticle]()

    f.onComplete {
        case Success(result) => 
          result.foreach (
            record => {
              val article: GetArticle = new GetArticle(
                record.memberID,
                record.articleID, 
                record.title,
                record.article_text,
                record.doc_type,
                record.thumbnail_url,
                record.article_url,
                record.author,
                record.article_likes,
                record.article_comments
                //record.post_date 
              )
              records += article
            }
          )
        case Failure(e) => { 
          e.printStackTrace
          None
        }
    }

    Await.result(f, 1000 millis);
    println("Returning: " + records)
    records
  }

  def updateArticle(p: GetArticle): Boolean = {
    val selector = BSONDocument("articleID" -> p.articleID)

    val modifier = BSONDocument ( "$set" ->BSONDocument (
      "memberID" -> p.memberID,
      "articleID" -> p.articleID,
      "title" -> p.title, 
      "article_text" -> p.article_text, 
      "doc_type" -> p.doc_type, 
      "author" -> p.author, 
      "thumbnail_url" -> p.thumbnail_url,
      "article_url" -> p.article_url 
    ))
    
    println(p.articleID)
    val f = articleCollection.flatMap(
      _.update(selector, modifier).map(_.n))

    var ret:Int = 0
    f.map(n => {
        ret = n
      }
    )
    

    Await.result(f, 1000 millis)

    if(ret > 0) true else false

  }



  def deleteArticle(field: String, value: String): Int = {

    val selector = if(value forall Character.isDigit) BSONDocument(field -> value.toInt) else BSONDocument(field -> value) 
    val f = articleCollection.flatMap(_.remove(selector).map(_.n)) // collect using the result cursor

   
    
    val i = Await.result(f, 5000 millis)

    println("Del count: " + i)

    i    
  }


 def newArticleComment(c: ArticleComment): MutableList[GetArticleComment] = {

    val records = MutableList[GetArticleComment]()
   
      val commentDoc = BSONDocument (
        "memberID" -> c.memberID,
        "text" -> c.text, 
        "author" -> c.author, 
        "thumbnail_url" -> c.thumbnail_url,
        "article_url" -> c.article_url, 
        "avatar" -> c.avatar,
        "comment_date" -> BSONDateTime(System.currentTimeMillis),
        "articleID" -> c.articleID,
        "commentID" -> getNextSequence("commentID")
      )

      println(commentDoc.getAs[Int]("commentID"))

      val commentID = commentDoc.getAs[Int]("commentID")

      val commentID_Val : Int = commentID match {case Some(str) => str} // Strip Some()

      val inst = articleCollection.flatMap(_.insert(commentDoc).map(_.n)) //.map(_ => {})) // use userWriter

      //create article comment in neo4j
       val driver = GraphDatabase.driver(neo4jUrl, AuthTokens.basic(userName, userPassword))
       val session = driver.session
       val script = s"CREATE (s:Feeds {memberID:${c.memberID}, type:'comment', feed_date: TIMESTAMP()}) "
       val result: StatementResult = session.run(script)
       session.close()
       driver.close()
       result.consume().counters().containsUpdates()

      var n: Int = 0
      inst.onComplete {
        case Success(value) => {
          n = value
        }
        case Failure(e) => e.printStackTrace
      }

      Await.result(inst, 5000 millis)  

      val f = articleCollection.flatMap(_.find(document(
        "commentID" -> commentID
        )). // query builder
        cursor[GetArticleComment]().collect[List]()) // collect using the result cursor

      f.onComplete {
        case Success(result) => 
          result.foreach (
            record => {
            val comment: GetArticleComment = new GetArticleComment(
              record.memberID,
              record.articleID,
              record.commentID,
              record.text,
              record.thumbnail_url,
              record.article_url,
              record.author,
              record.avatar
              //record.comment_date
            )

            records += comment
          }) 
        
        // Add to comment list on post collection
        val modifier = BSONDocument("$addToSet" -> BSONDocument("comments"->c.memberID))
        val selector = BSONDocument("articleID" -> c.articleID)
        val l = articleCollection.flatMap(_.update(selector, modifier).map(_.n))
        
        case Failure(e) => { 
          e.printStackTrace
          println("Error in getting post...")
        }
      }

      Await.result(f, 5000 millis)

    records
  }

  def retrieveArticleComments(ID: String, idValue: Int): MutableList[GetArticleComment] = {

    val f = articleCollection.flatMap(_.find(document(
      ID -> idValue
      )). // query builder
      cursor[GetArticleComment]().collect[List]()) // collect using the result cursor
    
    val records = MutableList[GetArticleComment]()
    println(idValue)

    f.onComplete {
        case Success(result) => 
          result.foreach(
            record => {

              val comment: GetArticleComment = new GetArticleComment(
                record.memberID,
                record.articleID,
                record.commentID,
                record.text,
                record.thumbnail_url,
                record.article_url,
                record.author,
                record.avatar
                //record.comment_date
              )
              records += comment
            }
          )
        case Failure(e) => { 
          e.printStackTrace
          println("Error in retrieveRecord..")
        }
    }

    Await.result(f, 1000 millis);
    records
  }

  def retrieveArticleComment(ID: String, idValue: Int): MutableList[GetArticleComment] = {

    val f = articleCollection.flatMap(_.find(document(
      ID -> idValue,
      "parentCommentID" -> -1
      )). // query builder
      cursor[GetArticleComment]().collect[List]()) // collect using the result cursor
    
    val records = MutableList[GetArticleComment]()
    println(idValue)

    f.onComplete {
        case Success(result) => 
          result.foreach(
            record => {

              val comment: GetArticleComment = new GetArticleComment(
                record.memberID,
                record.articleID,
                record.commentID,
                record.text,
                record.thumbnail_url,
                record.article_url,
                record.author,
                record.avatar
                //record.comment_date
              )

              records += comment
            }
          )
        case Failure(e) => { 
          e.printStackTrace
          println("Error in retrieveRecord..")
        }
    }

    Await.result(f, 1000 millis);
    records
  }

  def updateArticleComment(c: GetArticleComment): Boolean = {
    val selector = BSONDocument("commentID" -> c.commentID)

    val modifier = BSONDocument("$set" ->BSONDocument (
      "articleID" ->c.articleID,
      "commentID"->c.commentID,
      "text" -> c.text, 
      "author" -> c.author, 
      "avatar" -> c.avatar, 
      "author" -> c.author, 
      "thumbnail_url" -> c.thumbnail_url,
      "article_url" -> c.article_url 
    ))
    
    println(c.commentID)
    val f = commentCollection.flatMap(
      _.update(selector, modifier).map(_.n))

    var ret:Int = 0
    f.map(n => {
        ret = n
      }
    )
    

    Await.result(f, 1000 millis)

    if(ret > 0) true else false

  }


  def deleteArticleComment(field: String, value: String): Int = {

    val selector = if(value forall Character.isDigit) BSONDocument(field -> value.toInt) else BSONDocument(field -> value) 
    val f = commentCollection.flatMap(_.remove(selector).map(_.n)) // collect using the result cursor
    
    val i = Await.result(f, 5000 millis)

    println("Del count: " + i)

    i    
  }

  def likeArticle(p:ArticleLike): Boolean = {
    val modifier = BSONDocument("$addToSet" -> BSONDocument("likes"->p.memberID))

    val selector = BSONDocument("articleID" -> p.articleID)

    val f = articleCollection.flatMap(
      _.update(selector, modifier).map(_.n))

      //create article like in neo4j
       val driver = GraphDatabase.driver(neo4jUrl, AuthTokens.basic(userName, userPassword))
       val session = driver.session
       val script = s"CREATE (s:Feeds {memberID:${p.memberID}, type:'like', feed_date: TIMESTAMP()}) "
       val result: StatementResult = session.run(script)
       session.close()
       driver.close()
       result.consume().counters().containsUpdates()

    var ret:Int = 0
    f.map(n => {
      ret = n
    }
    )


    Await.result(f, 1000 millis)

    if(ret > 0) true else false

  }
  

  def unlikeArticle(articleID: Int, memberID: Int): Boolean = {
    val modifier = BSONDocument("$pull" -> BSONDocument("likes"->memberID))

    val selector = BSONDocument("articleID"->articleID)


    val f = articleCollection.flatMap(
      _.update(selector, modifier).map(_.n))

    var ret:Int = 0
    f.map(n => {
      ret = n
    }
    )


    Await.result(f, 1000 millis)

    if(ret > 0) true else false
  }


  


  
}

object DatabaseAccess extends DatabaseAccess




