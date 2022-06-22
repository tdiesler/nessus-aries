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
* [Aries Cloud Agent](https://github.com/hyperledger/aries-ACA-Py-controllers/tree/main/AliceFaberAcmeDemo)
* [Aries Protocol RFCs](https://github.com/hyperledger/aries-rfcs/tree/main/features)

### Ledger with VON-Network

This project requires access to a Hyperledger Indy Network. Is is recommended to use the [VON Network](https://github.com/bcgov/von-network), developed as a portable Indy Node Network implementation for local development. Instructions for setting up the von-network can be viewed [here](https://github.com/bcgov/von-network#running-the-network-locally).

Basic instructions for using the VON Network are [here](https://github.com/bcgov/von-network/blob/main/docs/UsingVONNetwork.md).

### ACA-Py SSI Agent

Hyperledger [Aries Cloud Agent Python (ACA-Py)](https://github.com/hyperledger/aries-cloudagent-python) is a foundation for building decentralized identity applications and services.

#### Run ACA-Py in insecure multitenant mode

This gives access to the [Swagger UI](http://localhost:8031). 

Alternatively, use the [ModHeader Plugin](https://chrome.google.com/webstore/detail/modheader) to provide 
the value of `--admin-api-key` in as an `X-API-Key` header value - this is only needed for the first request.

```
ACA-Py_USER_PORT=8030
ACA-Py_ADMIN_PORT=8031
ACA-Py_HOST=localhost

docker run -it --rm nessus/aries-cloudagent-python start -h

# Run in single wallet mode
docker run -it --rm \
   --name ACA-Py \
   -p ${ACAPY_ADMIN_PORT}:${ACAPY_ADMIN_PORT}  \
   nessus/aries-cloudagent-python start \
      --genesis-url http://host.docker.internal:9000/genesis \
      --endpoint http://${ACAPY_HOST}:${ACAPY_USER_PORT} \
      --inbound-transport http 0.0.0.0 ${ACAPY_USER_PORT} \
      --outbound-transport http \
      --admin 0.0.0.0 ${ACAPY_ADMIN_PORT} \
      --admin-insecure-mode \
      --multitenant \
      --multitenant-admin \
      --jwt-secret jwtsecret \
      --log-level info
```

### Revocation with Tails Server

Verifiable Credentials (VC) are issued to the Credential Holder and (if the credential supports revocation) can be revoked again by the Issuer.
To support [Credential Revocation](https://github.com/hyperledger/aries-cloudagent-python/blob/main/docs/GettingStartedAriesDev/CredentialRevocation.md) we
need a running instance of a [Tails Server](https://github.com/bcgov/indy-tails-server).

For an ultra-high-level intro, you might consider watching [this introductory video](https://youtu.be/QsRu4ZqJpG4).


## Start VON-Network, ACA-Py and Tails Server 

Use this when you want to run the tests.

```
docker compose up --detach && docker compose logs -f acapy
```
