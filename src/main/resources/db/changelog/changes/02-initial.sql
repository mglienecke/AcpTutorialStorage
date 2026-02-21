--liquibase formatted sql

--changeset ilp:002
ALTER TABLE ilp.drones ADD COLUMN description VARCHAR(255);