package io.defolters

import auth.JwtService
import auth.hash
import io.defolters.auth.MySession
import io.defolters.optimization.TaskOptimizer
import io.defolters.routes.*
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.jwt
import io.ktor.features.*
import io.ktor.gson.gson
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.routing.routing
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.util.KtorExperimentalAPI
import repository.DatabaseFactory
import repository.Repository
import routes.todosRoute
import routes.usersRoute
import kotlin.collections.set

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    install(Locations) {
    }

    install(CallLogging)
    install(DefaultHeaders)
    install(ConditionalHeaders)
    install(PartialContent)
    install(AutoHeadResponse)
    install(Sessions) {
        cookie<MySession>("MY_SESSION") {
            cookie.extensions["SameSite"] = "lax"
        }
    }

    install(CORS)
    {
        method(HttpMethod.Get)
        method(HttpMethod.Put)
        method(HttpMethod.Post)
        method(HttpMethod.Patch)
        method(HttpMethod.Delete)
        method(HttpMethod.Options)
        header(HttpHeaders.ContentType)
        header(HttpHeaders.XTotalCount)
        header(HttpHeaders.ContentRange)
        header(HttpHeaders.Authorization)
        header(HttpHeaders.ContentLength)
        header(HttpHeaders.AccessControlAllowOrigin)
        header(HttpHeaders.AccessControlAllowHeaders)
        header(HttpHeaders.AccessControlAllowCredentials)
        allowCredentials = true
        anyHost()
    }

    DatabaseFactory.init()
    val db = Repository()
    val jwtService = JwtService()
    val hashFunction = { s: String -> hash(s) }

    TaskOptimizer.optimizeTest()

    install(Authentication) {
        jwt("jwt") {
            verifier(jwtService.verifier)
            realm = "Todo Server"
            validate {
                val payload = it.payload
                val claim = payload.getClaim("id")
                val claimString = claim.asInt()
                val user = db.findUser(claimString)
                user
            }
        }
    }

    install(ContentNegotiation) {
        gson {
        }
    }

    routing {
        usersRoute(db, jwtService, hashFunction)
        todosRoute(db)
        itemTemplatesRoute(db)
        taskTemplatesRoute(db)
        ordersRoute(db)
        itemsRoute(db)
        tasksRoute(db)
        workerTypesRoute(db)
    }
}

const val API_VERSION = "/v1"
