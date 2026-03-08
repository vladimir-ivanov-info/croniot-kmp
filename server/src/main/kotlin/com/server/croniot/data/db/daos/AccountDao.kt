package com.server.croniot.data.db.daos

import com.server.croniot.data.db.entities.AccountEntity
import croniot.models.Account

interface AccountDao {

    fun get(email: String) : AccountEntity?

    fun insert(account: Account, password: String): Long

    fun isExistsAccountWithEmail(email: String): Boolean

    fun getAccountEagerSkipTasks(email: String, password: String): Account?

    fun getAll(): List<Account>


    fun isAccountExists(email: String) : Boolean

    fun getPassword(email: String) : String?

    fun getAccountId(email: String) : Long?
}
