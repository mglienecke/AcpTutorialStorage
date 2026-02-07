--liquibase formatted sql

--changeset acp:002
create SCHEMA IF NOT EXISTS acp;

CREATE TABLE IF NOT EXISTS acp.demo2 (
    id int primary key,
    name varchar(100) not null
);
