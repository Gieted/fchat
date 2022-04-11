# fchat

-----------

Fchat is a CLI chat application with support for channels and sending files.

## Installation

-----------
Pre-requirements:

- Java 17 installed
- `JAVA_HOME` set to the location of your Java installation

The installation procedure:

1. Download the latest release from [here](https://github.com/pawelkielb/fchat/releases/download/1.0.0/client.zip).
2. Unpack it to any folder you wish.
3. Add `bin/` folder to your `PATH`.
4. Restart the computer or restart explorer in the task manager.
5. Open a terminal and try using `fchat` command. If the installation was successful you should see a command list.

## Setup

-----------

Create a new directory and open a new terminal window there.

```bash
mkdir fchat 
cd fchat
```

Run `fchat init` command. This should create an `fchat.properties` file inside the folder.

```properties
#Sat Mar 19 17:58:52 CET 2022
server_host=localhost
server_port=1337
username=Guest
```

Now edit the configuration to your preferences. Save the file, your client is now ready to use. You can have multiple
clients in a separate directories.

## Commands

-----------

### fchat init

Creates a fchat.properties file in the current directory.

```
fchat init
```

### fchat create

Creates a new group channel. You can use `cd` command to enter the channel. Other users must use `fchat sync` to see the
channel.

```
fchat create [name] (usernames...)

e.g. fhcat create "Small Talks" ben steven
```

- name - a name of the channel
- usernames - 0 or more usernames of users who should be added to the channel

### fchat priv

Creates a new private channel. You can use `cd` command to enter the channel. The other user must use `fchat sync` to
see the channel.

```
fchat priv [username]

e.g. fchat priv christine
```

- username - a username of a user you want to chat with

### fchat sync

Updates the channels list.

```
fchat sync
```

### fchat send

Sends a message to a channel. To use the commands you have to first `cd` to a channel directory.

```
fchat send [message...]

e.g. fchat send Hello Threre!
```

### fchat read

Reads a messages from a channel. To use the commands you have to first `cd` to a channel directory.

```
fchat read (count)
```

- a count of last messages to read

### fchat sendfile

Sends a file to a channel. To use the commands you have to first `cd` to a channel directory.

```
fchat sendfile [path]
```

- path - a path of a file to send

### fchat download

Downloads a file from a channel. The file will be saved in the current directory. To use the commands you have to
first `cd` to a channel directory.

```
fchat download [name]
```

- a name of a file to download

## Running the server locally

-----------

In order to run the server locally:

1. Clone the repository
2. Run `./gradlew server:run`
3. A default server port is 1337. It can be changed using a `PORT` environmental variable
