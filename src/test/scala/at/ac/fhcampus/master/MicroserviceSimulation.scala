package at.ac.fhcampus.master

import java.util

import at.ac.fhcampus.master.dtos.{Rating, RegisterUser, SimpleRating, User}
import com.google.gson.Gson
import io.gatling.core.Predef._
import io.gatling.core.body.StringBody
import io.gatling.http.Predef._

import scala.util.Random

class MicroserviceSimulation extends Simulation {
  val httpConf = http.baseUrl("http://18.185.144.9")
    .doNotTrackHeader("1")

  val gson = new Gson()

  val CONSTANT_USERS_PER_SEC = 100
  val TEST_DURATION = 60

  val feeder = Iterator.continually(Map(
    "RandomArticleUrl" -> randomArticleUrl,
    "RandomUserName" -> randomUserName
  ))

  object CreateNewUser {
    val execute = exec(
      http("Create A New User")
        .post("/api/v1/users/register")
        .header("Content-Type", "application/json")
        .body(StringBody(session => gson.toJson(new RegisterUser(
          true, true, true, true,
          session("RandomUserName").as[String],
          "$2a$08$BO53tsxv2nE3qhJ2o/4ehet9yM6.KwTXgVR17yvhjooXXahTDCBFu",
          fixedRoles()
        )
        )))
        .check(status.is(200))
        .check(jsonPath("$..id").saveAs("userId"))
        .check(jsonPath("$..username").saveAs("username"))
    )
  }

  object LoginCreatedUser {
    val execute = exec(http("Login created user")
        .post("/api/v1/users/oauth/token?username=${RandomUserName}&password=password&grant_type=password")
        .header("Content-Type", "application/x-www-form-urlencoded")
        .check(status.is(200))
        .check(jsonPath("$..token_type").is("bearer"))
        .check(jsonPath("$..access_token").saveAs("AccessToken"))
    )
  }

  object PostArticle {
    val execute = exec(http("Post Article")
      .post("/api/v1/articles/")
      .header("Content-Type", "application/json")
      .body(StringBody(session => gson.toJson(new dtos.Article(null, session("RandomArticleUrl").as[String]))))
      .header("Authorization", "Bearer ${AccessToken}")
      .header("Content-Type", "application/json")
      .check(jsonPath("$..id").saveAs("newArticleId"))
    )
  }

  object ReadPostedArticle {
    val execute = exec(http("read-article-${newArticleId}")
      .get("/api/v1/articles/${newArticleId}")
      .header("Authorization", "Bearer ${AccessToken}")
      .header("Content-Type", "application/json")
    )
  }

  object RatePostedArticle {
    val execute = exec(http("rate-article-${newArticleId}")
      .post("/api/v1/ratings/")
      .header("Content-Type", "application/json")
      .body(StringBody(session => gson.toJson(randomRating(session("userId").as[Long], session("newArticleId").as[Long]))))
      .header("Authorization", "Bearer ${AccessToken}")
      .header("Content-Type", "application/json")
      .check(status.is(200))
    )
  }

  object RateArticleWithIdOne {
    val execute = exec(http("rate-article-with-id-1")
      .post("/api/v1/ratings/")
      .header("Content-Type", "application/json")
      .body(StringBody(session => gson.toJson(randomRating(session("userId").as[Long], 1L))))
      .header("Authorization", "Bearer ${AccessToken}")
      .header("Content-Type", "application/json")
      .check(status.is(200))
    )
  }

  object DisplayAccumulatedRatingForNewArticle {
    val execute = exec(http("accumulated-rating-article-${newArticleId}")
      .get("/api/v1/accumulated-ratings/by-article/${newArticleId}")
      .header("Authorization", "Bearer ${AccessToken}")
      .header("Content-Type", "application/json")
      .check(status.is(200))
    )
  }

  object DisplayAccumulatedRatingForArticleWithIdOne {
    val execute = exec(http("accumulated-rating-article-with-id-one")
      .get("/api/v1/accumulated-ratings/by-article/1")
      .header("Authorization", "Bearer ${AccessToken}")
      .header("Content-Type", "application/json")
      .check(status.is(200))
    )
  }

  val scn = scenario("Show all articles, click through some of them")
    .feed(feeder)
    .exec(
      CreateNewUser.execute,
      LoginCreatedUser.execute
    ).exitHereIfFailed
    .exec(http("show-one-article")
      .get("/api/v1/articles/1")
      .header("Authorization", "Bearer ${AccessToken}")
      .header("Content-Type", "application/json")
    ).pause(1)
    .exec(http("show-another-article")
      .get("/api/v1/articles/2")
      .header("Authorization", "Bearer ${AccessToken}")
      .header("Content-Type", "application/json")
    ).pause(1)
    .exec(
      PostArticle.execute,
      ReadPostedArticle.execute,
      RatePostedArticle.execute,
      DisplayAccumulatedRatingForNewArticle.execute,
      RateArticleWithIdOne.execute,
      DisplayAccumulatedRatingForArticleWithIdOne.execute
    )

  setUp(
    scn.inject(
      constantUsersPerSec(CONSTANT_USERS_PER_SEC) during(TEST_DURATION)
    )
  ).protocols(httpConf)

  def randomUserName: String = Random.alphanumeric.take(16).mkString

  def randomArticleUrl: String = "https://" + Random.alphanumeric.take(8).mkString + ".com/" + Random.alphanumeric.take(8).mkString + "/" + Random.alphanumeric.take(8).mkString + "/" + Random.alphanumeric.take(8).mkString + "/" + Random.alphanumeric.take(8).mkString + "/" + Random.alphanumeric.take(8).mkString + "/" + Random.alphanumeric.take(8).mkString + "/" + Random.alphanumeric.take(8).mkString + "/" + Random.alphanumeric.take(8).mkString + "/"

  def randomRating(userId: Long, articleId: Long) = new SimpleRating(
    Random.nextInt(10) + 1,
    Random.nextInt(10) + 1,
    userId,
    articleId
  )

  def fixedRoles(): util.ArrayList[dtos.Role] = {
    val list = new util.ArrayList[dtos.Role]()
    list.add(new dtos.Role(3))
    list
  }
}
