package com.croniot.server.db.daos

import com.croniot.server.db.controllers.ControllerDb
import croniot.models.SensorType

import croniot.models.SensorInfoDb
import java.sql.Connection
import java.util.*

class SensorDaoImpl : SensorDao {

    override fun insert(sensorType: SensorType) : Long {

        val session = ControllerDb.sessionFactory.openSession()
        val transaction = session.beginTransaction()
        val sensorId: Long
        try {
            session.persist(sensorType)
            session.flush()
            sensorId = sensorType.id

            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            //session.close()
        }
        return sensorId
    }

    override fun getColumnsUuidId(): List<SensorInfoDb> {
        val connection: Connection = ControllerDb.getConnection()
        val sql = "SELECT id_client, id_sensor FROM sensor_info"
        val ps = connection.prepareStatement(sql)
        val rs = ps.executeQuery()

        val sensorInfoList = LinkedList<SensorInfoDb>()

        //TODO check rs not null
        while(rs.next()){
            val uuid = rs.getString("id_client");
            val id = rs.getString("id_sensor");

            sensorInfoList.add(SensorInfoDb(uuid = uuid, id = id))
        }

        return sensorInfoList
    }

}