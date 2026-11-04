-- Firmware updates over the air (documentation/plans/ota.md, ADR 0010).

-- One row per uploaded firmware image. `project` and `version` are read
-- from the image's own ESP-IDF app descriptor (esp_app_desc_t), not typed
-- in by whoever uploads it, so they always match what the device will
-- report after installing it. The image lives in the row: a few MB per
-- release, and backups and the test database get it for free.
CREATE TABLE IF NOT EXISTS firmware_release (
    id BIGSERIAL PRIMARY KEY,
    project VARCHAR(32) NOT NULL,
    version VARCHAR(32) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    image BYTEA NOT NULL,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_firmware_release_project_version UNIQUE (project, version)
);

-- The release each device has been asked to install, at most one at a
-- time. Mirrors the retained /server/<uuid>/ota message; request_id
-- changes on every new request, which is how a device tells "retry this
-- version" apart from the same request arriving again on reconnect.
CREATE TABLE IF NOT EXISTS device_ota_request (
    device BIGINT PRIMARY KEY,
    firmware_release BIGINT NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    requested_at TIMESTAMPTZ(6) NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_device_ota_request_device
        FOREIGN KEY (device)
        REFERENCES device(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_device_ota_request_release
        FOREIGN KEY (firmware_release)
        REFERENCES firmware_release(id)
        ON DELETE CASCADE
);
