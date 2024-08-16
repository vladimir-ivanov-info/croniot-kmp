package db.controllers

import org.hibernate.SessionFactory
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.Configuration
import org.hibernate.service.ServiceRegistry

object HibernateUtil {
    private lateinit var sessionFactory: SessionFactory

   /* fun getSessionFactory(testing: Boolean = false): SessionFactory {
        if (!::sessionFactory.isInitialized) {
            try {
                val configuration = Configuration()
                configuration.configure(if (testing) "META-INF/hibernate-test.cfg.xml" else "META-INF/hibernate.cfg.xml")
                val serviceRegistry: ServiceRegistry = StandardServiceRegistryBuilder()
                    .applySettings(configuration.properties)
                    .build()
                sessionFactory = configuration.buildSessionFactory(serviceRegistry)
            } catch (ex: Exception) {
                ex.printStackTrace()
                throw RuntimeException("There was an error building the factory")
            }
        }
        return sessionFactory
    }*/


    fun getSessionFactory(testing: Boolean = false): SessionFactory {
        if (!::sessionFactory.isInitialized) {
            try {
                val configuration = Configuration()
                //val resourceName = if (testing) "META-INF/hibernate-test.cfg.xml" else "META-INF/hibernate.cfg.xml"
                val resourceName = "META-INF/hibernate.cfg.xml"
                configuration.configure(resourceName)
                val serviceRegistry: ServiceRegistry = StandardServiceRegistryBuilder()
                    .applySettings(configuration.properties)
                    .build()
                sessionFactory = configuration.buildSessionFactory(serviceRegistry)
            } catch (ex: Exception) {
                ex.printStackTrace()
                throw RuntimeException("There was an error building the factory")
            }
        }
        return sessionFactory
    }

    fun shutdown() {
        if (::sessionFactory.isInitialized) {
            sessionFactory.close()
        }
    }
}