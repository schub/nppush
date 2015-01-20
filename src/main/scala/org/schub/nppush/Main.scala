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

case class GetPadContentResult(text: String)

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

  // schedule pad update
  val actor = system.actorOf(Props(new GetPadContentActor()))
  system.scheduler.schedule(5.seconds, config.updatePeriod.seconds, actor, GetPadContent)

  val bpActor = system.actorOf(Props(new GetBlogPostActor()))
  bpActor ! GetBlogPost(config.blogId, config.username, config.password, config.postId)

  // process user input
  for (ln <- io.Source.stdin.getLines) {
    if (ln.startsWith("x") || ln.startsWith("X")) {
      shutdown()
    } else {
      println(ln)
    }
  }

  def shutdown(): Unit = {
    Console.println("cleanup and shutdown")
    IO(Http).ask(Http.CloseAll)(1.second).await
    system.shutdown()
    System.exit(0)
  }
}

object Configuration {

  val conf = ConfigFactory.load()

  def username = conf.getString("username")

  def password = conf.getString("password")

  def blogId = conf.getString("blogId")

  def postId = conf.getString("postId")

  def wpUrl = conf.getString("wpUrl")

  def sourceUrl = conf.getString("sourceUrl")

  def updatePeriod = conf.getInt("updatePeriod")
}

class GetBlogPostActor extends Actor with ActorLogging {

  val config = Configuration

  def receive = {
    case data: GetBlogPost => {

      val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

      val response: Future[HttpResponse] = pipeline(Post(config.wpUrl + "/xmlrpc.php", data))

      response onComplete {

        case Success(result) => {
          Console.println("got blog post: " + result.entity.asString.length)
          self ! PoisonPill
        }

        case Failure(error) => {
          log.error(error, "Could not get blog post")
          self ! PoisonPill
        }
      }

    }
  }
}

class UpdateBlogPostActor extends Actor with ActorLogging {

  val config = Configuration

  def receive = {
    case PadContent(content) => {
      Console.println("pad content received. {} characters", content.length)

//      val data = UpdateBlogPost(config.blogId, config.username, config.password, config.postId, BlogPostContent(content))
//
//      val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
//      val response: Future[HttpResponse] = pipeline(Post(config.wpUrl + "/xmlrpc.php", data))
//
//      response onComplete {
//
//        case Success(result) => {}
//
//        case Failure(error) => {}
//
//      }

    }
  }
}

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
