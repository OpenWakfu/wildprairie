
CREATE TABLE account(
   id serial primary key,
   login text not null,
   hashed_password text not null,
   community int not null
);