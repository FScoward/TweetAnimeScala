import scala.io.Source
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods._

/**
 * Created by FScoward on 2014/11/21.
 */
case class Anime(title: String,
                 url: String,
                 time: String,
                 station: String,
                 state: String,
                 next: String,
                 episode: String,
                 cable: String,
                 today: String,
                 week: String)

object Animemap {
  private val json = getAnimeMap
  
  private def getAnimeMap = {
    Source.fromURL("http://animemap.net/api/table/tokyo.json").mkString
  }
  
  /**
   * 一週間全部のアニメ放送情報取得
   * */
  def getAnimeList: List[Anime] = {
    implicit val formats = DefaultFormats
    val parsed = parse(json) \ "response" \ "item"
    parsed.extract[List[Anime]]
  }
  
  /**
   * 今日放送のアニメ取得
   * */
  def getTodayAnimeList(animeList: List[Anime]) = {
    animeList.filter(_.today == 1)
  }
  
  
}
