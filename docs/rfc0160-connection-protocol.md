# RFC 0160: Connection Protocol with multitenant wallets
  
https://github.com/hyperledger/aries-rfcs/tree/main/features/0160-connection-protocol

## Faber creates an invitation (/connections/create-invitation)

```
faber.connectionsCreateInvitation(
        CreateInvitationRequest.builder()
            .myLabel("Faber/Alice")
            .build(), 
        CreateInvitationParams.builder()
            .autoAccept(true)
            .build()).get();
            
Request
-------
http://localhost:8031/connections/create-invitation?auto_accept=true
{
  "my_label": "Faber/Alice"
}

Response
--------
{
  "connection_id": "56c62517-8d2a-40c3-a02e-d3571bf39f12",
  "invitation": {
    "@type": "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/connections/1.0/invitation",
    "@id": "c5f466d0-f5a4-405f-a7a8-d844ea885616",
    "recipientKeys": [
      "4RrW8NKu2d88ArCmiuqBFSe84yjnjDgsfYEdAxYr999S"
    ],
    "serviceEndpoint": "http://localhost:8030",
    "label": "Faber/Alice"
  },
  "invitation_url": "http://localhost:8030?c_i\u003deyJAdHlwZSI6...saWNlIn0\u003d"
}
```

Note, all WebSocket listeners receive the connection INVITATION event. 

```
Alice Connection: [@Faber] [INVITATION] ConnectionRecord(..., theirRole=INVITEE)
Acme Connection:  [@Faber] [INVITATION] ConnectionRecord(..., theirRole=INVITEE)
Faber Connection: [@Faber] [INVITATION] ConnectionRecord(..., theirRole=INVITEE)
```

## Alice receives the invitation from Faber (/connections/receive-invitation)

```
alice.connectionsReceiveInvitation(ReceiveInvitationRequest.builder()
        .recipientKeys(invitation.getRecipientKeys())
        .serviceEndpoint(invitation.getServiceEndpoint())
        .build(), ConnectionReceiveInvitationFilter.builder()
            .autoAccept(true)
            .build()).get();

Request
-------
http://localhost:8031/connections/receive-invitation?auto_accept=true
{
  "recipientKeys": [
    "91SPaaEyhv4mXaNts28xBDcCbGmEcDc69SdwD95jfoEJ"
  ],
  "serviceEndpoint": "http://localhost:8030"
}

Response
--------
{
  "invitation_key": "91SPaaEyhv4mXaNts28xBDcCbGmEcDc69SdwD95jfoEJ",
  "invitation_msg_id": "33160607-a119-4100-8005-725567c910f9",
  "invitation_mode": "once",
  "state": "request",
  "updated_at": "2022-04-16T08:37:05.373806Z",
  "my_did": "6f8wtpPMPAMe43uEnFyYSm",
  "their_role": "inviter",
  "created_at": "2022-04-16T08:37:05.337975Z",
  "rfc23_state": "request-sent",
  "request_id": "3aec6c4e-e0cc-4110-98f7-d6154d018c80",
  "accept": "auto",
  "connection_protocol": "connections/1.0",
  "routing_state": "none",
  "connection_id": "68239d73-d808-485c-83ef-1b0cf6a84541"
}
```

This is followed by these events to all WebSocket listeners

```
Acme Connection:  [@Alice] [INVITATION] ConnectionRecord(..., theirRole=INVITER)
Faber Connection: [@Alice] [INVITATION] ConnectionRecord(..., theirRole=INVITER)
Alice Connection: [@Alice] [INVITATION] ConnectionRecord(..., theirRole=INVITER)

Alice Connection: [@Alice] [REQUEST] ConnectionRecord(..., theirRole=INVITER)
Acme Connection:  [@Alice] [REQUEST] ConnectionRecord(..., theirRole=INVITER)
Faber Connection: [@Alice] [REQUEST] ConnectionRecord(..., theirRole=INVITER)

Acme Connection:  [@Faber] [REQUEST] ConnectionRecord(..., theirRole=INVITEE)
Faber Connection: [@Faber] [REQUEST] ConnectionRecord(..., theirRole=INVITEE)
Alice Connection: [@Faber] [REQUEST] ConnectionRecord(..., theirRole=INVITEE)

Acme Connection:  [@Faber] [RESPONSE] ConnectionRecord(..., theirRole=INVITEE)
Faber Connection: [@Faber] [RESPONSE] ConnectionRecord(..., theirRole=INVITEE)
Alice Connection: [@Faber] [RESPONSE] ConnectionRecord(..., theirRole=INVITEE)

Acme Connection:  [@Alice] [RESPONSE] ConnectionRecord(..., theirRole=INVITEE)
Faber Connection: [@Alice] [RESPONSE] ConnectionRecord(..., theirRole=INVITEE)
Alice Connection: [@Alice] [RESPONSE] ConnectionRecord(..., theirRole=INVITEE)

Faber Connection: [@Faber] [ACTIVE] ConnectionRecord(..., theirRole=INVITEE)
Alice Connection: [@Faber] [ACTIVE] ConnectionRecord(..., theirRole=INVITEE)
Acme Connection:  [@Faber] [ACTIVE] ConnectionRecord(..., theirRole=INVITEE)

Acme Connection:  [@Alice] [ACTIVE] ConnectionRecord(..., theirRole=INVITER)
Alice Connection: [@Alice] [ACTIVE] ConnectionRecord(..., theirRole=INVITER)
Faber Connection: [@Alice] [ACTIVE] ConnectionRecord(..., theirRole=INVITER)
```