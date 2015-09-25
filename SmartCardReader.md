# Requirements for eID Smart Card Reader with secure display and secure pinpad #

## Introduction ##

This specification document describes a firmware for a new type of eID smart card reader that allows for explicit confirmation of certain eID transactions using the secure display and secure pinpad of the smart card reader.


## Specifications ##

The firmware of the smartcard reader needs to identify whether the card inserted in the reader is a Belgian eID card.

If a Belgian eID card is used, the smartcard reader needs to analyze the APDUs received for the card and act as specified below for these specific commands. All other commands addressed to the smartcard should be forwarded to the smartcard:

  1. If a 'prepare signature' command is sent to the card, the reader needs to set a flag indicating whether an authentication or a non-repudiation signature will be requested further on.  Other signature preparation commands uninitialize this flag.
  1. If a 'verify PIN' command is sent to the card, and the flag of step 1 indicates that an authentication signature is going to be requested, the reader should display on its screen a message indicating that the citizen has to enter his/her PIN for authentication purposes. The citizen enters his/her PIN on the smartcard reader. Note that the text displayed on the reader’s display should be specified by the reader, not by the application sending APDUs to the smartcard.
  1. If a 'verify PIN' command is sent to the card, and the flag of step 1 indicates that a non-repudiation signature will be requested, the reader should display on its screen a message indicating that the citizen has to enter his/her PIN for non-repudiation purposes. The citizen enters his/her PIN on the smartcard reader. Note that the text displayed on the reader’s display should be specified by the reader, not by the application sending APDUs to the smartcard!
  1. If a 'generate signature' command using the non-repudiation key is sent to the card, and the flag of step 1 indicates that a non-repudiation signature was being prepared, the reader displays the information that will be signed. The citizen enters the confirmation button on the smartcard reader if it is ok to compute the signature. The smartcard reader determines the format of the information that should be displayed on its display using the OID embedded in the `generate signature’ command. The information that will be signed is displayed in text form if the OID in the command indicates that a text will be signed and in hex form for all other cases. E.g., if the command specifies that a SHA-1 hash is to be signed, the hash value will be displayed on the reader; if the command specifies a text "App-Name, 14-feb-2010,  4:42", then this text will be displayed on the reader.
  1. If a 'generate signature' command using the authentication key is sent to the card, and the flag of step 1 indicates that an authentication signature was being prepared, the reader displays the information that will be signed only if the specific OID is present. Normal authentication signatures (most likely in the context of SSL handshakes) should not be interrupted by the smart card reader.

Commands other than those specified above do not get filtered or processed by the smartcard reader.

The OID that specifies a text form will be specified by the Administration.