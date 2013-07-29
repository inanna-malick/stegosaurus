package controllers

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

import scala.concurrent.Future
import scala.concurrent.future

import james.JpegEncoder

import play.api.libs.iteratee.Enumerator 

import main.Extract.extract
import play.api.http.Writeable.wBytes
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Akka 
import play.api.libs.ws.WS
import play.api.mvc._

import play.api.Play.current

object Application extends Controller {

	val key = "barfoo"
	
	def index = Action { Ok(views.html.index("Stegosaurus")) }
	
	
	def embed(msg: String, img: BufferedImage) = Akka.future {
			val img_out = new ByteArrayOutputStream
			val encoder_widget = new JpegEncoder(img, 80, img_out, "test")
			encoder_widget Compress(new ByteArrayInputStream(msg getBytes), key)
			img_out
		}
	
	def encrypt_and_download = Action(parse.multipartFormData) { request =>
		println(request.body)
		request.body.file("picture").map { picture =>
			val img: BufferedImage = ImageIO.read(picture.ref.file)
			val fname = picture.filename

			val msg = request.body.dataParts("message").head
			
			
			val stegged_img = embed(msg, img)
			
			Async {
				stegged_img map { img_out =>
					val bais = new ByteArrayInputStream(img_out.toByteArray)
					val fileContent: Enumerator[Array[Byte]] = Enumerator fromStream bais

					SimpleResult(
						header = ResponseHeader(200),
						body = fileContent).as("application/x-download").withHeaders("Content-disposition" -> s"attachment; filename=$fname")
				}
			}
		}.getOrElse {
			Redirect(routes.Application.index).flashing("error" -> "Missing file")
		}
	}
	
	
	def encrypt_and_tweet = Action(parse.multipartFormData) { request =>
		request.body.file("picture").map { picture =>
			val img: BufferedImage = ImageIO.read(picture.ref.file)
			val fname = picture.filename

			val msg = request.body.dataParts("message").head
			
			
			val stegged_img = embed(msg, img)
			
			Async {
				stegged_img flatMap { img_out =>
					upload_image(img_out.toByteArray) map { img_url =>
						Redirect(s"https://twitter.com/intent/tweet?url=$img_url")
					}
				}
			}
			
			/* TODO: switch to choose between uploading and tweeting or just downloading img
			val bais = new ByteArrayInputStream img_out toByteArray
			val fileContent: Enumerator[Array[Byte]] = Enumerator fromStream bais

			SimpleResult(
				header = ResponseHeader(200),
				body = fileContent).as("application/x-download").withHeaders("Content-disposition" -> s"attachment; filename=$fname")
			*/

		}.getOrElse {
			Redirect(routes.Application.index).flashing("error" -> "Missing file")
		}
	}

	def decrypt = Action(parse.multipartFormData) { request =>
		request.body.file("picture").map { picture =>
			val img_in = picture.ref.file
			val str_out = new ByteArrayOutputStream
			extract(new FileInputStream(img_in), img_in.length.toInt, str_out, key)

			Ok(str_out toString)
		}.getOrElse {
			Redirect(routes.Application.index).flashing("error" -> "Missing file")}
	}

	//upload image to imgur, return json
	def upload_image(file: Array[Byte]): Future[String] = {
		WS.url("https://api.imgur.com/3/image")
	.withHeaders(("Authorization", "Client-ID dcb5b8c68dc45f4"))
	.post(file).map{ response =>
	 ((response.json \ "data") \ "link").toString.drop(1).dropRight(1) //trim enclosing quotes
	}
	}

	//mock imgur link to avoid hammering their upload api
	val upload_image_stub = future { "http://i.imgur.com/8DbaCGd.jpg" }

}
	
	