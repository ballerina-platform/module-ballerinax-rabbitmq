# Changelog
This file contains all the notable changes done to the Ballerina RabbitMQ package through the releases.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

### Changed
- [[#5069] Remove the definition and the usages of the deprecated rabbitmq:Message record](https://github.com/ballerina-platform/ballerina-library/issues/5069)

## [2.10.0] - 2023-09-18

### Changed
- [[#4734] Changed disallowing service level annotations in the compiler plugin](https://github.com/ballerina-platform/ballerina-standard-library/issues/4734)

## [2.7.0] - 2023-04-10

### Changed
- [[#4237] Exit the Listener When Panic Occurred](https://github.com/ballerina-platform/ballerina-standard-library/issues/4237)

# [2.5.0] - 2022-11-29

### Changed
- [API docs updated](https://github.com/ballerina-platform/ballerina-standard-library/issues/3463)

### Fixed
- [Fix error in passing additional arguments when declaring a queue.](https://github.com/ballerina-platform/ballerina-standard-library/issues/3686)

### Added
- [Add support for AnydataMessage in client acknowledgment APIs.](https://github.com/ballerina-platform/ballerina-standard-library/issues/3685)
- [Add support for virtual host configuration in connection config.](https://github.com/ballerina-platform/ballerina-standard-library/issues/3658)

## [2.4.0] - 2022-09-08

### Added
- [Add code-actions to generate rabbitmq:Service template.](https://github.com/ballerina-platform/ballerina-standard-library/issues/2770)
- [Add data-binding support.](https://github.com/ballerina-platform/ballerina-standard-library/issues/2812)
- [Add constraint validation support.](https://github.com/ballerina-platform/ballerina-standard-library/issues/3054)

## [2.2.1] - 2022-03-01

### Changed
- [Mark RabbitMQ service type as distinct.](https://github.com/ballerina-platform/ballerina-standard-library/issues/2398)

### Added
- [Add public cert and private key support for RabbitMQ secure socket.](https://github.com/ballerina-platform/ballerina-standard-library/issues/1470)

### Changed
- Mark close and abort functions in RabbitMQ client as remote 

## [1.1.0-alpha9] - 2021-04-22

### Added
- [Add compiler plugin validations for RabbitMQ services.](https://github.com/ballerina-platform/ballerina-standard-library/issues/1236)

## [1.1.0-alpha7] - 2021-04-02

### Changed
- [Change the configurations required to initialize the client and the listener.](https://github.com/ballerina-platform/ballerina-standard-library/issues/1178)
- [Change the `ConnectionConfiguration` in client and listener init to an included record parameter.](https://github.com/ballerina-platform/ballerina-standard-library/issues/1178)
