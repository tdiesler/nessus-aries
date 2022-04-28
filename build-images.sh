#!/bin/bash

# exit when any command fails
set -e

PWD=$(pwd)
TARGET_DIR="${PWD}/target"

if [[ -z ${ACAPY_VERSION} ]]; then
    #ACAPY_VERSION="0.7.3"
    ACAPY_VERSION="dev"
fi

if [[ -z ${TAILS_SERVER_VERSION} ]]; then
    TAILS_SERVER_VERSION="1.0.0"
fi

if [[ -z ${VON_NETWORK_VERSION} ]]; then
    VON_NETWORK_VERSION="1.7.2"
fi

function buildAcaPyImage () {
    push=$1
    
    imageName="nessusio/aries-cloudagent-python"
    fullName="${imageName}:${ACAPY_VERSION}"
    
    if [[ ! -d ${TARGET_DIR}/aries-cloudagent-python ]]; then
        git clone https://github.com/hyperledger/aries-cloudagent-python ${TARGET_DIR}/aries-cloudagent-python
        cd ${TARGET_DIR}/aries-cloudagent-python
    else 
        cd ${TARGET_DIR}/aries-cloudagent-python
        git fetch origin --tags
    fi
    
    if [[ ${ACAPY_VERSION} != "dev" ]]; then
        git checkout "${ACAPY_VERSION}"
    else
        cd ${HOME}/git/aries-cloudagent-python
        # git checkout main
        # git pull origin main
    fi
    
    echo "Building ${fullName} ..."
    docker build -t ${fullName} -f ./docker/Dockerfile.run .
    docker tag ${fullName} "${imageName}:latest"
    
    if [[ ${push} ]]; then
        docker push "${imageName}:latest"
        docker push "${fullName}"
    fi
}

function buildVonNetworkImage () {
    push=$1
    
    imageName="nessusio/von-network"
    fullName="${imageName}:${VON_NETWORK_VERSION}"
    
    if [[ ! -d ${TARGET_DIR}/von-network ]]; then
        git clone https://github.com/bcgov/von-network ${TARGET_DIR}/von-network
        cd "${TARGET_DIR}/von-network"
    else 
        cd "${TARGET_DIR}/von-network"
        git fetch origin --tags
    fi
    
    git checkout "v${VON_NETWORK_VERSION}"
    
    echo "Building ${fullName} ..."
    docker build -t ${fullName} .
    docker tag ${fullName} "${imageName}:latest"

    if [[ ${push} ]]; then
        docker push "${imageName}:latest"
        docker push "${fullName}"
    fi
}

function buildTailsServerImage () {
    push=$1
    
    imageName="nessusio/indy-tails-server"
    fullName="${imageName}:${TAILS_SERVER_VERSION}"
    
    if [[ ! -d ${TARGET_DIR}/indy-tails-server ]]; then
        git clone https://github.com/bcgov/indy-tails-server ${TARGET_DIR}/indy-tails-server
        cd "${TARGET_DIR}/indy-tails-server"
    else 
        cd "${TARGET_DIR}/indy-tails-server"
        git fetch origin --tags
    fi
    
    git checkout "v${TAILS_SERVER_VERSION}"
    
    echo "Building ${fullName} ..."
    docker build -t ${fullName} -f ./docker/Dockerfile.tails-server .
    docker tag ${fullName} "${imageName}:latest"
    
    if [[ ${push} ]]; then
        docker push "${imageName}:latest"
        docker push "${fullName}"
    fi
}

function buildImage () {
    shortName=$1
    push=$2

    mkdir -p ${TARGET_DIR}
    
    if [[ $shortName == "von-network" ]]; then
        buildVonNetworkImage ${push}

    elif [[ $shortName == "tails-server" ]]; then
        buildTailsServerImage ${push}

    elif [[ $shortName == "acapy" ]]; then
        buildAcaPyImage ${push}
    fi
}

if (( $# < 1 )); then
    echo "[Error] Illegal number of arguments."
    echo "Usage: $0 [all|von-network|tails-server|acapy] [push]"
    exit 1
fi

shortName=$1
push=$2

if [[ $shortName == "all" ]]; then
  buildImage "acapy" $push
  buildImage "von-network" $push
  buildImage "tails-server" $push

else
  buildImage $shortName $push
fi
