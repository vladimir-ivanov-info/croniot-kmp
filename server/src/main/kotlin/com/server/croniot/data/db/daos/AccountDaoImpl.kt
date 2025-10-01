package com.server.croniot.data.db.daos

import croniot.models.*
import jakarta.persistence.criteria.JoinType
import org.hibernate.Hibernate
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

    /*override fun getAccountEagerSkipTasks(email: String, password: String): Account? {
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
    }*/

    override fun getAccountEagerSkipTasks(email: String, password: String): Account? {
        sessionFactory.openSession().use { sess ->
            val tx = sess.beginTransaction()
            try {
                val cb = sess.criteriaBuilder
                val cr = cb.createQuery(Account::class.java)
                val root = cr.from(Account::class.java)

                val devicesFetch = root.fetch<Account, Device>("devices", JoinType.LEFT)
                // Traes sensorTypes; OK
                devicesFetch.fetch<Device, SensorType>("sensorTypes", JoinType.LEFT)
                // Si “SkipTasks” es literal, puedes no traer taskTypes aquí.
                // Si necesitas taskTypes (sin sus tasks), déjalo:
                devicesFetch.fetch<Device, TaskType>("taskTypes", JoinType.LEFT)

                val emailPredicate = cb.equal(root.get<String>("email"), email)
                val passwordPredicate = cb.equal(root.get<String>("password"), password)

                cr.select(root)
                    .where(cb.and(emailPredicate, passwordPredicate))
                    .distinct(true)

                val account = sess.createQuery(cr).uniqueResultOptional().orElse(null) ?: run {
                    tx.commit(); return null
                }

                // 🔴 IMPORTANTE: inicializa las colecciones anidadas aquí
                account.devices.forEach { d ->
                    d.sensorTypes.forEach { st ->
                        Hibernate.initialize(st.parameters) // ← evita LazyInitializationException
                    }
                    // Si también necesitas parámetros de TaskType:
                    // d.taskTypes.forEach { tt -> Hibernate.initialize(tt.parameters) }
                    // 👇 inicializa también los parámetros de cada TaskType
                    d.taskTypes.forEach { tt ->
                        Hibernate.initialize(tt.parameters)
                    }
                }

                tx.commit()
                return account
            } catch (e: Exception) {
                tx.rollback()
                throw e
            }
        }
    }

    /*override fun getAccountEagerSkipTasks(email: String, password: String): Account? {
        val session = sessionFactory.openSession()
        session.use { sess ->
            val tx = sess.beginTransaction()
            try {
                // --- Query principal: Account + devices + sensorTypes + taskTypes ---
                val cb = sess.criteriaBuilder
                val cr = cb.createQuery(Account::class.java)
                val root = cr.from(Account::class.java)

                val devicesFetch = root.fetch<Account, Device>("devices", JoinType.LEFT)
                devicesFetch.fetch<Device, SensorType>("sensorTypes", JoinType.LEFT)
                devicesFetch.fetch<Device, TaskType>("taskTypes", JoinType.LEFT)

                val emailPredicate = cb.equal(root.get<String>("email"), email)
                val passwordPredicate = cb.equal(root.get<String>("password"), password)

                cr.select(root)
                    .distinct(true) // evita duplicados por los JOIN FETCH
                    .where(cb.and(emailPredicate, passwordPredicate))

                val account = sess.createQuery(cr)
                    .setCacheable(false)
                    .uniqueResultOptional()
                    .orElse(null)

                if (account == null) {
                    tx.commit()
                    return null
                }

                // --- Segunda query: traer parameters de TODOS los sensorTypes ya cargados ---
                val sensorTypeIds = account.devices
                    .flatMap { it.sensorTypes }
                    .mapNotNull { it.id }
                    .toSet()

                if (sensorTypeIds.isNotEmpty()) {
                    // Cambia "parameters" si tu propiedad tiene otro nombre (p. ej. "parametersValues")
                    sess.createQuery(
                        """
                    select distinct st
                    from SensorType st
                    left join fetch st.parameters p
                    where st.id in (:ids)
                    """.trimIndent(),
                        SensorType::class.java
                    )
                        .setParameterList("ids", sensorTypeIds)
                        .setCacheable(false)
                        .list()
                }

                // --- Forzar inicialización (por si queda algo perezoso) ---
                account.devices.forEach { d ->
                    d.sensorTypes.forEach { st ->
                        Hibernate.initialize(st.parameters)
                        st.parameters.size // toque de calentamiento
                    }
                }

                tx.commit()
                return account
            } catch (ex: Exception) {
                tx.rollback()
                throw ex
            }
        }
    }*/

    /* override fun getAccountEagerSkipTasks(email: String, password: String): Account? {
         val session = sessionFactory.openSession()
         session.use { sess ->
             // Paso 1: Account + devices + sensorTypes + taskTypes
             val account = sess.createQuery(
                 """
             select distinct a
             from Account a
             left join fetch a.devices d
             left join fetch d.sensorTypes st
             left join fetch d.taskTypes tt
             where a.email = :email and a.password = :pwd
             """.trimIndent(),
                 Account::class.java
             )
                 .setParameter("email", email)
                 .setParameter("pwd", password)
                 .uniqueResultOptional()
                 .orElse(null)

             if (account == null) return null

             // Paso 2: Inicializar parameters de TODOS los sensorTypes cargados (una sola query)
             val sensorTypeIds = account.devices
                 .flatMap { it.sensorTypes }
                 .mapNotNull { it.id }
                 .toSet()

             if (sensorTypeIds.isNotEmpty()) {
                 // Esta query hace el fetch de la colección "parameters"
                 sess.createQuery(
                     """
                 select distinct st
                 from SensorType st
                 left join fetch st.parameters p
                 where st.id in (:ids)
                 """.trimIndent(),
                     SensorType::class.java
                 )
                     .setParameterList("ids", sensorTypeIds)
                     .list()
                 // No hace falta usar el resultado; el fetch deja las colecciones inicializadas en el PC.
             }

             return account
         }
     }*/

    /*override fun getAccountEagerSkipTasks(email: String, password: String): Account? {
        val session = sessionFactory.openSession()
        session.use { sess ->
            val tx = sess.beginTransaction()
            try {
                // 1) Account + devices + sensorTypes + taskTypes (sin cache)
                val account = sess.createQuery(
                    """
                select distinct a
                from Account a
                left join fetch a.devices d
                left join fetch d.sensorTypes st
                left join fetch d.taskTypes tt
                where a.email = :email and a.password = :pwd
                """.trimIndent(),
                    Account::class.java
                )
                    .setParameter("email", email)
                    .setParameter("password", password)
                    .setCacheable(false)
                    .uniqueResultOptional()
                    .orElse(null)

                if (account == null) {
                    tx.commit()
                    return null
                }

                // 2) IDs de los SensorType cargados
                val sensorTypeIds = account.devices
                    .flatMap { it.sensorTypes }
                    .mapNotNull { it.id }
                    .toSet()

                if (sensorTypeIds.isNotEmpty()) {
                    // 3) Evitar colecciones cacheadas vacías: purga L2 cache de la colección
                    // Role = "<paquete>.SensorType.parameters"
                    val role = SensorType::class.java.name + ".parameters"
                    val cache = sess.sessionFactory.cache
                    // Evict global por si acaso
                    cache.evictCollectionData(role)
                    // Evict por id (algunos setups lo agradecen)
                    sensorTypeIds.forEach { id ->
                        try { cache.evictCollectionData(role, id) } catch (_: Exception) {}
                    }

                    // 4) Fetch de parameters para TODOS los SensorType (sin cache)
                    sess.createQuery(
                        """
                    select distinct st
                    from SensorType st
                    left join fetch st.parameters p
                    where st.id in (:ids)
                    """.trimIndent(),
                        SensorType::class.java
                    )
                        .setParameterList("ids", sensorTypeIds)
                        .setCacheable(false)
                        .list()
                }

                // 5) Forzar inicialización por si quedara algo perezoso
                account.devices.forEach { d ->
                    d.sensorTypes.forEach { st ->
                        org.hibernate.Hibernate.initialize(st.parameters)
                        st.parameters.size // toque para inicializar
                    }
                }

                tx.commit()
                return account
            } catch (ex: Exception) {
                tx.rollback()
                throw ex
            }
        }
    }*/

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
