package at.ac.fhcampus.master

import java.util

import at.ac.fhcampus.master.dtos.{Rating, RegisterUser, User}
import com.google.gson.Gson
import io.gatling.core.Predef._
import io.gatling.core.body.StringBody
import io.gatling.http.Predef._

import scala.util.Random

class MicroserviceSimulation extends Simulation {
  val httpConf = http.baseUrl("http://3.121.159.210")
    .doNotTrackHeader("1")

  val gson = new Gson()

  val feeder = Iterator.continually(Map(
    "RandomArticleUrl" -> randomArticleUrl,
    "RandomUserName" -> randomUserName
  ))

  object CreateNewUser {
    val execute = exec(
      http("Create A New User")
        .post("/api/v1/users/registration/")
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

  object PostArticle {
    val execute = exec(http("Post Article")
      .post("/api/v1/articles/")
      .header("Content-Type", "application/json")
      .body(StringBody(session => gson.toJson(new dtos.Article(null, session("RandomArticleUrl").as[String]))))
      .basicAuth("${username}", "password")
      .check(jsonPath("$..id").saveAs("newArticleId"))
    )
  }

  object ReadPostedArticle {
    val execute = exec(http("read-article-${newArticleId}")
      .get("/api/v1/articles/${newArticleId}")
      .basicAuth("${username}", "password")
    )
  }

  object RatePostedArticle {
    val execute = exec(http("rate-article-${newArticleId}")
      .post("/api/v1/ratings/")
      .header("Content-Type", "application/json")
      .body(StringBody(session => gson.toJson(randomRating(session("userId").as[Long], session("newArticleId").as[Long]))))
      .basicAuth("${username}", "password")
      .check(status.is(200))
    )
  }

  object RateArticleWithIdOne {
    val execute = exec(http("rate-article-with-id-1")
      .post("/api/v1/ratings/")
      .header("Content-Type", "application/json")
      .body(StringBody(session => gson.toJson(randomRating(session("userId").as[Long], 1L))))
      .basicAuth("${username}", "password")
      .check(status.is(200))
    )
  }

  object DisplayAccumulatedRatingForNewArticle {
    val execute = exec(http("accumulated-rating-article-${newArticleId}")
      .get("/api/v1/ratings/accumulated/${newArticleId}")
      .basicAuth("${username}", "password")
      .check(status.is(200))
    )
  }

  object DisplayAccumulatedRatingForArticleWithIdOne {
    val execute = exec(http("accumulated-rating-article-with-id-one")
      .get("/api/v1/ratings/accumulated/1")
      .basicAuth("${username}", "password")
      .check(status.is(200))
    )
  }

  val scn = scenario("Show all articles, click through some of them")
    .exec(http("show-one-article")
      .get("/api/v1/articles/1")
      .header("Authorization", "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsicmVzb3VyY2Utc2VydmVyLXJlc3QtYXBpIl0sInVzZXJfbmFtZSI6InVzZXIiLCJzY29wZSI6WyJyZWFkIl0sImV4cCI6MTU5NTk3MzIzMSwiYXV0aG9yaXRpZXMiOlsiVVNFUiJdLCJqdGkiOiJhNzY5NTI1MC1jYTdmLTRjYTEtYWUyMy03ZjFmZDdhYzhhY2IiLCJjbGllbnRfaWQiOiJvYXV0aDItY2xpZW50In0.kBv3E5C5hql9BrCo6-E_TOJ1lwuXHm-xJWKH1UQdsg-mZM1Z2tDsSg2_DtJP5ccZYtoUZizS2zmj3Mu_kPUfXbMqat4ZAw3xjQblRdP1qZSZMZRgS6yrvhHSirP_SeypfirYm4kLJ44arvjjmsPCKjTf2957XhOgCBJ6T-tVAFOcIwOqPO8KUyIwPX4oJA0QAoXkgB4G-GQRmJOmj9lhgKVnErIBUWhc0tTAm4cqwp4ABwJ7_3LP9EfamoSqHzpOdV5w5ejrCKucvPlMlWSBgAq23iwhA53SUy9bhJvixV2nMwjSGira9FwS-2-8rcrFcM-8VHSWMFnsA1trEVN2mQ")
      .header("Content-Type", "application/json")
    ).pause(1)

  setUp(
    scn.inject(
      atOnceUsers(3250)
//      constantUsersPerSec(500) during(60)
    )
  ).protocols(httpConf)

  def randomUserName: String = Random.alphanumeric.take(16).mkString

  def randomArticleUrl: String = "https://" + Random.alphanumeric.take(8).mkString + ".com/" + Random.alphanumeric.take(8).mkString + "/" + Random.alphanumeric.take(8).mkString + "/" + Random.alphanumeric.take(8).mkString + "/" + Random.alphanumeric.take(8).mkString + "/" + Random.alphanumeric.take(8).mkString + "/" + Random.alphanumeric.take(8).mkString + "/" + Random.alphanumeric.take(8).mkString + "/" + Random.alphanumeric.take(8).mkString + "/"

  def randomRating(userId: Long, articleId: Long) = new Rating(
    Random.nextInt(10) + 1,
    Random.nextInt(10) + 1,
    new User(userId),
    new dtos.Article(articleId, null)
  )

  def fixedRoles(): util.ArrayList[dtos.Role] = {
    val list = new util.ArrayList[dtos.Role]()
    list.add(new dtos.Role(3))
    list
  }
}
