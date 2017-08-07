Result
======

Results represent success or failure of an operation. They have sweet methods for doing great things.

## Installation

Add jitpack as a maven repository and then add the result dependency

    repositories {
        maven { url 'https://jitpack.io' }
    }
    
    dependencies {
        compile 'com.github.pivotal:resultkt:1.0.0'
    }

## Usage

You can make a result and access its values

    val success: Result<String, Any> = Result.success("all went well")
    success.success == "all went well"
    success.success { it == "all went well" }
    success.tap { println(it) }
    
Mostly though, you'll want to chain your results with map. Successful results will run the mapper,
failures will propagate and not be mapped.

    result
        .map { it.toUpperCase() }
        .map { it.length }
        .map { it + 1 }

At the end of your chained maps you will want to do something on success or failure

    val output = result.then(
           onSuccess = { success(it) },
           onFailure = { failure(it) }
    )    