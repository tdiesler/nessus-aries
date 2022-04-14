# Nessus Aries

With Nessus Aries we explore aspects of digital identy and verifiable credentials based on [Hyperledger Aries](https://www.hyperledger.org/use/aries).

This is a contribution to "Making The World Work Better For All".

Who is going to control our digitial identity? Digital forms of our birth certificate, passport, drivers license,
medical records, vaccination certificates, univeristy degrees, property certificates, etc.
Is it the state, a corporation or should we be in control ourselves?

<img src="docs/img/ssi-book.png" height="200">

Shouldn't we have in fact a [self sovereign identity](https://www.manning.com/books/self-sovereign-identity) (SSI)?

## External Documentation

* [The Story of Open SSI Standards](https://www.youtube.com/watch?v=RllH91rcFdE)
* [Hyperledger Aries Wiki](https://wiki.hyperledger.org/display/aries)
* [Aries Cloud Agent](https://github.com/hyperledger/aries-acapy-controllers/tree/main/AliceFaberAcmeDemo)

## Running the VON Network

This project requires access to a Hyperledger Indy Network. Is is recommended to use the [VON Network](https://github.com/bcgov/von-network), developed as a portable Indy Node Network implementation for local development. Instructions for setting up the von-network can be viewed [here](https://github.com/bcgov/von-network#running-the-network-locally).

Basic instructions for using the VON Network are [here](https://github.com/bcgov/von-network/blob/main/docs/UsingVONNetwork.md).

```
git clone https://github.com/bcgov/von-network
cd von-network

./manage build
./manage up --logs
```

### Running the ACAPy Agent

Hyperledger [Aries Cloud Agent Python (ACAPy)](https://github.com/hyperledger/aries-cloudagent-python) is a foundation for building decentralized identity applications and services.

### Build the ACAPy Docker image

```
git clone https://github.com/hyperledger/aries-cloudagent-python
cd aries-cloudagent-python

docker build -t nessus/aries-cloudagent -f ./docker/Dockerfile.run .
```

### Run ACAPy in single wallet mode

This also gives access to the [Swagger UI](http://localhost:8031)

```
ACAPY_USER_PORT=8030
ACAPY_ADMIN_PORT=8031
ACAPY_HOST=localhost

docker run -it --rm nessus/aries-cloudagent start help

# Run in single wallet mode
docker run -it --rm \
   --name acapy \
   -p ${ACAPY_USER_PORT}:${ACAPY_USER_PORT} \
   -p ${ACAPY_ADMIN_PORT}:${ACAPY_ADMIN_PORT}  \
   nessus/aries-cloudagent start \
      --genesis-url http://host.docker.internal:9000/genesis \
      --endpoint http://${ACAPY_HOST}:${ACAPY_USER_PORT} \
      --inbound-transport http 0.0.0.0 ${ACAPY_USER_PORT} \
      --outbound-transport http \
      --admin 0.0.0.0 ${ACAPY_ADMIN_PORT} \
      --admin-insecure-mode \
      --auto-provision \
      --seed 000000000000000000000000Trustee1 \
      --wallet-storage-type default \
      --wallet-key trusteewkey \
      --wallet-name trustee \
      --wallet-type indy \
      --recreate-wallet \
      --storage-type indy \
      --log-level info
```

### Run ACAPy in multitenant mode

Use this when you want to run the test cases

```
ACAPY_USER_PORT=8030
ACAPY_ADMIN_PORT=8031
ACAPY_HOST=localhost

# Run in multitenant mode
docker run -it --rm \
   --name acapy \
   -p ${ACAPY_USER_PORT}:${ACAPY_USER_PORT} \
   -p ${ACAPY_ADMIN_PORT}:${ACAPY_ADMIN_PORT}  \
   nessus/aries-cloudagent start \
      --genesis-url http://host.docker.internal:9000/genesis \
      --endpoint http://${ACAPY_HOST}:${ACAPY_USER_PORT} \
      --inbound-transport http 0.0.0.0 ${ACAPY_USER_PORT} \
      --outbound-transport http \
      --storage-type indy \
      --admin 0.0.0.0 ${ACAPY_ADMIN_PORT} \
      --admin-api-key adminkey \
      --multitenant \
      --multitenant-admin \
      --jwt-secret jwtsecret \
      --seed 000000000000000000000000Trustee1 \
      --wallet-storage-type default \
      --wallet-key trusteewkey \
      --wallet-name trustee \
      --wallet-type indy \
      --recreate-wallet \
      --auto-provision \
      --auto-ping-connection \
      --auto-accept-requests \
      --log-level info
```
