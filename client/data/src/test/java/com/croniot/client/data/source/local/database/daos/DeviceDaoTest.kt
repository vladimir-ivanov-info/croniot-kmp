package com.croniot.client.data.source.local.database.daos

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.croniot.client.data.source.local.database.AppDatabase
import com.croniot.client.data.source.local.database.entities.AccountEntity
import com.croniot.client.data.source.local.database.entities.DeviceEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DeviceDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var deviceDao: DeviceDao
    private lateinit var accountDao: AccountDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        deviceDao = db.deviceDao()
        accountDao = db.accountDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun insertAccount(): Long =
        accountDao.insert(AccountEntity(uuid = "acc-1", nickname = "nick", email = "user@example.com"))

    @Test
    fun `WHEN a device is inserted THEN getByUuid returns the persisted device`() = runTest {
        val accountId = insertAccount()
        deviceDao.insert(DeviceEntity(uuid = "device-1", accountId = accountId, name = "Device 1", description = "desc"))

        val result = deviceDao.getByUuid("device-1")

        assertEquals("device-1", result?.uuid)
        assertEquals("Device 1", result?.name)
    }

    @Test
    fun `WHEN uuid is unknown THEN getByUuid returns null`() = runTest {
        assertNull(deviceDao.getByUuid("unknown"))
    }

    @Test
    fun `WHEN an account has multiple devices THEN getByAccountId returns every one`() = runTest {
        val accountId = insertAccount()
        deviceDao.insert(DeviceEntity(uuid = "device-1", accountId = accountId, name = "A", description = ""))
        deviceDao.insert(DeviceEntity(uuid = "device-2", accountId = accountId, name = "B", description = ""))

        val result = deviceDao.getByAccountId(accountId)

        assertEquals(2, result.size)
    }

    @Test
    fun `WHEN the parent account is deleted THEN it cascades and deletes its devices`() = runTest {
        val accountId = insertAccount()
        deviceDao.insert(DeviceEntity(uuid = "device-1", accountId = accountId, name = "A", description = ""))

        accountDao.deleteByUuid("acc-1")

        assertNull(deviceDao.getByUuid("device-1"))
    }
}
