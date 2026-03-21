package com.server.croniot.application

import javax.sql.DataSource

object DatabaseSchemaInitializer {

    fun createSchemaIfNeeded(dataSource: DataSource) {
        val statements = listOf(

            // =============================================
            // ACCOUNT
            // =============================================
            """
            CREATE TABLE IF NOT EXISTS account (
                id BIGSERIAL PRIMARY KEY,
                uuid VARCHAR(255) NOT NULL,
                nickname VARCHAR(255),
                email VARCHAR(255) NOT NULL,
                password VARCHAR(255) NOT NULL
            )
            """.trimIndent(),

            // =============================================
            // DEVICE
            // =============================================
            """
            CREATE TABLE IF NOT EXISTS device (
                id BIGSERIAL PRIMARY KEY,
                uuid VARCHAR(255) NOT NULL,
                name VARCHAR(255),
                description VARCHAR(255),
                iot BOOLEAN DEFAULT FALSE,
                account BIGINT NOT NULL,
                device_order INTEGER,

                CONSTRAINT uq_device_uuid UNIQUE (uuid),

                CONSTRAINT fk_device_account
                    FOREIGN KEY (account)
                    REFERENCES account(id)
                    ON DELETE CASCADE
            )
            """.trimIndent(),

            // =============================================
            // DEVICE_PROPERTIES
            // =============================================
            """
            CREATE TABLE IF NOT EXISTS device_properties (
                device_id BIGINT NOT NULL,
                property_key VARCHAR(255) NOT NULL,
                property_value VARCHAR(255),
                PRIMARY KEY (device_id, property_key),
                CONSTRAINT fk_device_properties_device
                    FOREIGN KEY (device_id)
                    REFERENCES device(id)
                    ON DELETE CASCADE
            )
            """.trimIndent(),

            // =============================================
            // DEVICE_TOKEN
            // =============================================
            """
            CREATE TABLE IF NOT EXISTS device_token (
                id BIGSERIAL PRIMARY KEY,
                device BIGINT NOT NULL,
                token VARCHAR(255) NOT NULL,
                CONSTRAINT fk_device_token_device
                    FOREIGN KEY (device)
                    REFERENCES device(id)
                    ON DELETE CASCADE
            )
            """.trimIndent(),

            // =============================================
            // SENSOR_TYPE
            // =============================================
            """
            CREATE TABLE IF NOT EXISTS sensor_type (
                id BIGSERIAL PRIMARY KEY,
                uid BIGINT NOT NULL,
                name VARCHAR(255),
                description VARCHAR(255),
                device BIGINT NOT NULL,
                sensor_type_order INTEGER,

                CONSTRAINT fk_sensor_type_device
                    FOREIGN KEY (device)
                    REFERENCES device(id)
                    ON DELETE CASCADE,

                CONSTRAINT uq_sensor_type_device_uid
                    UNIQUE (device, uid)
            )
            """.trimIndent(),

            // =============================================
            // TASK_TYPE
            // =============================================
            """
            CREATE TABLE IF NOT EXISTS task_type (
                id BIGSERIAL PRIMARY KEY,
                uid BIGINT NOT NULL,
                name VARCHAR(255),
                description VARCHAR(255),
                device BIGINT NOT NULL,
                task_type_order INTEGER,

                CONSTRAINT fk_task_type_device
                    FOREIGN KEY (device)
                    REFERENCES device(id)
                    ON DELETE CASCADE,

                CONSTRAINT uq_task_type_device_uid
                    UNIQUE (device, uid)
            )
            """.trimIndent(),

            // =============================================
            // PARAMETER_SENSOR
            // =============================================
            """
            CREATE TABLE IF NOT EXISTS parameter_sensor (
                id BIGSERIAL PRIMARY KEY,
                uid BIGINT,
                name VARCHAR(255),
                type VARCHAR(255),
                unit VARCHAR(255),
                description VARCHAR(255),
                sensor_type BIGINT NOT NULL,
                param_order INTEGER,
                CONSTRAINT fk_parameter_sensor_sensor_type
                    FOREIGN KEY (sensor_type)
                    REFERENCES sensor_type(id)
                    ON DELETE CASCADE
            )
            """.trimIndent(),

            // =============================================
            // PARAMETER_SENSOR_CONSTRAINTS
            // =============================================
            """
            CREATE TABLE IF NOT EXISTS parameter_sensor_constraints (
                parameter_id BIGINT NOT NULL,
                constraint_key VARCHAR(255) NOT NULL,
                constraint_value VARCHAR(255),
                PRIMARY KEY (parameter_id, constraint_key),
                CONSTRAINT fk_param_sensor_constraints_param
                    FOREIGN KEY (parameter_id)
                    REFERENCES parameter_sensor(id)
                    ON DELETE CASCADE
            )
            """.trimIndent(),

            // =============================================
            // PARAMETER_TASK
            // =============================================
            """
            CREATE TABLE IF NOT EXISTS parameter_task (
                id BIGSERIAL PRIMARY KEY,
                uid BIGINT,
                name VARCHAR(255),
                type VARCHAR(255),
                unit VARCHAR(255),
                description VARCHAR(255),
                task_type BIGINT NOT NULL,
                param_order INTEGER,
                CONSTRAINT fk_parameter_task_task_type
                    FOREIGN KEY (task_type)
                    REFERENCES task_type(id)
                    ON DELETE CASCADE
            )
            """.trimIndent(),

            // =============================================
            // PARAMETER_TASK_CONSTRAINTS
            // =============================================
            """
            CREATE TABLE IF NOT EXISTS parameter_task_constraints (
                parameter_id BIGINT NOT NULL,
                constraint_key VARCHAR(255) NOT NULL,
                constraint_value VARCHAR(255),
                PRIMARY KEY (parameter_id, constraint_key),
                CONSTRAINT fk_param_task_constraints_param
                    FOREIGN KEY (parameter_id)
                    REFERENCES parameter_task(id)
                    ON DELETE CASCADE
            )
            """.trimIndent(),

            // =============================================
            // SENSOR_DATA
            // =============================================
            """
            CREATE TABLE IF NOT EXISTS sensor_data (
                id BIGSERIAL PRIMARY KEY,
                device BIGINT NOT NULL,
                sensortype BIGINT NOT NULL,
                value VARCHAR(255),
                date_time TIMESTAMPTZ(6),
                CONSTRAINT fk_sensor_data_device
                    FOREIGN KEY (device)
                    REFERENCES device(id)
                    ON DELETE CASCADE,
                CONSTRAINT fk_sensor_data_sensor_type
                    FOREIGN KEY (sensortype)
                    REFERENCES sensor_type(id)
                    ON DELETE CASCADE
            )
            """.trimIndent(),

            // =============================================
            // TASK
            // =============================================
            """
            CREATE TABLE IF NOT EXISTS task (
                id BIGSERIAL PRIMARY KEY,
                uid BIGINT,
                task_type BIGINT NOT NULL,
                task_order INTEGER,
                CONSTRAINT fk_task_task_type
                    FOREIGN KEY (task_type)
                    REFERENCES task_type(id)
                    ON DELETE CASCADE
            )
            """.trimIndent(),

            // =============================================
            // TASK_STATE_INFO
            // =============================================
            """
            CREATE TABLE IF NOT EXISTS task_state_info (
                id BIGSERIAL PRIMARY KEY,
                date_time TIMESTAMPTZ(6),
                state VARCHAR(255),
                progress DOUBLE PRECISION,
                error_message VARCHAR(255),
                task BIGINT NOT NULL,
                state_order INTEGER,
                CONSTRAINT fk_task_state_info_task
                    FOREIGN KEY (task)
                    REFERENCES task(id)
                    ON DELETE CASCADE
            )
            """.trimIndent(),

            // =============================================
            // TASK_PARAMETER_VALUE
            // =============================================
            """
            CREATE TABLE IF NOT EXISTS task_parameter_value (
                id_task BIGINT NOT NULL,
                id_parameter BIGINT NOT NULL,
                value VARCHAR(255),
                PRIMARY KEY (id_task, id_parameter),
                CONSTRAINT fk_task_param_value_task
                    FOREIGN KEY (id_task)
                    REFERENCES task(id)
                    ON DELETE CASCADE,
                CONSTRAINT fk_task_param_value_param
                    FOREIGN KEY (id_parameter)
                    REFERENCES parameter_task(id)
                    ON DELETE CASCADE
            )
            """.trimIndent(),

            // =============================================
            // INDEXES
            // =============================================
            "CREATE INDEX IF NOT EXISTS idx_device_account ON device(account)",
            "CREATE INDEX IF NOT EXISTS idx_sensor_type_device ON sensor_type(device)",
            "CREATE INDEX IF NOT EXISTS idx_task_type_device ON task_type(device)",
            "CREATE INDEX IF NOT EXISTS idx_sensor_data_device ON sensor_data(device)",
            "CREATE INDEX IF NOT EXISTS idx_sensor_data_sensortype ON sensor_data(sensortype)",
            "CREATE INDEX IF NOT EXISTS idx_sensor_data_datetime ON sensor_data(date_time)",
            "CREATE INDEX IF NOT EXISTS idx_task_state_info_task ON task_state_info(task)",
            "CREATE INDEX IF NOT EXISTS idx_task_task_type ON task(task_type)"
        )

        dataSource.connection.use { connection ->
            connection.autoCommit = false

            try {
                connection.createStatement().use { statement ->
                    statements.forEach { sql ->
                        statement.execute(sql)
                    }
                }
                connection.commit()
            } catch (e: Exception) {
                connection.rollback()
                throw IllegalStateException("Error creating database schema", e)
            }
        }
    }
}