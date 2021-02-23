package me.minhael.design.koin

import me.minhael.design.job.Job

interface JobQueue {
    fun append(name: String, job: Job): String
    fun remove(name: String)

    fun peek(): List<Pair<String, Job>>
    fun removeIds(vararg uuid: String)
}