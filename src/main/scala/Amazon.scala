import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.{TimeZone, Calendar}
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import env.Environment
import org.apache.commons.codec.binary.Base64
import scala.collection.SortedMap
import scala.collection.immutable.{Seq, TreeMap}
import scala.io.Source
import scala.collection.JavaConversions._
import scala.xml.parsing.ConstructingParser
import scalaj.http.{HttpOptions, Http}
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods._
/**
 * Created by FScoward on 2014/11/21.
 */
object Amazon {
  private val accessKey = Environment.accessKey
  private val secretAccessKey = Environment.secretAccessKey
  private val HMAC_SHA_256 = "HmacSHA256"
  private val UTF8_CHARSET = "UTF-8"
  private val REQUEST_URI = "/onca/xml"
  private val REQUEST_METHOD = "GET"
  private val endpoint = "ecs.amazonaws.jp"

  // 初期化
  val mac = {
    val secretAccessKeyBytes = secretAccessKey.getBytes(UTF8_CHARSET)
    val secretKeySpec = new SecretKeySpec(secretAccessKeyBytes, HMAC_SHA_256)
    val mac = Mac.getInstance(HMAC_SHA_256)
    mac.init(secretKeySpec)
    mac
  }
  
  /**
   * 検索し、先頭の結果を返却
   * */ 
  def itemSearch(keyword: String): String = {
    val params = Map("Operation" -> "ItemSearch", "SearchIndex" -> "All", "AssociateTag" -> "coward0d-22", "Keywords" -> keyword)
    val url = sign(params)
    val xml = ConstructingParser.fromSource(Source.fromURL(url, UTF8_CHARSET), false)
    val items = xml.document().docElem \ "Items" \ "Item"
    urlShortener(items \ "DetailPageURL" text).values.toString
  }

  /**
   * シグネチャー付きのURLを生成
   * */
  private def sign(params: Map[String, String]) = {
    val sortedParams = TreeMap.empty[String, String] ++ params ++ Map("AWSAccessKeyId" -> accessKey, "Timestamp" -> timestamp)
    val canonicalQS = canonicalize(sortedParams)
    val toSign = s"""$REQUEST_METHOD\n$endpoint\n$REQUEST_URI\n$canonicalQS"""
    val sig = percentEncodeRfc3986(hmac(toSign))
    s"""http://$endpoint$REQUEST_URI?$canonicalQS&Signature=$sig"""
  }

  /**
   * hmac 生成
   * */
  private def hmac(stringToSign: String) = {
    try {
      val data = stringToSign.getBytes(UTF8_CHARSET)
      val rawMac = mac.doFinal(data)
      new String(new Base64().encode(rawMac))
    } catch {
      case e: UnsupportedEncodingException => throw new RuntimeException(UTF8_CHARSET + "is unsupported!", e)
    }
  }

  /**
   * タイムスタンプ生成
   * */
  private def timestamp = {
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'") {
      this.setTimeZone(TimeZone.getTimeZone("GMT"))
    }.format(Calendar.getInstance.getTime)
  }

  /**
   * 整形
   * */
  private def canonicalize(sortedParamMap: SortedMap[String, String]) = {
    val builder = new StringBuilder
    sortedParamMap.entrySet().iterator().foreach{ kvpair => 
      builder.append(percentEncodeRfc3986(kvpair.getKey))
      builder.append("=")
      builder.append(percentEncodeRfc3986(kvpair.getValue))
      builder.append("&")
    }
    builder.deleteCharAt(builder.lastIndexOf("&"))
    builder.toString
  }

  // + * %7E を変換
  private def percentEncodeRfc3986(string: String): String = {
    try {
      URLEncoder.encode(string, UTF8_CHARSET)
        .replace("+", "%20")
        .replace("*", "%2A")
        .replace("%7E", "~")
    } catch {
      case e: UnsupportedEncodingException => string
    }
  }
  
  /**
   * google の URL Shortener API を使用して短縮URLに変換
   * */
  private def urlShortener(url: String) = {
    val json = s"""{\"longUrl\": \"$url\"}"""
    val result = Http.postData(s"""https://www.googleapis.com/urlshortener/v1/url?key=${Environment.googleApiKey}""", json)
      .header("content-type", "application/json")
      .option(HttpOptions.connTimeout(10000))
      .option(HttpOptions.readTimeout(50000))
      .charset(UTF8_CHARSET)
    parse(result.asString) \ "id"
  }
}
