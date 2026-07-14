---
description: OkHttp Client usage in functional and selenium tests, cleanup responses to avoid connection leaks.
globs:
  - functional-test/src/test/groovy/**/*.groovy
alwaysApply: false
applyTo: "functional-test/src/test/groovy/**/*.groovy"
---

# Background

The functional and Selenium tests make use of an OkHttp client object.

Any use of OkHttpClient to make a request creates a Response object, and the Response object must be cleaned up to 
prevent response buffers from interfering with each other within the internal pool.  Response objects
can be either closed explicitly, or the response body can be consumed by the client code, or the 
Response object can be used in a try-with-resources clause to automatically close it because it implements Closeable.

**Important**: If a Response body is not consumed, then it's possible for the content in the response buffer to leak into a later request, which can cause 
unexpected content to be returned, and sometimes causes an unexpected failure in a later test.

This can appear as an unexpected exception looking like:

```
Caused by: java.net.ProtocolException: Unexpected status line: 5D
	at okhttp3.internal.http.StatusLine$Companion.parse(StatusLine.kt:73)
```

## RdClient and Base Test Classes

Test classes provide a wrapper type which holds the OkHttpClient, in a utility class called RdClient. 
In addition we have base classes for all functional and selenium unit test classes (`BaseContainer`, `SeleniumBaseContainer` and `ClusterBaseContainer`)
which provide additional wrapper utility method shortcuts for using the client to make requests.

The RdClient wrapper, and the "BaseContainer" base test classes, both provide methods for making HTTP requests, in two patterns:

1. do*X* - where *X* is a HTTP method such as *Get*, *Post*, *Delete* etc, or a bare `doRequest` method.  The return type is the OkHttp Response object.
    * RdClient: These methods do not automatically close the response object or consume the response body, so it is up to the caller to ensure cleanup.
    * Base test classes: These methods *do* automatically cache the Response object to clean up later as a Closeable resource, so the caller does not need to explicitly close the response or consume the body.
2. bare *x* methods, where *x* is a HTTP method, such as `get`, `post`, etc, where the return type is a Java type that has been deserialized from a JSON response.
    * These methods *do* automatically consume the response body to deserialize the response, and do not require explicit cleanup.

In addition the base test classes *do* automatically cache the response object to clean up later as a Closeable resource for the `do*HttpMethod*` methods.

## Mandatory

1. If a 200 status is expected, and only the response content is necessary for the test, prefer the `get(..)`,`post(...)`, etc. form of the methods.
   * Corollary, if you can simplify a `doGet` followed by a `response.body().string()` into a single `get(...)` call, then do so.
        * For example, instead of this:
   ```groovy
        def response = client.doGet("/config/get?key=${key}&strata=default")
         if(response?.body()!=null){
             def parsedBody = mapper.readValue(response?.body()?.string(), Map.class)
             if(parsedBody?.value == value){
                 seenValue = true
             }
         }
   ```
    * Do this:
    ```groovy
          def response = client.get("/config/get?key=${key}&strata=default", Map.class)
          if(response?.value == value){
              seenValue = true
          }
    ```
2. If the test needs to validate the status code, or headers, or other aspects of the response in addition to the body content, then use the `do*X*` form of the method.
   * Prefer using the `do*X*` methods on the base test classes, which will automatically cache the Response for cleanup.
3. **Close OkHttp Responses**: In all other cases where a bare OkHttp `Response` object is returned, the object MUST be closed *OR* have the response body consumed, using one of these methods: 
   * **Preferred**: Use try-with-resources to automatically close the Response object. e.g. `try(def response = client.doGet(...)) { ... }`
   * Consume the response body using `response.body().string()` or similar.  This is ok:
        ```
        def response = client.doGet(...)
        def content = response.body().string()  // consumes body and closes response
        // use content in test
        ```
   * Otherwise ensure `response.close()` is called, such as in a `cleanup:` block of the Spock spec.
  

## Before Completing

Verify all rules are satisfied in the file. Fix any violations before responding.