package com.croniot.client.data.source.local.database.daos

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.croniot.client.data.source.local.database.AppDatabase
import com.croniot.client.data.source.local.database.entities.AccountEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AccountDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: AccountDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.accountDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `WHEN an account is inserted THEN getByUuid returns the persisted account`() = runTest {
        dao.insert(AccountEntity(uuid = "acc-1", nickname = "nick", email = "user@example.com"))

        val result = dao.getByUuid("acc-1")

        assertEquals("nick", result?.nickname)
    }

    @Test
    fun `WHEN uuid is unknown THEN getByUuid returns null`() = runTest {
        assertNull(dao.getByUuid("unknown"))
    }

    @Test
    fun `WHEN multiple accounts are inserted THEN getAll returns every one`() = runTest {
        dao.insert(AccountEntity(uuid = "acc-1", nickname = "A", email = "a@example.com"))
        dao.insert(AccountEntity(uuid = "acc-2", nickname = "B", email = "b@example.com"))

        assertEquals(2, dao.getAll().size)
    }

    @Test
    fun `WHEN inserting with the same uuid THEN it replaces the previous row`() = runTest {
        dao.insert(AccountEntity(uuid = "acc-1", nickname = "Old", email = "old@example.com"))
        dao.insert(AccountEntity(uuid = "acc-1", nickname = "New", email = "new@example.com"))

        val all = dao.getAll()

        assertEquals(1, all.size)
        assertEquals("New", all.first().nickname)
    }

    @Test
    fun `WHEN deleteByUuid is called THEN it removes the account`() = runTest {
        dao.insert(AccountEntity(uuid = "acc-1", nickname = "nick", email = "user@example.com"))

        dao.deleteByUuid("acc-1")

        assertNull(dao.getByUuid("acc-1"))
    }

    @Test
    fun `WHEN there are no accounts THEN getAll returns an empty list`() = runTest {
        assertTrue(dao.getAll().isEmpty())
    }
}
