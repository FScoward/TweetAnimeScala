/**
 * Created by FScoward on 2014/11/21.
 */

object TweetAnime {

  def main(args: Array[String]): Unit = {
    
    val today = Animemap.getAnimeList.filter(_.today == "1")
    
    val affiliates = today.map(anime => {
      Thread.sleep(5000)
      Tweets.makeTweet(anime, Amazon.itemSearch(anime.title))
    })
    
    affiliates.map(Tweets.tweet)
  }
}
