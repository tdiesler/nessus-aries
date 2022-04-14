# Aries CLI

## Install

Aries CLi can be installed using one of [these methods](https://github.com/animo/aries-cli#installation).

## Setup the initial ENDORSER wallet

These initial setup instructions assume that you have the VON-Network and a multitenant ACA-Py instance running.
This is documented in the main [README](README.md)

```
./aries-wallet --create-all
```

## CLI Wallet Environments

How to create configuration environments is documented [here](https://docs.aries-cli.animo.id/guides/configuration)

The above will create respective wallet environments for Government, Faber, Acme, Thrift and Alice

```
aries-cli configuration view
Configuration path: ".../.config/aries-cli/config.yaml"
---
configurations:
  Acme:
    endpoint: "http://localhost:8031"
    api_key: adminkey
    auth_token: eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ3YWxsZXRfaWQiOiJmYzcxNGJhMS04OGJhLTRlMDYtOGEzMi1lZDNjZDM4MDY1MWEifQ.O6oU1rB4svaASIcX7eT9269v01NuSAUVKPnWaOCN7Ds
  Alice:
    endpoint: "http://localhost:8031"
    api_key: adminkey
    auth_token: eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ3YWxsZXRfaWQiOiIyZGFhYTg3NS05MmVmLTRiMjMtOTNmNi0zZjQ5YWRhNTMyZjIifQ.eahnPl_ypikkDp3geZusqiUWYhJw7F6ZnQEK03RQ9jE
  Faber:
    endpoint: "http://localhost:8031"
    api_key: adminkey
    auth_token: eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ3YWxsZXRfaWQiOiJjMDI2OWIzYy04OGI4LTQwM2UtOWU2MS03OWFjOTM4NDE4MzQifQ.BPxwxVGBg-jzpO2bM2YvP2i8BPjO4FDlbwifsMAT108
  Government:
    endpoint: "http://localhost:8031"
    api_key: adminkey
    auth_token: eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ3YWxsZXRfaWQiOiI3MDQ2Mzc2Zi04OWMyLTRhZjItYTMyZS1lYjBhNjhlZDczMzYifQ.7PYTVXLr1dnkVun7bWCJlXlNeEbg_mL483IVw9o0EvI
  Thrift:
    endpoint: "http://localhost:8031"
    api_key: adminkey
    auth_token: eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ3YWxsZXRfaWQiOiI0NWJlNzE0OS05MGMzLTQ3MzEtYjhlMC0zNTk0MzY2YzhlY2EifQ.rJW7VpWb4dtU_3-MItPgmvufw0mnYNg2zmGRikYbX-g
  default:
    endpoint: "http://localhost:8031"
    api_key: adminkey
    auth_token: eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ3YWxsZXRfaWQiOiIyZGFhYTg3NS05MmVmLTRiMjMtOTNmNi0zZjQ5YWRhNTMyZjIifQ.eahnPl_ypikkDp3geZusqiUWYhJw7F6ZnQEK03RQ9jE
```

The default environment can be switched like this ...

```
./aries-wallet --switch Alice
```

A wallet can be recreated like this ...

```
./aries-wallet --remove Alice
./aries-wallet --create Alice
```

All wallets can be recreated like this

```
./aries-wallet --remove-all
./aries-wallet --create-all
```

## Alice invites Faber

```
url=$(aries-cli -e Alice connection invite --auto-accept | sed -n 2p)
aries-cli -e Faber connection receive --url ${url}
```
