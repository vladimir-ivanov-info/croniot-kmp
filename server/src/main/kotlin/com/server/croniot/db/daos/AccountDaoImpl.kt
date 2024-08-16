package com.croniot.server.db.daos

import croniot.models.*
import com.croniot.server.db.controllers.ControllerDb
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

    override fun getAccount(email: String, password: String): Account? {
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
    }

    override fun getEmail(accountId: Long): String? {
        val connection: Connection = ControllerDb.getConnection()
        val sql = "SELECT email FROM account WHERE id = ?"
        val ps = connection.prepareStatement(sql)
        ps.setLong(1, accountId)
        val rs = ps.executeQuery()

        var email : String? = null

        //TODO check rs not null
        if(rs.next()){
            email = rs.getString("email");

        }
        return email
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