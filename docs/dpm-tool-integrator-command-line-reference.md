# DPM Tool Integrator -command-line reference


## 1. Overview

Command line interface for interacting with Data Point Modeling tool.

Command line utility supports following operations: 
- Listing Data Models from the DPM Tool
- Importing new Model Version to existing Data Model on the DPM Tool from the local database
 

<br/>

### 1.1 Revision history

| Revision | Date       | Author(s) | Description                                                |
| -------- | ---------- | --------- | ---------------------------------------------------------- |
| 0.1      | 2019-09-10 | HE        | Initial help content for DPM Tool Integrator 0.1.0 version |
| 0.2      | 2019-11-19 | HE        | Support for password prompt                                |
|          |            |           |                                                            |

<br/>

## 2. Command line options

`--help`

Prints help text about command line options and exit.

`--version`

Prints information about the DPM Tool Integrator version and exit.

`--list-data-models`

Lists Data Models available in the DPM Tool and exit. 

`--import-db-to-existing-model` _[FileName]_

Import new Model Version to existing Data Model from given DPM database. _[FileName]_ must point to a local database file. 

`--target-data-model` _[ModelName]_

Identifies the target Data Model for the import operation. _[ModelName]_ must be name of existing Data Model.

`--dpm-tool-config`  _[FileName]_

Selects the configuration file, from where the DPM Tool host addresses etc details are loaded.
_[FileName]_ must point to a local configuration file.
Optional. If not given, _dpm-integrator-config.json_ file will be loaded A) from current working directory or B) from the folder containing application JAR.
See section _4. DPM Tool Integrator configuration file structure_ for further reference.

`--username`  _[Username]_

Username for authenticating with the DPM Tool. 

`--password`  _[Password]_

Password for authenticating with the DPM Tool. Optional. If not given, password will be asked later.

<br/>

## 3. Command line examples 

### 3.1 Show command line help

```
integrator --help
```

Prints help text about command line options and exit.

<br/>

### 3.2 List Data Models from the DPM Tool

```
integrator --list-data-models --username <username> --password <password>
```

Lists existing Data Models from the DPM Tool (`--list-data-models`). Authentication username and password are provided via command line. 

<br/>


### 3.3 List Data Models from the DPM Tool and prompt for password

```
integrator --list-data-models --username <username>
```

Lists existing Data Models from the DPM Tool. Authentication username is taken from command line, password is asked via prompt. 

<br/>


### 3.4 Import new Model Version to existing Data Model

```
integrator-cli.jar --import-db-to-existing-model <database_file.db> --target-data-model <taget_model_name> --username <username> --password <password>
```

Creates new Model Version to existing Data Model (`--import-db-to-existing-model`) by importing Model Version contents from
given DPM database file (`<database_file.db>`). Model Version is created to target data model (`--target-data-model`) with given (`<taget_model_name>`).
Note: If data model name contain spaces, remember use quotes around the name in the command line (`"name with multiple words"`).
Given username and password are used in authentication.

<br/>

## 4. DPM Tool Integrator configuration file structure

The DPM Tool Integrator uses following configuration file structure do define host address etc details for the used DPM Tool. 

```json
{
  "dpmToolName": "<Name of the DPM Tool, used in CLI user interface>",
  "clientAuthBasic": {
    "username": "<Client identifier for DPM Tool authentication",
    "password": "<Client password for DPM Tool authentication"
  },
  "serviceAddress": {
    "authServiceHost": "<DPM Tool Authentication service host address>",
    "hmrServiceHost": "<DPM Tool HMR service host address>",
    "exportImportServiceHost": "<DPM Tool Export/Import service host address>"
  }
}
```

