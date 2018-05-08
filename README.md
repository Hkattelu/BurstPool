# ChronoxPool #

## Dependencies ##

In order to start up this pool on a server you will need Java 8. The pool runs on scala so you
will also need to get sbt to fetch dependencies and start the server with jetty.

## Editing the Config file ##

The pool is configured by editing a file titled 'config.json' in the directory src/main/resources. It should be edited
to fit your needs for the pool. The attributes to edit are as follows:

- POOL_FEE: The % to charge users for the pool 
- CURRENT_BLOCK_SHARE: The % of the block reward given to miners who submitted nonces for the current block
- HISTORIC_BLOCK_SHARE: The % of the block reward given to miners who submitted nonces for the last MIN_HEIGHT_DIFF blocks
- BAN_TIME: The number of minutes to ban users who submit invalid nonces or invalid info
- PAY_TIME: The number of days between which to payout accumulating rewards
- ACCOUNT_ID: The ID of the account to which the pool funds should go to
- SECRET_PHRASE: The passphrase of the account to which the pool funds should go to 
- TARGET_DEADLINE: The minimum deadline for a submitted nonce to be considered "valid" and earn a share
- MIN_HEIGHT_DIFF: The number of past blocks for which historic shares are calculated
- DB_USER: The username to the database where data will be stored
- DB_PASS: The password to the database where data will be stored
- DB_HOST: The hostname to the database server where data will be stored
- DB_PORT: The port on which the database server is being hosted
- DB_NAME: The name of the database where data will be stored

## Testing ##

To test the database, edit the configuration file to whatever test data is needed, including the database info. Then 
run the following commands:

```sh
$ cd ChronoxPool
$ sbt
> test
```

## Build & Run ##

To start up the pool server, simply use jetty:start :

```sh
$ cd ChronoxPool
$ sbt
> jetty:start
> browse
```

If `browse` doesn't launch your browser, manually open [http://localhost:8124/](http://localhost:8124/) in your browser.
