package tasks

import contributors.*
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.delayFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import okhttp3.internal.threadName
import java.lang.ArithmeticException
import java.lang.Exception
import kotlin.random.Random
import kotlin.system.measureTimeMillis

suspend fun loadContributorsChannels(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    coroutineScope {
        val repos = service.getOrgRepos(req.org)
            .also {
                logRepos(req, it)
            }
            .bodyList()

        val channel = Channel<List<User>>()
        for ((index, repo) in repos.withIndex()) {
            launch {
                val users = service.getRepoContributors(req.org, repo.name)
                    .also {
                        logUsers(repo, it)
                    }
                    .bodyList()

                channel.send(users)
            }
        }

        var allUsers = emptyList<User>()
        repeat(repos.size) {
            val users = channel.receive()
            allUsers = (allUsers + users).aggregate()
            updateResults(allUsers, it == repos.lastIndex)
        }
    }
}

fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

val threadLocal = ThreadLocal<String?>() //
@OptIn(DelicateCoroutinesApi::class)
fun main() {
    runBlocking {
//        numbers().take(2).collect {
//            println(it)
//        }
        val time = measureTimeMillis {
            simple()
                .conflate()
                .collect {
                delay(1000)
                log("Collected $it")
            }
        }

        println("Collected in $time")
    }
}


fun simple(): Flow<Int> = flow {
    log("Started simple flow")
    for (i in 1..30) {
        delay(100)
        log("Emitting $i")
        emit(i)
    }
}

fun simple2(): Flow<Int> = flow {
    for(i in 1 .. 3) {
        delay(100)
        println("Emitting $i")
        emit(i)
    }
}

suspend fun performAction(request: Int): String {
    delay(1000)
    return "response $request"
}

fun numbers(): Flow<Int> = flow {
    try {
        emit(1)
        emit(2)
        println("this line will not execute")
        emit(3)
    } finally {
        println("Finally in numbers")
    }
}