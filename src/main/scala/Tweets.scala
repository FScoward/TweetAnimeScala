import twitter4j.TwitterFactory

/**
 * Created by FScoward on 2014/11/24.
 */
object Tweets {
  private val twitter = TwitterFactory.getSingleton
  
  /**
   * ツイートを生成
   * */
  def makeTweet(content: Anime, affiliate: String) = {
    s"""【${content.station}】${content.title} ${content.next} - ${content.time} ～\n$affiliate"""
  }
  
  def tweet(text: String) = {
    twitter.updateStatus(text)
  }
}
