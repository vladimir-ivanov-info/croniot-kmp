package com.croniot.server.db.daos

import com.croniot.server.db.controllers.ControllerDb
import java.sql.Connection

class SensorDataDaoImpl : SensorDataDao {

    override fun insert(uuid: String, id_sensor: String, value : String) {
        val connection: Connection = ControllerDb.getConnection()
        val sql = "INSERT IGNORE INTO sensor_data (uuid, id_sensor, value, datetime) VALUES (?, ?, ?, ?)"
        val ps = connection.prepareStatement(sql)

        val currentDateTime = Global.getCurrentDateTime()

        ps.setString(1, uuid)
        ps.setString(2, id_sensor)
        ps.setString(3, value)
        ps.setString(4, currentDateTime)

        ps.executeUpdate()
    }
}