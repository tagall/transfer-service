# transfer-service

to create executable jar run 'mvn package' (-clean if needed to rebuild)

to run transfer service run 'java -jar transfer-service-1.0-SNAPSHOT.jar'

applications will start on port 4567

basic REST methods supported:
- POST createAccount with body {"name":"Jon","balance":"100"}
- GET getAccount/$id with needed id
- GET getAllAccounts - returns list of all accounts
- POST performExchange with body {"from":{"id":1,"name":"From","balance":100},"to":{"id":2,"name":"To","balance":100},"amount":35}
