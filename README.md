# Schema Registry SerDe Examples

This project has been split into two separate Maven projects to isolate dependencies and avoid conflicts between Apicurio Registry and Confluent Schema Registry SerDe implementations.

## Project Structure

### 1. Apicurio Registry SerDe (`apicurio-registry-serde/`)

Contains Apicurio Registry based kafka applications for testing. Also
contains a pure ByteArray based deserializer application for debugging.

### 2. Confluent Schema Registry SerDe (`confluent-schema-registry-serde/`)

Contains Confluent based kafka applications for testing.

## Why Split?

Both Apicurio and Confluent SerDe libraries depend on different versions of:
- `wire-schema`
- `wire-compiler`
- `wire-runtime`

This causes classpath conflicts when both are included in the same project. By separating them, each project has clean, isolated dependencies.
