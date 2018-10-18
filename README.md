Workshop: Divolte Clickstream Collection
========================================

This repository contains the exercises for the _Divolte Clickstream Collection_ workshop, first presented as part of [GoDataFest] in 2018.

The exercises assume you have [Docker] and [Docker Compose] installed and working locally, as well as some sort of editor you're familiar with for editing files.

Some shell scripts are provided for convenience:

 - `./refresh`  
   This will build the local services and ensure everything starts up. After making changes you can run this script and it will rebuild and restart the services as necessary.
 - `./load-data`  
   This will run a one-off process that is necessary after starting the stack for the first-time. (It loads the data necessary for the application to work.)

Once the stack is up the demonstration application will be available at [http://localhost:9011/](http://localhost:9011/).

The exercises are described in three files and should be completed in order:

 - `exercise-1.md`
 - `exercise-2.md`
 - `exercise-3.md`

If you have any questions or comments, please let us know.

[GoDataFest]:     https://www.godatafest.com/
[Docker]:         https://docs.docker.com/install/
[Docker Compose]: https://docs.docker.com/compose/install/
