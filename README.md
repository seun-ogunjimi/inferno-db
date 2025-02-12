# Inferno DB

----

Inferno DB is a key-value store that provides a REST API for interacting with the database. It is designed to be fast,
scalable, and efficient. Inferno DB uses an in-memory cache/indexing for faster read operations and data partitioning
for scalability. It also supports data compaction for efficient storage and configurable parameters for tuning
performance.

### Features

1. Key-Value store
2. REST API for interacting with the database
3. In-memory cache/indexing for faster read operations
4. Data partitioning for scalability
5. Data compaction for efficient storage
6. Configurable parameters for tuning performance via commandline variables
7. Support for multiple buckets
8. Multi-threaded server for handling concurrent requests
9. Asynchronous reads for faster response times
10. Synchronous writes for data consistency
11. CRC32 checksum for data integrity

## Installation

### Prerequisites

- Java 21 or higher
- Maven

### Steps

1. Clone the repository:
    ```sh
    git clone https://github.com/{user}/inferno-db.git
    cd inferno-db
    ```

2. Build the project:
    ```sh
    mvn clean install
    ```

3. Run the application:
    ```sh
    java -jar target/inferno-db-1.0.0.jar
    ```
    - Optional
      commandline variables can be passed to the application like so:
    ```sh
    java -jar target/inferno-db-1.0.0.jar --server-port=9090 --max-file-size=1024 --max-memory-size=1024 --compaction-threshold=2
    ```
    - The above command will start the server on port 9090 with a max file size of 1KB, max memory size of 1KB, and a
      compaction threshold of 2.

### Configuration

Inferno DB can be configured using commandline variables. The following variables are available:

- `server-port` - The port on which the server will run. Default is 9090.
- `max-file-size` - The maximum size of a file before a rollover. Default is 10,485,760 bytes. (10M)
- `max-memory-size` - The maximum size of the in-memory buffer per write/read. Default is 1024 bytes. (1KB)
- `compaction-threshold` - The number of files(max-file-size) after which a compaction will be triggered. Default is 2

## Usage

### Running the Server

To start the Inferno DB server, use the following command:

```sh
java -jar target/inferno-db-1.0.0.jar

```

### REST API

Inferno DB provides a REST API for interacting with the database. The following endpoints are available:
>>> Base URL: `http://localhost:9090`

- `GET /inferno/{bucket}/{key}` - Get the value for the specified key
  Response:
  ```json
      {
         "key": "hello",
         "value": "Hello, World!"
      }
    ```

- `POST /inferno/{bucket}/{key}` - Create or update the value for the specified key (Remember to repeat the key in the
  body)
  Request:
    ```json
        {
          "key": "hello",
          "value": "Hello, World!"
        }
    ```
  Response: 200 OK
- `DELETE /inferno/{bucket}/{key}` - Delete the value for the specified key
  Response:
  ```json
      {
         "key": "hello",
         "value": "Hello, World!"
      }
    ```
  Response: 204 No Content
- `GET /inferno/{bucket}/key?startKey={key1}&endKey={key2}` - Get all the key-value pair in the range of startKey and
  endKey
  Response:
    ```json
        [
             {
             "key": "hello",
             "value": "Hello, World!"
             },
             {
             "key": "world",
             "value": "World, Hello!"
             }
        ]
    ```
  Response: 200 OK
- `PUT /inferno/{bucket}/key` - Bulk creation or update of key-value pairs
  Request:
    ```json
        [
            {
                "key": "hello",
                "value": "Hello, World!"
            },
            {
                "key": "world",
                "value": "World, Hello!"
            }
        ]
    ```
  Response: 200 OK

- ERROR RESPONSES
  - 400 Bad Request - Invalid request
  - 404 Not Found - Key not found
  - 500 Internal Server Error - Server error
  Response:
  ```json
      {
        "timestamp": "1739382788546",
        "status": "404",
        "message": "Key not found"
      }
    ```
- Example:
  ```sh
  curl -X PUT http://localhost:9090/inferno/root/hello -d '{"key": "hello", "value": "Hello, World!"}'
  ```

>>>NOTE: Always ensure that you have a well-formed JSON body when making requests to the API. I wrote a custom JSON parser, so it is very strict and may not do well with special characters or invalid JSON.


#### About Buckets
- Default bucket is `root`, you can create a new bucket by passing the `bucket` path parameter.
- `{bucket}` is the name of the bucket you want to interact with. It is optional and defaults to `root`.
- If you want to interact with the default bucket, you can omit the `{bucket}` path parameter.
- If specified, the `{bucket}` path parameter must be alphanumeric and can contain hyphens.
- It is case-insensitive and must be URL encoded.
- It is created if it does not exist.


### Used: Language

1. Java
2. Maven

#### Test Dependencies

1. JUnit
2. Mockito

### Wishlist

- [ ] Add support for second level compaction
- [ ] Add support for data replication
- [ ] Add support for data sharding
- [ ] Add support for high availability and fault tolerance (using Raft consensus algorithm)
- [ ] Add support for data querying pagination


### License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.





  

