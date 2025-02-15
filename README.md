
// README.mustache
# todo WebAssembly Interface Types

This directory contains WIT (WebAssembly Interface Type) definitions generated from an OpenAPI specification.

## Structure

- `api.wit`: The main WIT file containing all interfaces and types
- This README

## Usage

These WIT definitions can be used with WebAssembly component model tools like `wit-bindgen` to generate bindings for various languages.

### Example (Rust)

```bash
wit-bindgen rust api.wit --out-dir ./src/bindings
```

## Types

### ErrorError

Record with fields:
- code: enum _empty {
    validation_error,
    unauthorized,
    forbidden,
    not_found,
    rate_limit_exceeded,
    internal_error
}
- message: string
- details: list&lt;ErrorErrorDetailsInner&gt;

### ErrorErrorDetailsInner

Record with fields:
- field: string
- message: string

### Todo

Record with fields:
- id: string
- title: string
- description: string
- completed: bool
- dueDate: Datetime
- userId: string
- createdAt: Datetime
- updatedAt: Datetime

### Todocreate

Record with fields:
- title: string
- description: string
- dueDate: Datetime
- userId: string

### Todolist

Record with fields:
- data: list&lt;Todo&gt;
- metadata: TodolistMetadata

### TodolistMetadata

Record with fields:
- total: s32
- limit: s32
- offset: s32

### Todoresponse

Record with fields:
- data: Todo

### Todoupdate

Record with fields:
- title: string
- description: string
- completed: bool
- dueDate: Datetime

### error_type

Record with fields:
- error_type: ErrorError


## APIs

### DefaultApi

#### todosget

List todos

Parameters:
- userid: string
- status: EnumEmptyActiveCompleted
- limit: s32
- offset: s32
- sortby: EnumEmptyCreatedatDuedate
- sortorder: EnumEmptyAscDesc

Returns: expected&lt;Todolist, error&gt;

#### todospost

Create a new todo

Parameters:
- todocreate: Todocreate

Returns: expected&lt;Todoresponse, error&gt;

#### todostodoiddelete

Delete a todo

Parameters:
- todoid: string

Returns: expected&lt;void, error&gt;

#### todostodoidget

Get a specific todo

Parameters:
- todoid: string

Returns: expected&lt;Todoresponse, error&gt;

#### todostodoidpatch

Update a todo

Parameters:
- todoid: string
- todoupdate: Todoupdate

Returns: expected&lt;Todoresponse, error&gt;

