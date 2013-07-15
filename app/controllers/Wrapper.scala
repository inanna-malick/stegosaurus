package controllers

import java.awt.Toolkit
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import james.JpegEncoder
import main.Extract.extract
import twitter4j.StallWarning
import twitter4j.Status
import twitter4j.StatusDeletionNotice
import twitter4j.StatusListener
import twitter4j.TwitterStreamFactory

object Util {
  val config = new twitter4j.conf.ConfigurationBuilder()
    .setOAuthConsumerKey("SSV5kHAwq7cYPhF0uf6XxA")
    .setOAuthConsumerSecret("7NUTY1qR57j1gg7wRbiCQ2HPl9ApYaiYuBnRZTc3sQ")
    .setOAuthAccessToken("287184001-F6kjAgAq6Lmpeg5mVn7s66pHRnhdjmrrfAyxNl0w")
    .setOAuthAccessTokenSecret("EUwFC7nPU2vv9vbr4iN1bX5Ig83jFG9soLNDlOLap0")
    .build

   //listen for all tweets on a specific channel - how about #mypublickey? yes, grab all those and decrypt message
  def simpleStatusListener = new StatusListener() {
    def onStatus(status: Status) { println(status.getText) }
    def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) {}
    def onTrackLimitationNotice(numberOfLimitedStatuses: Int) {}
    def onException(ex: Exception) { ex.printStackTrace }
    def onScrubGeo(arg0: Long, arg1: Long) {}
    def onStallWarning(warning: StallWarning) {}
  }
}

object Wrapper {

  def main(args: Array[String]): Unit = {

    val baos = embed("ohshitson", "http://i.imgur.com/XiTwYPM.jpg", "foo")

    val f = new File("out.jpg")
    val dataOut = new FileOutputStream(f)
    baos.writeTo(dataOut)
    dataOut.close

    println(retrieve(new File("out.jpg"), "foo"))

    val twitterStream = (new TwitterStreamFactory(Util.config)).getInstance
    twitterStream.addListener(Util.simpleStatusListener)
    twitterStream.sample
    Thread.sleep(2000)
    twitterStream.cleanUp
    twitterStream.shutdown

  }
  
  
  /*
   * TODO: 
   * withDecryption(img, key)( (msg: String) => Unit )
   * withEncrption(img, msg, key)( (y: Image) => Unit )
   
   * 
   * how much time is spent BUILDING JpegEncoder object vs Compress-ing msg?
   * 
   * */
  

  //retrieve msg from src f(ile) and save to result
  def retrieve(f: File, key: String): String = {
    val fis = new FileInputStream(f)
    val baos = new ByteArrayOutputStream()
    extract(fis, f.length.toInt, baos, key)

    baos.toString()
  }

  def embed(msg: String, img_url: String, key: String): ByteArrayOutputStream = {
    val img = Toolkit.getDefaultToolkit().getImage(new URL(img_url))
    val baos = new ByteArrayOutputStream()
    val b = new JpegEncoder(img, 80, baos, "test")
    b.Compress(new ByteArrayInputStream(msg.getBytes()), key)

    baos
  }

}