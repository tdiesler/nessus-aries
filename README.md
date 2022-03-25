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

#### Running the ACA-Py Agent

Hyperledger [Aries Cloud Agent Python (ACA-Py)](https://github.com/hyperledger/aries-cloudagent-python) is a foundation for building decentralized identity applications and services.

```
git clone https://github.com/hyperledger/aries-cloudagent-python
cd aries-cloudagent-python

export PORTS=8100:8200

./scripts/run_docker start \
  --genesis-url http://host.docker.internal:9000/genesis \
  --inbound-transport http 0.0.0.0 8100 \
  --outbound-transport http \
  --admin-insecure-mode \
  --admin 0.0.0.0 8200 \
  --endpoint http://localhost:8100
```
