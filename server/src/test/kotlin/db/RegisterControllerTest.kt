package db

import io.mockk.every
import io.mockk.mockk

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.assertEquals


import io.mockk.MockKAnnotations
import org.junit.Before
import org.junit.jupiter.api.Assertions.*
/*
class RegisterControllerTest {

    @org.junit.jupiter.api.Test
    fun registerAccountAndDevice() {

    }

    @org.junit.jupiter.api.Test
    fun registerDevice() {
    }


    @Before
    fun onBefore(){
        //MockAnnotations.init(this)
    }
//}

//import org.junit.Test
//import kotlin.test.assertEquals

//class MyKotlinTest {

    @Test
    fun testAddition() {
        val sum = 2 + 3
        assertEquals(5, sum, "2 + 3 should equal 5")
    }
}*/

//@Testcontainers
//class RegisterControllerTest {
//
//    @Container
//    //private val postgresContainer = PostgreSQLContainer<Nothing>("postgres:latest")
//   /* private val postgresContainer = PostgreSQLContainer("postgres:latest")
//        .withDatabaseName("testdb")
//        .withUsername("testuser")
//        .withPassword("testpass")**/
//
//    private val postgresContainer = PostgreSQLContainer("postgres:latest")
//        .withDatabaseName("iot_testdb")
//        .withUsername("testuser")
//        .withPassword("testpass")
//        //.with
//
//    private lateinit var connection: Connection
//
//    @BeforeEach
//    fun setUp() {
//        postgresContainer.start()  // Ensure the container is started
//        connection = DriverManager.getConnection(
//            postgresContainer.jdbcUrl,
//            postgresContainer.username,
//            postgresContainer.password
//        )
//        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS test_table (id SERIAL PRIMARY KEY, name VARCHAR(255));")
//    }
//
//    @AfterEach
//    fun tearDown() {
//        connection.createStatement().execute("DROP TABLE test_table;")
//        connection.close()
//        postgresContainer.stop()  // Ensure the container is stopped
//    }
//
//    @Test
//    fun testInsertAndSelect() {
//        val insertStatement = connection.prepareStatement("INSERT INTO test_table (name) VALUES (?)")
//        insertStatement.setString(1, "Test Name")
//        insertStatement.executeUpdate()
//
//        val resultSet = connection.createStatement().executeQuery("SELECT name FROM test_table")
//        resultSet.next()
//        val name = resultSet.getString("name")
//
//        assertEquals("Test Name", name)
//    }
//}
//}