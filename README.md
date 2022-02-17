An intro to codegen with AWS Smithy — III C++ entity codegen
============================================================

![](https://miro.medium.com/max/1400/1*96-qq8SBd2fcKwEyvx4pyg.jpeg)

In [parts I](https://medium.com/@kgilmer/an-intro-to-codegen-with-aws-smithy-setup-98dca1f589fe) and [II](https://medium.com/@kgilmer/an-intro-to-codegen-with-aws-smithy-ii-graphviz-eafcdb57e4f?source=your_stories_page----------------------------------------) we learned the basics of how to get a Smithy-based codegen project setup and generating a simple “program”. In this part we’ll pull in more functionality by Smithy to produce a codegen module that generates C++ classes from a Smithy model’s request and response shapes. Such a codegen could be used as a foundational piece to bigger codegen projects that may utilize these types to generate, say a C++ service client or server stub.

The code in this post is available in this GitHub repository/branch: [https://github.com/kgilmer/aws-smithy-tutorial/tree/part-3](https://github.com/kgilmer/aws-smithy-tutorial/tree/part-3)

Upgrades to `CodegenPlugin`
===========================

First we’ll remove all of our code from the GraphViz Dot work in the past part, and use a type provided by Smithy, `ShapeVisitor` , to provide a simple way of getting an event for each shape in a model. Using these events, we can drive codegen to produce a complete output for a given model. For brevity we’ll have our `CodegenPlugin` also implement the `ShapeVisitor` interface although you may choose to implement it as a stand-alone type.

Next we need to call into `ShapeVisitor` with our model to be able to receive events for shapes in `execute()`:

And then we’ll provide a simple implementation for all of the new methods that simply prints the shape to stdout :

After updating all functions from `ShapeVisitor` and running our `codegen-test` module, we’ll get output like this from the `build` task:

```
I see (service: \`example.weather.simple#Weather\`)  
I see (operation: \`example.weather.simple#GetCurrentTemp\`)  
I see (structure: \`example.weather.simple#GetCurrentTempOutput\`)  
I see (member: \`example.weather.simple#GetCurrentTempOutput$temp\`)  
I see (string: \`smithy.api#String\`)  
I see (structure: \`example.weather.simple#GetCurrentTempInput\`)  
I see (member: \`example.weather.simple#GetCurrentTempInput$format\`)
```

From seeing these event messages print out it becomes clear that we can, upon a model event, generate code by reading specific state from the model and combining it with language-specific strings to produce a working codegen.

As in the previous post, before digging into the codegen portion of the implementation let’s first explore what we’d like to produce by writing it by hand first. In this case, as this is only for demonstration purposes, we’ll generate simple C++ classes. In a more practical codegen project, we may wish to generate different kinds of classes based on some settings. This can be consistently handled by representing our settings as a JSON object within the `smithy-build.json` file. In fact, if we look at the `PluginContext` that’s passed to us in `execute(PluginContext)` we can see a property `settings` which provides any custom JSON to us that was specified in `smithy-build.json`.

A simple C++ class for `GetCurrentTempInput` may look like:

A simple C++ class for `GetCurrentTempOutput` may look like:

From the [Smithy documentation](https://awslabs.github.io/smithy/1.0/spec/core/model.html#structure) we can see that to generate entity types in C++ we’d be transforming a Smithy model’s `structure`types. Unlike in our previous post, let’s do something better than printing code to stdout. Smithy provides a utility class for writing code to files called `[CodeWriter](https://awslabs.github.io/smithy/javadoc/1.6.1/software/amazon/smithy/utils/CodeWriter.html)`. Keep in mind it’s not coupled to Smithy model APIs and so is entirely optional. A `CodeWriter` corresponds to a single source file in a target language. To generate C++ files we can define some defaults like indention and new line behavior. We can express these types as a new subclass of `CodeWriter`that will also include some management logic. It’s nice to keep track of all the `CodeWriter`s in a consistent location to ensure that there are not multiple instances per file. We also need a way of writing the contents of a `CodeWriter`to the disk. We’ll add this functionality in our `CppWriter`:

We provide a function to return a `CodeWriter`based on the filename, and a function to emit all code to the `FileManifest` provided by the `CodegenContext`instance provided to us by Smithy. By using this `FileManifest` we don’t have to concern ourselves with where the files are written to. Smithy has well-defined locations for these files.

Now that we can write to files via our `CodeWriter` implementation, let’s get down to the business of generating C++ source. First let’s tackle the header files. We can create a new Kotlin file for functions relating to generating headers, `HeaderGenerator.kt` :

This is relatively a lot of code, but there is nothing too complicated going on in here. We essentially are querying the Smithy Model API for specifics (member names, children of members, etc.) and then generating the C++ with the model data. This code is overly verbose, and there are lots of ways we can clean it up and make it more manageable, but for the purposes of learning it may be best to keep it simpler. In summary, we call `generateEntityHeader()` as the top-level function for headers. We generate some common code such as `include` declarations and then generate the class declaration. We implement some more functions for specific portions of the header file, the constructor parameters, fields, and member declarations. We also implement a simple function to produce a C++ type for some Smithy shape. In our simple case, we only care about strings (due to our simple [Weather model](https://awslabs.github.io/smithy/quickstart.html#weather-service) example) and so this is extremely brief. There are specific Smithy APIs that should be used for serious codegen implementations, but we’ll get to that in a later post.

Now we can add another Kotlin file for functions relating to the generation of the Cpp files themselves. It re-uses some code from the `HeaderGenerator.kt` and works in a similar way:

Next we need to call our generators from the `CodegenPlugin` . Because we’re generating entity types, we need to write code from the `structureShape()`function:

Lastly, we need to integrate our CppWriter’s `flushAll()` function into our plugin implementation to ensure that the file contents are actually written to the build directory. To make this happen we’ll update our `CodegenPlugin.execute()` implementation:

Once all of this is in place, we can generate some C++ code! Running the Gradle task `build` in `codegen-test` will cause 4 files to be emitted to `build/smithyprojections/codegen-test/example-codegen`:

```
$ tree codegen-test/build/smithyprojections/codegen-test/source/example-codegen/  
codegen-test/build/smithyprojections/codegen-test/source/example-codegen/  
├── GetCurrentTempInput.cpp  
├── GetCurrentTempInput.h  
├── GetCurrentTempOutput.cpp  
└── GetCurrentTempOutput.h  
$ cat codegen-test/build/smithyprojections/codegen-test/source/example-codegen/GetCurrentTempInput.h#include <iostream>  
  
class GetCurrentTempInput {  
public:  
    GetCurrentTempInput(std::string format);  
  
    std::string getFormat();  
    void setFormat(std::string format);  
private:  
    std::string \_format;  
};
```

Summary
=======

In this post we’ve implemented a simple C++ entity type codegen. We have incorporated some more Smithy APIs into our project: `CodeWriter`, `ShapeVisitor`, and many functions provided by the Smithy Model API such as `StructureShape.members()` . A working version of this code is available at [https://github.com/kgilmer/aws-smithy-tutorial/tree/part-3/codegen/src/main/kotlin/org/example/smithy](https://github.com/kgilmer/aws-smithy-tutorial/tree/part-3/codegen/src/main/kotlin/org/example/smithy).

Next Up: [Part IV — Symbols]()
