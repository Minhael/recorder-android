package me.minhael.design

/**
 * Interface to manage key value properties
 */
interface Props : Store {

    fun <T> commit(callable: (Store) -> T): T
}