package controllers

import scalaz._
import scalaz.Scalaz._
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, FileInputStream, File}
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import scala.concurrent.{Future, future}
import james.JpegEncoder
import main.Extract.extract
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Akka
import play.api.libs.Files.TemporaryFile
import play.api.libs.iteratee.Enumerator
import play.api.libs.ws.WS
import play.api.http.Writeable.wBytes
import play.api.mvc._
import play.api.Play.current

object Application extends Controller {	
	def index = Action { Ok(views.html.index("Stegosaurus")) }
	
	def encrypt_and_download = handleUpload {request => 
			for {
				(fname, img) <- extractImage(request)
				msg <- extractMessage(request)
				key <- extractKey(request)
				download_future = for {
					stegged_img <- embed_msg(msg, key, img)
				} yield download_file(fname, stegged_img)
			} yield download_future
		}
	
	def encrypt_and_tweet = handleUpload {request => 
			for {
				(fname, img) <- extractImage(request)
				msg <- extractMessage(request)
				key <- extractKey(request)
				tweet_future = for {
					stegged_img <- embed_msg(msg, key, img)
					img_url <- upload_image(stegged_img)
				} yield Redirect(s"https://twitter.com/intent/tweet?url=$img_url")
			} yield tweet_future
		}
	
	def decrypt = handleUpload {request =>
			for {
				(fname, img) <- extractImageData(request)
				key <- extractKey(request)
				msg_future = for { 
						msg <- extract_msg(key, img)
					} yield Ok(<h1> message => {msg} </h1>).as("text/html")
			} yield msg_future
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
					println(s"${response.json}")
				 ((response.json \ "data") \ "link").toString.drop(1).dropRight(1) //trim enclosing quotes
			}
		}
	
	
	/**
	 * ASYNCH STEGANOGRAPHY HELPERS
	 */
	def extract_msg(key: String, img: java.io.File): Future[String] = future{
			val msg = new ByteArrayOutputStream 
			extract(new FileInputStream(img), img.length.toInt, msg, key)
			msg.toString 
		}
	
	def embed_msg(msg: String, key: String, img: BufferedImage) = Akka.future {
			val img_out = new ByteArrayOutputStream
			new JpegEncoder(img, 80, img_out, "test").Compress(new ByteArrayInputStream(msg getBytes), key)
			img_out.toByteArray
		}
	
	
	/**
	 * RESPONSE HELPERS
	 */
	
	//wrap a function FilePost => Valid[Future[Result]] in all the necessary boilerplate
	val handleUpload = (handle: (FilePost => Valid[Future[Result]])) =>
			Action(parse.multipartFormData) {handle map respond} //<- concatenating functions with map, I <heart> scalaz
			
	//Extract response from validation, apply Asynch boilerplate to result
	val respond = (result: Valid[Future[Result]]) => Async {result.fold(errorResponse, response => response)}
	
	//Wrap an error string in some basic html
	val errorResponse = (error: String) => future{ Ok(<h1>error => {error}</h1>).as("text/html") }

	
	def download_file(fname: String, bytes: Array[Byte]): Result = {
			val bais = new ByteArrayInputStream(bytes)
			val data: Enumerator[Array[Byte]] = Enumerator fromStream bais
			SimpleResult(
				header = ResponseHeader(200),
				body = data)
			.as("application/x-download")
			.withHeaders("Content-disposition" -> s"attachment; filename=$fname")
		}
	
	
	/**
	 * getting silly with type aliases 
	 */
	type FilePost = Request[MultipartFormData[TemporaryFile]] // because I'm not typing this more than once
	type Valid[T] = Validation[String, T] //Validation with a String message on error
	
	
	/**
	 * EXTRACTOR HELPERS
	 */
	//extract image
	def extractImageData(request: FilePost): Valid[(String, File)] = 
		for {
			img_data <- request.body.file("picture").toSuccess(e = "could not find picture")
		} yield (img_data.filename, img_data.ref.file)
	
	//extract and format image
	def extractImage(request: FilePost): Valid[(String, BufferedImage)] = 
		extractImageData(request).map{
			case (s, img) => (s, ImageIO.read(img))
		}
		
	
	//generate a function to extract a given field from a request containing multipart form data
	def extractField(key: String): FilePost => Valid[String] =  
		request => request.body.dataParts.get(key).map(_.head)	//get (Option of) field value for key
			.toSuccess(s"failed to find $key")	//Option => Validation with error message
			.ensure(s"value of $key cannot be empty string")(_ != "")	//validate Validation data
	
	val extractMessage: FilePost => Valid[String] = extractField("message")
	val extractKey: FilePost => Valid[String] = extractField("key")
	
}