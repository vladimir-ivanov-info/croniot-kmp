package com.server.croniot.data.db.daos

import croniot.models.*
import jakarta.persistence.criteria.JoinType
import org.hibernate.SessionFactory
import javax.inject.Inject

class AccountDaoImpl @Inject constructor(
    private val sessionFactory: SessionFactory,
) : AccountDao {

    override fun insert(account: Account): Long {
        val session = sessionFactory.openSession()
        val transaction = session.beginTransaction()
        val accountId: Long
        try {
            session.persist(account)
            session.flush()
            accountId = account.id
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e // Rethrow the exception after rollback
        } finally {
            // session.close() // Close the session to release resources
        }
        return accountId
    }

    override fun isExistsAccountWithEmail(email: String): Boolean {
        val session = sessionFactory.openSession()

        session.use { sess ->
            val cb = sess.criteriaBuilder
            val cr = cb.createQuery(Long::class.java)
            val root = cr.from(Account::class.java)

            val emailPredicate = cb.equal(root.get<String>("email"), email)

            cr.select(cb.count(root)).where(emailPredicate)

            val query = sess.createQuery(cr)
            val accountExists = query.singleResult

            return accountExists != null && accountExists > 0
        }
    }

    override fun getAccountEagerSkipTasks(email: String, password: String): Account? {
        val session = sessionFactory.openSession()
        session.use { sess ->

            val cb = sess.criteriaBuilder
            val cr = cb.createQuery(Account::class.java)
            val root = cr.from(Account::class.java)

            val devicesFetch = root.fetch<Account, Device>("devices", JoinType.LEFT)
            devicesFetch.fetch<Device, SensorType>("sensorTypes", JoinType.LEFT)
            devicesFetch.fetch<Device, TaskType>("taskTypes", JoinType.LEFT)

            val emailPredicate = cb.equal(root.get<String>("email"), email)
            val passwordPredicate = cb.equal(root.get<String>("password"), password)

            cr.select(root).where(cb.and(emailPredicate, passwordPredicate))

            val query = sess.createQuery(cr).uniqueResultOptional()
            return query.orElse(null)
        }
    }

    override fun getAll(): List<Account> {
        val session = sessionFactory.openSession()

        val cb = session.criteriaBuilder

        val cr = cb.createQuery(Account::class.java)
        val root = cr.from(Account::class.java)
        cr.select(root)

        val query = session.createQuery(cr)
        val list = query.resultList
        return list
    }
}
