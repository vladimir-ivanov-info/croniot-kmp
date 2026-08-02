package com.server.croniot.testsupport.fakes

import com.server.croniot.data.db.daos.AccountDao
import com.server.croniot.data.db.daos.VerifyPasswordResult
import com.server.croniot.data.db.entities.AccountEntity
import croniot.models.Account

/**
 * In-memory fake of [AccountDao], keyed by email like the real table's unique index.
 * Used to unit-test [com.server.croniot.data.repositories.AccountRepository]'s own orchestration
 * logic without a real Postgres — the DAO layer itself is already covered by
 * AccountJooqDaoImplIntegrationTest against a real database.
 */
class FakeAccountDao : AccountDao {

    private val byEmail = mutableMapOf<String, AccountEntity>()
    private val passwordsByEmail = mutableMapOf<String, String>()
    private var nextId = 1L

    var verifyPasswordResult: VerifyPasswordResult = VerifyPasswordResult.Invalid

    fun seed(entity: AccountEntity, password: String = "irrelevant") {
        byEmail[entity.email] = entity
        passwordsByEmail[entity.email] = password
    }

    override fun get(email: String): AccountEntity? = byEmail[email]

    override fun insert(account: Account, password: String): Long {
        val id = nextId++
        byEmail[account.email] = AccountEntity(
            id = id,
            uuid = account.uuid,
            nickname = account.nickname,
            email = account.email,
            password = password,
        )
        passwordsByEmail[account.email] = password
        return id
    }

    override fun isExistsAccountWithEmail(email: String): Boolean = byEmail.containsKey(email)

    override fun getAccountEagerSkipTasks(email: String): Account? = byEmail[email]?.let {
        Account(it.uuid, it.nickname, it.email, mutableListOf())
    }

    override fun verifyPassword(email: String, plaintext: String): VerifyPasswordResult = verifyPasswordResult

    override fun getAll(): List<Account> = byEmail.values.map { Account(it.uuid, it.nickname, it.email, mutableListOf()) }

    override fun isAccountExists(email: String): Boolean = byEmail.containsKey(email)

    override fun getAccountId(email: String): Long? = byEmail[email]?.id

    override fun getEmailById(accountId: Long): String? = byEmail.values.firstOrNull { it.id == accountId }?.email
}
