-- Copyright 2021 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

---------- INSTANCE_STATS_HOUR TABLE START ------------
BEGIN;
CREATE TABLE IF NOT EXISTS INSTANCE_STATS_HOUR (
	REPORTEDAT TIMESTAMPTZ NOT NULL,
	ACCOUNTID TEXT,
	APPID TEXT,
	SERVICEID TEXT,
	ENVID TEXT,
	CLOUDPROVIDERID TEXT,
	INSTANCETYPE TEXT,
	INSTANCECOUNT INTEGER,
	ARTIFACTID TEXT,
	SANITYSTATUS BOOLEAN NOT NULL DEFAULT FALSE,
	CREATEDAT TIMESTAMPTZ DEFAULT (NOW()),
	UPDATEDAT TIMESTAMPTZ DEFAULT (NOW()),

	CONSTRAINT INSTANCE_STATS_HOUR_UNIQUE_RECORD_INDEX UNIQUE(ACCOUNTID,APPID,SERVICEID,ENVID,CLOUDPROVIDERID,INSTANCETYPE,REPORTEDAT)
);
COMMIT;

SELECT CREATE_HYPERTABLE('INSTANCE_STATS_HOUR','reportedat',chunk_time_interval => interval '1 month',if_not_exists => TRUE);

BEGIN;
CREATE INDEX IF NOT EXISTS INSTANCE_STATS_HOUR_APPID_INDEX ON INSTANCE_STATS_HOUR(APPID,REPORTEDAT DESC);
CREATE INDEX IF NOT EXISTS INSTANCE_STATS_HOUR_ACCOUNTID_INDEX ON INSTANCE_STATS_HOUR(ACCOUNTID,REPORTEDAT DESC);
CREATE INDEX IF NOT EXISTS INSTANCE_STATS_HOUR_SERVICEID_INDEX ON INSTANCE_STATS_HOUR(SERVICEID,REPORTEDAT DESC);
CREATE INDEX IF NOT EXISTS INSTANCE_STATS_HOUR_ENVID_INDEX ON INSTANCE_STATS_HOUR(ENVID,REPORTEDAT DESC);
CREATE INDEX IF NOT EXISTS INSTANCE_STATS_HOUR_CLOUDPROVIDERID_INDEX ON INSTANCE_STATS_HOUR(CLOUDPROVIDERID,REPORTEDAT DESC);
CREATE INDEX IF NOT EXISTS INSTANCE_STATS_HOUR_INSTANCECOUNT_INDEX ON INSTANCE_STATS_HOUR(INSTANCECOUNT,REPORTEDAT DESC);
CREATE INDEX IF NOT EXISTS INSTANCE_STATS_HOUR_ARTIFACTID_INDEX ON INSTANCE_STATS_HOUR(ARTIFACTID,REPORTEDAT DESC);
CREATE INDEX IF NOT EXISTS INSTANCE_STATS_HOUR_CREATEDAT_INDEX ON INSTANCE_STATS_HOUR(CREATEDAT DESC);
CREATE INDEX IF NOT EXISTS INSTANCE_STATS_HOUR_UPDATEDAT_INDEX ON INSTANCE_STATS_HOUR(UPDATEDAT DESC);
CREATE INDEX IF NOT EXISTS INSTANCE_STATS_HOUR_SANITYSTATUS_INDEX ON INSTANCE_STATS_HOUR(SANITYSTATUS,REPORTEDAT DESC);
COMMIT;

DROP TRIGGER IF EXISTS update_updatedAt_col ON INSTANCE_STATS_HOUR;
CREATE TRIGGER update_updatedAt_col BEFORE UPDATE ON INSTANCE_STATS_HOUR FOR EACH ROW EXECUTE PROCEDURE update_updatedAt_column();

---------- INSTANCE_STATS_HOUR TABLE END ------------
