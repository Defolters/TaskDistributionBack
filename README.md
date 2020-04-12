# TodoServer
This server uses authentication to allow multiple users to create accounts and store their todos.

There are several routes:
1. v1/users/create - This will create a new user. Pass in the email address, name and password
2. v1/users/login - This will login a user. Pass in email address, and password
3. v1/users/logout - This will login a user. Pass in email address
4. v1/users/delete - This will delete a user. Pass in email address

## Todos
There are several routes:
1. POST v1/todos - This will create a new todo. Pass in the todo string and done (true/false)
2. GET v1/todos - This will get a list of todos for the current user. Optional: Pass in limit and offset
3. DELETE v1/todos - This will delete a todo. Pass in todo id.

