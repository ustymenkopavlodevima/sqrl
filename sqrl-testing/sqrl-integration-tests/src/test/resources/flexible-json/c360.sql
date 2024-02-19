CREATE TEMPORARY FUNCTION IF NOT EXISTS `tojson` AS 'com.datasqrl.json.JsonFunctions$ToJson' LANGUAGE JAVA;

CREATE TEMPORARY FUNCTION IF NOT EXISTS `__DataSQRLUuidGenerator` AS 'com.datasqrl.SecureFunctions$Uuid' LANGUAGE JAVA;

CREATE TEMPORARY TABLE `orders$1` (
  `_uuid` AS __DATASQRLUUIDGENERATOR(),
  `_ingest_time` AS PROCTIME(),
  `_source_time` TIMESTAMP(3) WITH LOCAL TIME ZONE NOT NULL,
  `id` BIGINT NOT NULL,
  `customerid` BIGINT NOT NULL,
  `time` VARCHAR(2147483647) CHARACTER SET `UTF-16LE` NOT NULL,
  `entries` ROW(`productid` INTEGER NOT NULL, `quantity` INTEGER NOT NULL, `unit_price` DOUBLE NOT NULL, `discount` DOUBLE) NOT NULL ARRAY NOT NULL,
  WATERMARK FOR `_source_time` AS (`_source_time` - INTERVAL '1' SECOND)
) WITH (
   'connector' = 'datagen',
   'number-of-rows' = '20'
);

CREATE TEMPORARY TABLE `orders$2$1` (
  `_uuid` CHAR(36) CHARACTER SET `UTF-16LE` NOT NULL,
  `_ingest_time` TIMESTAMP(3) WITH LOCAL TIME ZONE NOT NULL,
  `_source_time` TIMESTAMP(3) WITH LOCAL TIME ZONE NOT NULL,
  `id` BIGINT NOT NULL,
  `customerid` BIGINT NOT NULL,
  `time` VARCHAR(2147483647) CHARACTER SET `UTF-16LE` NOT NULL,
  `entries` ROW(`productid` INTEGER NOT NULL, `quantity` INTEGER NOT NULL, `unit_price` DOUBLE NOT NULL, `discount` DOUBLE) NOT NULL ARRAY NOT NULL,
  `json` RAW('org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode', 'AEdvcmcuYXBhY2hlLmZsaW5rLmFwaS5qYXZhLnR5cGV1dGlscy5ydW50aW1lLmtyeW8uS3J5b1NlcmlhbGl6ZXJTbmFwc2hvdAAAAAIASG9yZy5hcGFjaGUuZmxpbmsuc2hhZGVkLmphY2tzb24yLmNvbS5mYXN0ZXJ4bWwuamFja3Nvbi5kYXRhYmluZC5Kc29uTm9kZQAABPLGmj1wAAAAAgBIb3JnLmFwYWNoZS5mbGluay5zaGFkZWQuamFja3NvbjIuY29tLmZhc3RlcnhtbC5qYWNrc29uLmRhdGFiaW5kLkpzb25Ob2RlAQAAAEoASG9yZy5hcGFjaGUuZmxpbmsuc2hhZGVkLmphY2tzb24yLmNvbS5mYXN0ZXJ4bWwuamFja3Nvbi5kYXRhYmluZC5Kc29uTm9kZQEAAABOAEhvcmcuYXBhY2hlLmZsaW5rLnNoYWRlZC5qYWNrc29uMi5jb20uZmFzdGVyeG1sLmphY2tzb24uZGF0YWJpbmQuSnNvbk5vZGUAAAAAAClvcmcuYXBhY2hlLmF2cm8uZ2VuZXJpYy5HZW5lcmljRGF0YSRBcnJheQEAAAArAClvcmcuYXBhY2hlLmF2cm8uZ2VuZXJpYy5HZW5lcmljRGF0YSRBcnJheQEAAAHwAClvcmcuYXBhY2hlLmF2cm8uZ2VuZXJpYy5HZW5lcmljRGF0YSRBcnJheQAAAAKs7QAFc3IAQm9yZy5hcGFjaGUuZmxpbmsuYXBpLmNvbW1vbi5FeGVjdXRpb25Db25maWckU2VyaWFsaXphYmxlU2VyaWFsaXplckEOvk2iII+1AgABTAAKc2VyaWFsaXplcnQAJkxjb20vZXNvdGVyaWNzb2Z0d2FyZS9rcnlvL1NlcmlhbGl6ZXI7eHBzcgBtb3JnLmFwYWNoZS5mbGluay5hcGkuamF2YS50eXBldXRpbHMucnVudGltZS5rcnlvLlNlcmlhbGl6ZXJzJFNwZWNpZmljSW5zdGFuY2VDb2xsZWN0aW9uU2VyaWFsaXplckZvckFycmF5TGlzdAAAAAAAAAABAgAAeHIAYW9yZy5hcGFjaGUuZmxpbmsuYXBpLmphdmEudHlwZXV0aWxzLnJ1bnRpbWUua3J5by5TZXJpYWxpemVycyRTcGVjaWZpY0luc3RhbmNlQ29sbGVjdGlvblNlcmlhbGl6ZXIAAAAAAAAAAQIAAUwABHR5cGV0ABFMamF2YS9sYW5nL0NsYXNzO3hwdnIAE2phdmEudXRpbC5BcnJheUxpc3R4gdIdmcdhnQMAAUkABHNpemV4cAAABPLGmj1wAAAAAAAABPLGmj1wAAAAAA==')
) WITH (
  'properties.bootstrap.servers' = '${kafka.bootstrap}',
  'connector' = 'kafka',
  'format' = 'flexible-json',
  'topic' = 'orders',
  'properties.group.id' = 'datasqrl-orders'
);

CREATE VIEW `root$1`
AS
SELECT `_uuid`, `_ingest_time`, `_source_time`, `id`, `customerid`, `time`, `entries`, TOJSON('{"int": 1, "string": "str", "array": [0,1,2], "nested":{"key":"value"}}') AS `json`
FROM `orders$1`;

EXECUTE STATEMENT SET BEGIN
INSERT INTO `orders$2$1`
(SELECT *
FROM `root$1`)
;
END;