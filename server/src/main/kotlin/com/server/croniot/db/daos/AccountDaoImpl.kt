package com.croniot.server.db.daos

import croniot.models.*
import com.croniot.server.db.controllers.ControllerDb
import jakarta.persistence.criteria.JoinType
import java.sql.Connection

class AccountDaoImpl: AccountDao {

    override fun insert(account: Account) : Long {

        val session = ControllerDb.sessionFactory.openSession()
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
            //session.close() // Close the session to release resources
        }

        return accountId
    }

    override fun isExistsAccountWithEmail(email: String) : Boolean {

        val session = ControllerDb.sessionFactory.openSession()

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

    /*override fun getAccount(email: String, password: String): Account? {
        val session = ControllerDb.sessionFactory.openSession()
        session.use { sess ->
            val cb = sess.criteriaBuilder
            val cr = cb.createQuery(Account::class.java)
            val root = cr.from(Account::class.java)

            val emailPredicate = cb.equal(root.get<String>("email"), email)
            val passwordPredicate = cb.equal(root.get<String>("password"), password)

            cr.select(root).where(cb.and(emailPredicate, passwordPredicate))

            val result = sess.createQuery(cr).resultList

            val account = if (result.isNotEmpty()) result.first() else null

            account?.devices = account?.devices?.filter { it.iot }?.toMutableSet() ?: mutableSetOf() //TODO optimize query instead of filtering afterwards

            return account
        }
    }*/

    /*override fun getAccount(email: String, password: String): Account? {
        val session = ControllerDb.sessionFactory.openSession()
        session.use { sess ->
            val cb = sess.criteriaBuilder
            val cr = cb.createQuery(Account::class.java)
            val root = cr.from(Account::class.java)

            // Join with devices and filter by iot == true
            val devicesJoin = root.join<Account, Device>("devices")
            val emailPredicate = cb.equal(root.get<String>("email"), email)
            val passwordPredicate = cb.equal(root.get<String>("password"), password)
            //val iotPredicate = cb.equal(devicesJoin.get<Boolean>("iot"), true)

            // Use distinct to ensure no duplicate account results due to join
            cr.select(root).distinct(true).where(cb.and(emailPredicate, passwordPredicate/*, iotPredicate*/))

           // val result = sess.createQuery(cr).resultList
          //  return result.firstOrNull() // Returns the first result if present, else null

            val query = sess.createQuery(cr).uniqueResultOptional()
            return query.orElse(null)
        }
    }*/

    override fun getAccountEagerSkipTasks(email: String, password: String): Account? {
        val session = ControllerDb.sessionFactory.openSession()
        session.use { sess ->
           /* val cb = sess.criteriaBuilder
            val cr = cb.createQuery(Account::class.java)
            val root = cr.from(Account::class.java)

            // Predicates for email and password
            val emailPredicate = cb.equal(root.get<String>("email"), email)
            val passwordPredicate = cb.equal(root.get<String>("password"), password)

            // Select and apply predicates
           // cr.select(root).distinct(true).where(cb.and(emailPredicate, passwordPredicate))
            cr.select(root).where(cb.and(emailPredicate, passwordPredicate))

            // Execute with uniqueResultOptional for efficiency
            val query = sess.createQuery(cr).uniqueResultOptional()
            return query.orElse(null)*/

            val cb = sess.criteriaBuilder
            val cr = cb.createQuery(Account::class.java)
            val root = cr.from(Account::class.java)

            // Fetch devices along with their nested sensorTypes and taskTypes
            val devicesFetch = root.fetch<Account, Device>("devices", JoinType.LEFT)
            devicesFetch.fetch<Device, SensorType>("sensorTypes", JoinType.LEFT)
            devicesFetch.fetch<Device, TaskType>("taskTypes", JoinType.LEFT)

            // Add predicates for email and password
            val emailPredicate = cb.equal(root.get<String>("email"), email)
            val passwordPredicate = cb.equal(root.get<String>("password"), password)

            // Apply the selection and predicates
            cr.select(root).where(cb.and(emailPredicate, passwordPredicate))

            // Execute the query
            val query = sess.createQuery(cr).uniqueResultOptional()
            return query.orElse(null)
        }
    }

    override fun getAccountLazy(email: String, password: String): Account? {


        return null
    }

    override fun getAll(): List<Account> {
        val session = ControllerDb.sessionFactory.openSession()

        val cb = session.criteriaBuilder

        val cr = cb.createQuery(Account::class.java)
        val root = cr.from(Account::class.java)
        cr.select(root)

        val query = session.createQuery(cr)
        val list = query.resultList
        return list
    }

}