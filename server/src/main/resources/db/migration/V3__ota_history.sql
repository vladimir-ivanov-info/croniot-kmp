-- Update history and installed firmware, for the device page of admin-ui
-- (documentation/server.md §4c). Both are filled from the device's own
-- events (firmware_info, ota_*), see services/OtaEventTracker.kt.

-- Read from the image on upload, like project and version. image_hash is
-- the SHA-256 ESP-IDF appends to the image (its last 32 bytes): the
-- device reports it for the image it runs, which is how an installed
-- firmware is matched to its upload. NULL for releases uploaded before
-- this migration and for images without an appended hash.
ALTER TABLE firmware_release ADD COLUMN IF NOT EXISTS built_at VARCHAR(20);
ALTER TABLE firmware_release ADD COLUMN IF NOT EXISTS idf_version VARCHAR(32);
ALTER TABLE firmware_release ADD COLUMN IF NOT EXISTS image_hash CHAR(64);

-- One row per update request; device_ota_request keeps only the pending
-- one, mirroring the retained MQTT message. `status` is one of:
-- requested, downloading, retrying, installed, validating, probing
-- (still going) and confirmed, failed, rolled_back, ignored, cancelled
-- (finished, finished_at set).
CREATE TABLE IF NOT EXISTS device_ota_update (
    id BIGSERIAL PRIMARY KEY,
    device BIGINT NOT NULL,
    firmware_release BIGINT,
    request_id VARCHAR(64) NOT NULL,
    version VARCHAR(32) NOT NULL,
    from_version VARCHAR(32),
    status VARCHAR(16) NOT NULL,
    progress SMALLINT NOT NULL DEFAULT 0,
    detail VARCHAR(128),
    requested_at TIMESTAMPTZ(6) NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMPTZ(6),

    CONSTRAINT uq_device_ota_update_request UNIQUE (request_id),

    CONSTRAINT fk_device_ota_update_device
        FOREIGN KEY (device)
        REFERENCES device(id)
        ON DELETE CASCADE,

    -- Deleting a release keeps its history rows.
    CONSTRAINT fk_device_ota_update_release
        FOREIGN KEY (firmware_release)
        REFERENCES firmware_release(id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_device_ota_update_device ON device_ota_update (device, requested_at DESC);

-- What the device last said it runs (its firmware_info event, once per
-- boot). image_hash is 16 hex digits: the event's message is short.
CREATE TABLE IF NOT EXISTS device_firmware (
    device BIGINT PRIMARY KEY,
    project VARCHAR(32) NOT NULL,
    version VARCHAR(32) NOT NULL,
    built_at VARCHAR(20),
    idf_version VARCHAR(32),
    size_bytes BIGINT,
    image_hash VARCHAR(16),
    slot VARCHAR(16),
    reported_at TIMESTAMPTZ(6) NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_device_firmware_device
        FOREIGN KEY (device)
        REFERENCES device(id)
        ON DELETE CASCADE
);
