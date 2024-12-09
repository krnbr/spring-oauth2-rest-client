The scenarios covered by this POC are listed in the table below, which are backed by the mock server part of the repo [here](https://github.com/krnbr/mocks):-

<style>
    .scenarios {
        color-scheme: only light;
        color: black;
        width: 70%;
        text-align: center;
    }
    .scenarios th {
        background-color: #DCDCDC;
        word-wrap: break-word;
        text-align: center;
    }
    .scenarios tr:nth-child(even) {background: #CCC}
    .scenarios tr:nth-child(odd) {background: #FFF;}
    .scenarios tr:nth-child(odd) td {height: 1px}
</style>

<div class="scenarios">

| Scenario | upstream endpoint | Token Endpoint | Resource Endpoint | Downstream Token Endpoint              | Downstream Resource Endpoint |
|----------|-------------------|----------------|-------------------|----------------------------------------|------------------------------|
| 1        | /ping             | tls            | tls               | https://localhost:8453/oauth2/v2/token | https://localhost:8453/ping  |
|          |                   |                |                   |                                        |                              |
| 2        | /v1/ping          | mtls           | tls               | https://localhost:8443/oauth2/v2/token | https://localhost:8453/ping  |
|          |                   |                |                   |                                        |                              |
| 3        | /v2/ping          | tls            | mtls              | https://localhost:8453/oauth2/v2/token | https://localhost:8443/ping  |
|          |                   |                |                   |                                        |                              |
| 4        | /v3/ping          | mtls           | mtls              | https://localhost:8443/oauth2/v2/token | https://localhost:8443/ping  |
|          |                   |                |                   |                                        |                              |

</div>
