package at.ac.fhcampus.master

import java.util

import at.ac.fhcampus.master.dtos.{CloudNativeSimpleRating, RegisterUser, SimpleRating}
import com.google.gson.Gson
import io.gatling.core.Predef._
import io.gatling.core.body.StringBody
import io.gatling.http.Predef._

import scala.util.Random

class CloudNativeSimulation extends Simulation {
  val httpConf = http.baseUrl("https://3mq79fnen6.execute-api.eu-central-1.amazonaws.com/prod")
    .doNotTrackHeader("1")

  val gson = new Gson()

  val CONSTANT_USERS_PER_SEC = 1000
  val TEST_DURATION = 10

  val feeder = Iterator.continually(Map(
    "RandomArticleUrl" -> randomArticleUrl,
  ))

  object RetrieveOAuthToken {
    val execute = exec(http("Login created user")
        .post("https://fh-campus-rating-app-auth.auth.eu-central-1.amazoncognito.com/oauth2/token?grant_type=client_credentials")
        .header("Authorization", "Basic NXExcG5oYjNpaGFmNm00ZTRoYTJsYXAzZjI6cWkydTQxbXBodmI2YnVnaG0wZXA3cWdhNW5zbzgxc2M4ZTNxODgzMnZxMWx2YXFodTdy")
        .header("Content-Type", "application/x-www-form-urlencoded")
        .check(status.is(200))
        .check(jsonPath("$..token_type").is("Bearer"))
        .check(jsonPath("$..access_token").saveAs("AccessToken"))
    )
  }

  object PostArticle {
    val execute = exec(http("Post Article")
      .post("/api/v1/articles/")
      .header("Content-Type", "application/json")
      .body(StringBody(session => gson.toJson(new dtos.CloudNativeArticle(null, session("RandomArticleUrl").as[String]))))
      .header("Authorization", "Bearer ${AccessToken}")
      .header("Content-Type", "application/json")
      .check(jsonPath("$..article_id").saveAs("newArticleId"))
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
      .body(StringBody(session => gson.toJson(randomRating(session("newArticleId").as[String]))))
      .header("Authorization", "Bearer ${AccessToken}")
      .header("Content-Type", "application/json")
      .check(status.is(201))
    )
  }

  object RateArticleWithIdOne {
    val execute = exec(http("rate-article-with-id-f83feab4-d54b-41df-baee-a00238b93d93")
      .post("/api/v1/ratings/")
      .header("Content-Type", "application/json")
      .body(StringBody(session => gson.toJson(randomRating("f83feab4-d54b-41df-baee-a00238b93d93"))))
      .header("Authorization", "Bearer ${AccessToken}")
      .header("Content-Type", "application/json")
      .check(status.is(201))
    )
  }

  object DisplayAccumulatedRatingForNewArticle {
    val execute = exec(http("accumulated-rating-article-${newArticleId}")
      .get("/api/v1/accumulated/${newArticleId}")
      .header("Authorization", "Bearer ${AccessToken}")
      .header("Content-Type", "application/json")
      .check(status.is(200))
    )
  }

  object DisplayAccumulatedRatingForArticleWithIdOne {
    val execute = exec(http("accumulated-rating-article-with-id-one")
      .get("/api/v1/accumulated/304b012e-29fd-4faa-b5f0-6a5dff95a989")
      .header("Authorization", "Bearer ${AccessToken}")
      .header("Content-Type", "application/json")
      .check(status.is(200))
    )
  }

  val scn = scenario("Show all articles, click through some of them")
    .feed(feeder)
    .exec(
      RetrieveOAuthToken.execute.pause(2),
    ).exitHereIfFailed
    .exec(http("show-one-article")
      .get("/api/v1/articles/f83feab4-d54b-41df-baee-a00238b93d93")
      .header("Authorization", "Bearer ${AccessToken}")
      .header("Content-Type", "application/json")
    ).pause(1)
    .exec(http("show-another-article")
      .get("/api/v1/articles/08704906-0475-4cce-b4be-621d73af0988")
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

  def randomRating(articleId: String) = new CloudNativeSimpleRating(
    Random.nextInt(10) + 1,
    articleId
  )

  def fixedRoles(): util.ArrayList[dtos.Role] = {
    val list = new util.ArrayList[dtos.Role]()
    list.add(new dtos.Role(3))
    list
  }
}
