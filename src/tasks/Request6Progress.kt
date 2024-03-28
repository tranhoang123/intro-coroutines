package tasks

import contributors.*
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

suspend fun loadContributorsProgress(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .body() ?: emptyList()

//    var currentD = listOf<User>()
//    coroutineScope {
//        repos.mapIndexed { index, repo ->
//            async(this.coroutineContext) {
//                service.getRepoContributors(req.org, repo.name)
//                    .also {
//                        logUsers(repo, it)
//                    }
//                    .bodyList()
//                    .also {
//                        currentD = currentD + it
//                        updateResults(currentD.aggregate(), index == repos.size - 1)
//                    }
//            }
//        }
//    }
    var allUsers = emptyList<User>()

    coroutineScope {
        for ( (index, repo) in repos.withIndex() ) {
            launch {
                val users = service.getRepoContributors(req.org, repo.name)
                    .also {
                        logUsers(repo, it)
                    }
                    .bodyList()

                allUsers = (allUsers + users).aggregate()
                updateResults(allUsers, index == repos.lastIndex)
            }
        }
    }
}

fun main(): Unit = runBlocking {
    val channel = Channel<String>()

    launch {
        channel.send("A1")
        channel.send("A2")

        log("A done")
    }

    launch {
        channel.send("B1")
        log("B done")
    }

    launch {
        repeat(2) {
            val x = channel.receive()
            log(x)
        }
        log("receive done")
    }
}