package controllers

import scalaz._
import Scalaz._

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
	def index = Action { Ok(views.html.index("Stegosaurus")) }
	
	/**
	 * de-steg msg from img
	 */
	def extract_msg(key: String, img: java.io.File): Future[String] = future{
		val msg = new ByteArrayOutputStream 
		extract(new FileInputStream(img), img.length.toInt, msg, key)
		msg.toString 
		
	}

	
	/**
	 * Embed msg in image
	 */
	def embed(msg: String, key: String, img: BufferedImage) = Akka.future {
			val img_out = new ByteArrayOutputStream
			val encoder_widget = new JpegEncoder(img, 80, img_out, "test")
			encoder_widget Compress(new ByteArrayInputStream(msg getBytes), key)
			img_out
		}
	
	def encrypt_and_download = Action(parse.multipartFormData) { request =>
		val result = for {
			img_data <- request.body.file("picture").toSuccess(e = "fuck, no picture")
			msg <- request.body.dataParts.get("message")
						.map(_.head).filter(_ != "").toSuccess(e = "fuck, no msg")
			key <- request.body.dataParts.get("key")
						.map(_.head).filter(_ != "").toSuccess(e = "fuck, no key")
			img <- ImageIO.read(img_data.ref.file).success
			fname <- img_data.filename.success
			download_future <- (for {
				stegged_img <- embed(msg, key, img)
			} yield {
				val bais = new ByteArrayInputStream(stegged_img.toByteArray)
				val data: Enumerator[Array[Byte]] = Enumerator fromStream bais
				SimpleResult(
					header = ResponseHeader(200),
					body = data)
				.as("application/x-download")
				.withHeaders("Content-disposition" -> s"attachment; filename=$fname")
			}).success
		} yield download_future

		//TODO: non-awful error msg
		val onError = (error: String) => future{ Ok(<h1>error => {error}</h1>).as("text/html") } 

		Async {
			result.fold(
				errorResponse,
				response => response)
		}
	}
	
	def encrypt_and_tweet = Action(parse.multipartFormData) { request =>
		val result = for {
			img_data <- request.body.file("picture").toSuccess(e = "fuck, no picture")
			msg <- request.body.dataParts.get("message")
						.map(_.head).filter(_ != "").toSuccess(e = "fuck, no msg")
			key <- request.body.dataParts.get("key")
						.map(_.head).filter(_ != "").toSuccess(e = "fuck, no key")
			img <- ImageIO.read(img_data.ref.file).success
			fname <- img_data.filename.success
			tweet_future <- (for {
				stegged_img <- embed(msg, key, img)
				img_url <- upload_image(stegged_img.toByteArray)
			} yield Redirect(s"https://twitter.com/intent/tweet?url=$img_url")).success
		} yield tweet_future

		//TODO: non-awful error msg
		val onError = (error: String) => future{ Ok(<h1>error => {error}</h1>).as("text/html") } 

		Async {
			result.fold(
				errorResponse,
				response => response)
		}
	}
	

	def decrypt = Action(parse.multipartFormData) { request =>
		val result = for {
			img_data <- request.body.file("picture").toSuccess(e = "fuck, no picture")
			key <- request.body.dataParts.get("key")
						.map(_.head).filter(_ != "").toSuccess(e = "fuck, no key")
			img <- img_data.ref.file.success
			msg_future <- (for { 
					msg <- extract_msg(key, img)
				} yield Ok(<h1>message => {msg}</h1>).as("text/html")).success
		} yield msg_future
		
		Async {
			result.fold(
				errorResponse,
				response => response)		
		}
	}

	/**
	 * upload image to imgur anonymously, return URL of hosted image
	 * TODO: type coercion of json response
	 * URL-validating return type
	 */
	def upload_image(file: Array[Byte]): Future[String] = {
			WS.url("https://api.imgur.com/3/image")
			.withHeaders(("Authorization", "Client-ID dcb5b8c68dc45f4"))
			.post(file).map{ response =>
			 ((response.json \ "data") \ "link").toString.drop(1).dropRight(1) //trim enclosing quotes
	}
	}
	
	val errorResponse = (error: String) => future{ Ok(<h1>error => {error}</h1>).as("text/html") }
}
	
	