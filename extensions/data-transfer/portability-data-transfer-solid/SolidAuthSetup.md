# Solid Auth Setup

To authenticate to solid for development, you will have to:
 1) Create a client certificate for 
    [WebID-TLS](https://dvcs.w3.org/hg/WebID/raw-file/tip/spec/tls-respec.html) login
 1) Add that key to your profile in Solid
 1) Use that key when running DTP

## Creating Client Cert
For this we'll use [OpenSSL](https://www.openssl.org/) to generate the certificate.

The most important part of the certificate is that you need to include a Subject Alternative Name
with a URI pointing to your profile in the certificate.

The first thing to do is create a configuration file to tell openssl that you want to define a
Subject Alternative Name.  It should look something like:

````
[ req ]
default_bits        = 2048
default_keyfile     = server-key.pem
distinguished_name  = subject
req_extensions      = req_ext
x509_extensions     = x509_ext
string_mask         = utf8only

[ subject ]
countryName         = Country Name (2 letter code)

stateOrProvinceName     = State or Province Name (full name)

localityName            = Locality Name (eg, city)

organizationName         = Organization Name (eg, company)

commonName          = Common Name (e.g. server FQDN or YOUR name)

emailAddress            = Email Address

# Section x509_ext is used when generating a self-signed certificate. I.e., openssl req -x509 ...
[ x509_ext ]

subjectKeyIdentifier        = hash
authorityKeyIdentifier  = keyid,issuer


basicConstraints        = CA:FALSE
keyUsage            = digitalSignature, keyEncipherment
subjectAltName          = @alternate_names
nsComment           = "OpenSSL Generated Certificate"


[ req_ext ]

subjectKeyIdentifier        = hash

basicConstraints        = CA:FALSE
keyUsage            = digitalSignature, keyEncipherment
subjectAltName          = @alternate_names
nsComment           = "OpenSSL Generated Certificate"


[ alternate_names ]
# Customize this
URI   = "https://example.inrupt.net/profile/card#me"
````

Next run (assuming your config file is named san.cnf):

`openssl req -config san.cnf -new -x509 -sha256 -newkey rsa:2048 -nodes -keyout inrupt.key.pem -days 365 -out inrupt.cert.pem`

Then run, this will ask for a password, remember it:
 
 `openssl pkcs12 -export -clcerts -in inrupt.cert.pem -inkey inrupt.key.pem -out client.p12 -nodes`
 

## Adding Client Cert to Solid

To add your key to your solid profile you will need to update your profile with the
modulus and exponent from your key.  You can get those by running:

`openssl rsa -noout -modulus -in inrupt.key.pem`

`openssl rsa -text -in inrupt.key.pem`

Then you need to make a post request to `https://example.inrupt.net/profile/card` with something
like:

 ````
 @prefix : <#>.
 @prefix solid: <http://www.w3.org/ns/solid/terms#>.
 @prefix pro: <./>.
 @prefix n0: <http://xmlns.com/foaf/0.1/>.
 @prefix n: <http://www.w3.org/2006/vcard/ns#>.
 @prefix schem: <http://schema.org/>.
 @prefix ldp: <http://www.w3.org/ns/ldp#>.
 @prefix inbox: </inbox/>.
 @prefix sp: <http://www.w3.org/ns/pim/space#>.
 @prefix foobar: </>.
 @prefix cert: <http://www.w3.org/ns/auth/cert#>.
 @prefix rdfs: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>.
 @prefix xsd: <http://www.w3.org/2001/XMLSchema#>. 
 
 pro:card a n0:PersonalProfileDocument; n0:maker :me; n0:primaryTopic :me.
 
 :id1543640031820 n:value <mailto:example@example.com>.
 
 :id1543640043040 n:value <tel:1234567>.
 
 :mycert
     a cert:RSAPublicKey;
     rdfs:label "inrupt.cert.pem";
     cert:modulus "MYMODULUSHERE"^^xsd:hexBinary;
     cert:exponent 65537.
 
 :me
     a schem:Person, n0:Person;
     n:hasEmail :id1543640031820;
     n:hasTelephone :id1543640043040;
     ldp:inbox inbox:;
     sp:preferencesFile </settings/prefs.ttl>;
     sp:storage foobar:;
     solid:account foobar:;
     solid:privateTypeIndex </settings/privateTypeIndex.ttl>;
     solid:publicTypeIndex </settings/publicTypeIndex.ttl>;
     n0:name "My Name";
     cert:key :mycert.
 ````

## Configuring DTP

So then you should be able to log in via your Client Cert, you should give a shot via importing
your cert into your browser and then logging into Solid, selecting the WebID-TLS option.

Currently we don't have Solid auth hooked up in production, but you can test it localy via
the [ManualTest](src/test/java/org/datatransferproject/transfer/solid/contacts/ManualTest.java)
by entering the path to your `client.p12` and password in that file.