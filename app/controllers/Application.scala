package controllers

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream

import scala.concurrent.Future
import scala.concurrent.future

import james.JpegEncoder
import javax.imageio.ImageIO
import main.Extract.extract
import play.api.http.Writeable.wBytes
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WS
import play.api.mvc.Action
import play.api.mvc.Controller

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def encrypt = Action(parse.multipartFormData) { request =>
    request.body.file("picture").map { picture =>
      val img = ImageIO.read(picture.ref.file)
      val msg = request.body.dataParts("message").head
      val key = "barfoo"

      Async {

        println(msg)

        val baos = new ByteArrayOutputStream()

        val b = time("build with image") {
          new JpegEncoder(img, 50, baos, "test")
        }
        time("message and key") {
          b.Compress(new ByteArrayInputStream(msg.getBytes()), key)
        }

        //upload_image(baos.toByteArray()) <-- actually upload
        upload_image_stub map { json =>
          val img_url = json
          println(img_url)
          Redirect(tweet_url(img_url))
        }
      }

      /*
      val bais = new ByteArrayInputStream(baos.toByteArray())
      val fileContent: Enumerator[Array[Byte]] = Enumerator.fromStream(bais)

      val fname = picture.filename

		
      
      SimpleResult(
        header = ResponseHeader(200),
        body = fileContent).as("application/x-download").withHeaders("Content-disposition" -> s"attachment; filename=$fname")
      
      */

      //Ok("foo")

    }.getOrElse {
      Redirect(routes.Application.index).flashing(
        "error" -> "Missing file")
    }
  }

  def decrypt = Action(parse.multipartFormData) { request =>
    request.body.file("picture").map { picture =>
      val key = "barfoo"

      val f = picture.ref.file
      val fis = new FileInputStream(f)
      val baos = new ByteArrayOutputStream()
      extract(fis, f.length.toInt, baos, key)

      Ok(baos.toString)

    }.getOrElse {
      Redirect(routes.Application.index).flashing(
        "error" -> "Missing file")
    }
  }

  //upload image to imgur, return json
  //TODO: better auth, oauth(?)
  def upload_image(file: Array[Byte]): Future[String] = {
    WS.url("https://api.imgur.com/3/image")
      .withHeaders(("Authorization", "Client-ID dcb5b8c68dc45f4"))
      .post(file).map({ response => println(response.json); ((response.json \ "data") \ "link").toString().replace("\"", "") })
  }

  val upload_image_stub = future { "http://i.imgur.com/P7JPThX.jpg" }

  def tweet_url(img_url: String): String = s"https://twitter.com/intent/tweet?url=$img_url"

  def time[R](name: String)(block: => R): R = {
    println(s"begin timing $name")
    val t0 = System.nanoTime()
    val result = block // call-by-name
    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0) + "ns")
    result
  }

}
  
  
  
  
  
  
  
  
  
  
