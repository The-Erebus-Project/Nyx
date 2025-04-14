# Erebus - mTLS certificates
To enable mTLS auth, you need to create/source respective certificates.
For this example - we will create self-signed certs for **localhost** and **127.0.0.1** addresses that will last for 3650 days (~10 years)

## TLS Certificates chain creation
### Requirements
- OpenSSL
```bash
sudo apt-get install libssl-dev
```

### Process
#### Root CA certificate and key creation
1. Generate Root CA Private Key
```bash
openssl genrsa -out ca-key.pem 4096
```
2. Generate Root CA Certificate
```bash
openssl req -new -x509 -nodes -days 3650 \
   -key ca-key.pem \
   -out ca-cert.pem
```

#### Server Certificate and Key creation
1. Generate Server Private Key and Server CSR
```bash
openssl req -newkey rsa:4096 -nodes -days 3650 \
   -keyout server-key.pem \
   -out server-req.pem
```
2. Server Certificate Creation and Signing using CA Key
```bash
openssl x509 -req -days 3650 -set_serial 01 \
   -in server-req.pem \
   -out server-cert.pem \
   -CA ca-cert.pem \
   -CAkey ca-key.pem \
   -extensions SAN   \
   -extfile <(printf "\n[SAN]\nsubjectAltName=DNS:localhost,IP:127.0.0.1\nextendedKeyUsage=serverAuth")
```
**NOTE:** Adjust **subjectAltName** to allow additional hosts (in case above, we allow local debugging. Make sure to add deployment URL once it's ready)

#### Client Certificate and Key creation
1. Generate Client Private Key and Client CSR
```bash
openssl req -newkey rsa:4096 -nodes -days 3650 \
   -keyout client-key.pem \
   -out client-req.pem
```
2. Client Certificate Creation and Signing using CA Key
```bash
openssl x509 -req -days 3650 -set_serial 01  \
   -in client-req.pem  \
   -out client-cert.pem  \
   -CA ca-cert.pem   \
   -CAkey ca-key.pem   \
   -extensions SAN  \
   -extfile <(printf "\n[SAN]\nsubjectAltName=DNS:localhost,IP:127.0.0.1\nextendedKeyUsage=clientAuth")
```
**NOTE:** Adjust **subjectAltName** to allow additional hosts (in case above, we allow local debugging. Make sure to add deployment URL once it's ready)