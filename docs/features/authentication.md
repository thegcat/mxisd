# Authentication
- [Description](#description)
- [Basic](#basic)
  - [Overview](#overview)
  - [synapse](#synapse)
  - [mxisd](#mxisd)
  - [Validate](#validate)
  - [Next steps](#next-steps)
    - [Profile auto-fil](#profile-auto-fill)
- [Advanced](#advanced)
  - [Overview](#overview-1)
  - [Requirements](#requirements)
  - [Configuration](#configuration)
    - [Reverse Proxy](#reverse-proxy)
      - [Apache2](#apache2)
    - [DNS Overwrite](#dns-overwrite)

## Description
Authentication is an enhanced feature of mxisd to ensure coherent and centralized identity management.  
It allows to use Identity stores configured in mxisd to authenticate users on your Homeserver.

Authentication is divided into two parts:
- [Basic](#basic): authenticate with a regular username.
- [Advanced](#advanced): same as basic with extra ability to authenticate using a 3PID.

## Basic
Authentication by username is possible by linking synapse and mxisd together using a specific module for synapse, also
known as password provider.

### Overview
An overview of the Basic Authentication process:
```
                                                                                    Identity stores
 Client                                                                             +------+
   |                                            +-------------------------+    +--> | LDAP |
   |   +---------------+  /_matrix/identity     | mxisd                   |    |    +------+
   +-> | Reverse proxy | >------------------+   |                         |    |
       +--|------------+                    |   |                         |    |    +--------+
          |                                 +-----> Check ID stores     >------+--> | SQL DB |
     Login request                          |   |                         |    |    +--------+
          |                                 |   |     |                   |    |
          |   +--------------------------+  |   +-----|-------------------+    +-->  ...
          +-> | Homeserver               |  |         |
              |                          |  |         |
              | - Validate credentials >----+         |
              |   Using REST auth module |            |
              |                          |            |
              | - Auto-provision <-------------------<+
              |   user profiles          |    If valid credentials and supported by Identity store(s)
              +--------------------------+
```
Performed on [synapse with REST auth module](https://github.com/kamax-io/matrix-synapse-rest-auth/blob/master/README.md)

### Synapse
- Install the [password provider](https://github.com/kamax-io/matrix-synapse-rest-auth)
- Edit your **synapse** configuration:
  - As described by the auth module documentation
  - Set `endpoint` to `http://mxisdAddress:8090` - Replace `mxisdAddress` by an IP/host name that provides a direct
  connection to mxisd.  
  This **MUST NOT** be a public address, and SHOULD NOT go through a reverse proxy.
- Restart synapse

### mxisd
- Configure and enable at least one [Identity store](../stores/README.md)
- Restart mxisd

### Validate
Login on the Homeserver using credentials present in one of your Identity stores.

## Next steps
### Profile auto-fill
Auto-filling user profile depends on its support by your configured Identity stores.  
See your Identity store [documentation](../stores/README.md) on how to enable the feature.


## Advanced
The Authentication feature allows users to login to their Homeserver by using their 3PIDs in a configured Identity store.

### Overview
This is performed by intercepting the Homeserver endpoint `/_matrix/client/r0/login` as depicted below:
```
            +----------------------------+
            |  Reverse Proxy             |
            |                            |
            |                            |     Step 1    +---------------------------+     Step 2
            |                            |               |                           |
Client+---->| /_matrix/client/r0/login +---------------->|                           | Look up address  +---------+
            |                      ^     |               |  mxisd - Identity server  +----------------->| Backend |
            |                      |     |               |                           |                  +---------+
            | /_matrix/* +--+      +---------------------+                           |
            |               |            |               +---------------+-----------+
            |               |            |     Step 4                    |
            |               |            |                               | Step 3
            +---------------|------------+                               |
                            |                                            | /_matrix/client/r0/login
                            |                       +--------------+     |
                            |                       |              |     |
                            +---------------------->|  Homeserver  |<----+
                                                    |              |
                                                    +--------------+

```

Steps of user authentication using a 3PID:
1. The intercepted login request is directly sent to mxisd instead of the Homeserver.
2. Identity stores are queried for a matching user identity in order to modify the request to use the user name.
3. The Homeserver, from which the request was intercepted, is queried using the request at previous step.
   Its address is resolved using the DNS Overwrite feature to reach its internal address on a non-encrypted port.
4. The response from the Homeserver is sent back to the client, believing it was the HS which directly answered.

### Requirements
- [Basic Authentication configured and working](#basic)
- Reverse proxy setup
- Homeserver
- Compatible [Identity store](../stores/README.md)

### Configuration
#### Reverse Proxy
##### Apache2
The specific configuration to put under the relevant `VirtualHost`:
```apache
ProxyPass /_matrix/client/r0/login http://localhost:8090/_matrix/client/r0/login
```
`ProxyPreserveHost` or equivalent **must** be enabled to detect to which Homeserver mxisd should talk to when building results.

Your VirtualHost should now look similar to:
```apache
<VirtualHost *:443>
    ServerName example.org
    
    ...
    
    ProxyPreserveHost on
    ProxyPass /_matrix/client/r0/login http://localhost:8090/_matrix/client/r0/login
    ProxyPass /_matrix/identity http://localhost:8090/_matrix/identity
    ProxyPass /_matrix http://localhost:8008/_matrix
</VirtualHost>
```

#### DNS Overwrite
Just like you need to configure a reverse proxy to send client requests to mxisd, you also need to configure mxisd with
the internal IP of the Homeserver so it can talk to it directly to integrate its directory search.

To do so, put the following configuration in your mxisd configuration:
```yaml
dns.overwrite.homeserver.client:
  - name: 'example.org'
    value: 'http://localhost:8008'
```
`name` must be the hostname of the URL that clients use when connecting to the Homeserver.
In case the hostname is the same as your Matrix domain, you can use `${server.name}` to auto-populate the `value`
using the `matrix.domain` configuration option and avoid duplicating it.

`value` is the base internal URL of the Homeserver, without any `/_matrix/..` or trailing `/`.
