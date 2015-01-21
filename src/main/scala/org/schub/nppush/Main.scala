package org.schub.nppush

import akka.actor._
import akka.event.Logging
import akka.io.IO
import akka.pattern.ask
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Failure}
import spray.can.Http
import spray.client.pipelining._
import spray.http._
import spray.util._

import org.schub.nppush.Protocol._
import java.util.Date
import java.nio.file.{Files, Paths}
import java.io.File


case class GetPadContentResult(text: String)

/**
 * Main class. Application entry point.
 */
object Main extends App {

  // print some user info
  Console.println(
    """nppush - periodically loads content from a source url and updates a word press blog post.
      |
      |press 'x' and enter to exit
      |
    """.stripMargin)

  // startup akka
  implicit val system = ActorSystem("nppush")
  val log = Logging(system, getClass)

  // create config
  val config = Configuration

  // schedule blog update
  val actor = system.actorOf(Props(new GetPadContentActor()))
  system.scheduler.schedule(1.seconds, config.updatePeriod.seconds, actor, GetPadContent)

  // process user input
  for (ln <- io.Source.stdin.getLines) {
    if (ln.startsWith("x")) {
      shutdown()
    }
  }

  def shutdown(): Unit = {
    Console.println("cleanup and shutdown")
    IO(Http).ask(Http.CloseAll)(1.second).await
    system.shutdown()
    System.exit(0)
  }
}

/**
 * Configuration object.
 */
object Configuration {

  lazy val conf = ConfigFactory.load()

  lazy val username = conf.getString("username")

  lazy val password = conf.getString("password")

  lazy val blogId = conf.getString("blogId")

  lazy val postId = conf.getString("postId")

  lazy val wpUrl = conf.getString("wpUrl")

  lazy val sourceUrl = conf.getString("sourceUrl")

  lazy val updatePeriod = conf.getInt("updatePeriod")

  lazy val backupDir = conf.getString("backupDir")
}

/**
 * Actor class for getting a wordpress blog post via xml-rpc api.
 */
class GetBlogPostActor extends Actor with ActorLogging {

  val config = Configuration

  def receive = {
    case data: GetBlogPost => {

      val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

      val response: Future[HttpResponse] = pipeline(Post(config.wpUrl + "/xmlrpc.php", data))

      response onComplete {

        case Success(result) => {
          Console.println("got blog post: " + result.entity.asString.length)
        }

        case Failure(error) => {
          log.error(error, "Could not get blog post")
        }
      }

    }
  }
}

/**
 * Actor class for updating a wordpress blog post via xml-rpc api.
 */
class UpdateBlogPostActor extends Actor with ActorLogging {

  val config = Configuration

  def receive = {

    case pad: PadContent => {

      Console.println("pad content received. " + pad.content.length + " characters")

      // backup pad content
      val textWriterActor = context.actorOf(Props(new TextWriteActor()))
      textWriterActor ! pad

      // update blog post
      val data = UpdateBlogPost(config.blogId, config.username, config.password, config.postId, BlogPostContent(pad.content))

      val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
      val response: Future[HttpResponse] = pipeline(Post(config.wpUrl + "/xmlrpc.php", data))

      response onComplete {

        case Success(result) => {
          Console.println("blog post updated")
        }

        case Failure(error) => {
          log.error(error, "error updating blog post")
        }
      }
    }
  }
}

/**
 * Actor class for getting a text via http get.
 */
class GetPadContentActor extends Actor with ActorLogging {

  val config = Configuration

  def receive = {

    case GetPadContent => {

      Console.println("get pad content")
      val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
      val response: Future[HttpResponse] = pipeline(Get(config.sourceUrl))
      response onComplete {

        case Success(result) => {
          val pad = result.entity.asString
          context.actorOf(Props(new UpdateBlogPostActor())) ! PadContent(pad)
        }

        case Failure(error) => {
          log.error(error, "Could not get pad content")
        }
      }

    }
  }
}

/**
 * Actor class for writing text to disk.
 */
class TextWriteActor extends Actor with ActorLogging {

  val config = Configuration

  val format = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")

  def receive = {

    case pad: PadContent => {

      // check if backup directory exists and create one if not
      Files.exists(Paths.get(config.backupDir)) match {
        case true => // do nothing
        case false => {
          val dir = new File(config.backupDir)
          dir.mkdir()
        }
      }

      val fileName = config.backupDir + "/pad_backup_" + format.format(new Date()) + ".txt"
      val pw = new java.io.PrintWriter(new File(fileName))

      try {
        pw.write(pad.content)
        Console.println("wrote pad content to backup dir: " + fileName)
      } catch {
        case e: Exception => log.error(e, "could not write backup file of pad content")
      } finally {
        pw.close()
      }

      self ! PoisonPill
    }
  }
}
