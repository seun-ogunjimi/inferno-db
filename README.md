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
    - `server-port` - The port on which the server will run. Default is 9090.
    - `max-file-size` - The maximum size of a file before a rollover. Default is 10,485,760 bytes. (10M)
    - `max-memory-size` - The maximum size of the in-memory buffer per write/read. Default is 1024 bytes. (1KB)
    - `compaction-threshold` - The number of files(max-file-size) after which a compaction will be triggered. Default is

## Usage

### Running the Server

To start the Inferno DB server, use the following command:

```sh
java -jar target/inferno-db-1.0.0.jar

```

### REST API

Inferno DB provides a REST API for interacting with the database. The following endpoints are available:
Base URL: `http://localhost:9090`
Default port is 9090, you can change the port by passing the `server-port` commandline variable.
Defaut bucket is `root`, you can create a new bucket by passing the `bucket` path parameter.

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

### Configuration

Inferno DB can be configured using commandline variables. The following variables are available:

- `server-port` - The port on which the server will run. Default is 9090.
- `max-file-size` - The maximum size of a file before a rollover. Default is 10,485,760 bytes. (10M)
- `max-memory-size` - The maximum size of the in-memory buffer per write/read. Default is 1024 bytes. (1KB)
- `compaction-threshold` - The number of files(max-file-size) after which a compaction will be triggered. Default is 2
- `bucket` - The bucket to use. Default is `root`

### Design

### Used: Language and Framework

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
-

### License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.





  

