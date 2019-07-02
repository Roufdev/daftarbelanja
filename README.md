<img src="https://bluemixassets.eu-gb.mybluemix.net/api/Products/image/logos/cloudant.svg?key=[starter-cloudant]&event=readme-image-view" alt="Cloudant Logo" width="200px"/>

## Cloudant
Bluemix Mobile Starter for Cloudant Sync in Java

[![](https://img.shields.io/badge/bluemix-powered-blue.svg)](https://bluemix.net)
[![](https://img.shields.io/badge/platform-android-lightgrey.svg?style=flat)](https://developer.android.com/index.html)

### Table of Contents
* [Summary](#summary)
* [Requirements](#requirements)
* [Configuration](#configuration)
* [Run](#run)
* [License](#license)

### Summary

The Cloudant Sync starter Android application shows how to do basic CRUD
(create, read, update, delete) with the local Datastore and how to
replicate between a remote Cloudant database and a local Datastore.

The application is a simple example of a "to-do" list with items which
can be created, marked "done", and deleted.

### Requirements

Bluemix Account

### Configuration

None. The application already has your unique credentials embedded and will create and sync to a private database for you.  

### Run

Now you are ready to build and run the sample application. You can run
the application on an emulator or an a development-enabled Android
device.

When the application starts, it will attempt to create and connect to your private remote Cloudant database. Try adding a couple of tasks and hit "Upload (Push)" from the menu in the top right to add to the remote database. You should see these JSON documents appear in your Cloudant database. Changes to the documents in the Cloudant database will be replicated back to the device when you tap "Download (Pull)".

If you see "Replication Error" rather than "Replication Complete" as a popup message, check the logs to see more details on the exception.

If you'd like to enable more functionality provided by the Cloudant Sync sdk (like [encryption](https://github.com/cloudant/sync-android/blob/master/doc/encryption.md) or [conflict handling](https://github.com/cloudant/sync-android/blob/master/doc/conflicts.md)) see the [Cloudant Sync docs](https://github.com/cloudant/sync-android/tree/master/doc) for more learning.

### License
This package contains code licensed under the Apache License, Version 2.0 (the "License"). You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 and may also view the License in the LICENSE file within this package.
