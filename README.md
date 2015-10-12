# restodoservice

Restodo service provides rest api for managing todo notes. 

### API

* POST /users

Handles registering users. Expects the body of the request to be filled with 3
params: email, password and verification url. Creates an entry in db and sends a mail to the user for verification
verification token will be appended to the verification url in the mail. Client application is expected to 
redirect user verification to PATCH /users/:email with verification-token value in the patch body.


* PATCH /users/:email

Verifies user email, thus finishing registration process.

* POST /login

Attempts login using email and password. returns authentication token.
If login fails, authentication token will be null. 200 is returned in either case.

* POST /todos

Service for creating a new to-do. Must have authorization token in the header and it determines
a user for whom todo will be added. Body of the request must contain description of todo and a score,
which determines a priority by wich todo will be regarded. Lower score, lower priority

* GET /todos

Retrieves all todos for a user starting with score 0 and ending with max-score provided as query param.
Must have authorization token in the header and it determines a user for whom todos will be retrieved. 
Returns json in a form description : score. e.g. 
{"do something" : 5, "do something less important" : 10}

* GET /todos/first
 
Retrieves todo with highest priority. Must have authorization token in the header and it determines a user for whom todos will be retrieved.

* DELETE /todos

Deletes todo with the highest priority and returns todo with the next highest priority. Must have authorization token in the header and it determines a user for whom todos will be retrieved.

# Building restodoservice

* Add config.edn to the root of the project with the following data:

```
{
  :app-port _application-port_ 
  :app-host _application_host_with_port_
  :redis-host _redis-host_ 
  :redis-port _redis-port_
  :salt _salt-for-password-hash-gen_ 
  :mail-host _mail-host_
  :mail-username _mail-username_
  :mail-password _mail-password_
  }
```

* lein run

# Testing restodoservice

If you are using postman, in the project root you may find our sample collection: restodo.json.postman_collection