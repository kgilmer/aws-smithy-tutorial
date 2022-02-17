An intro to codegen with AWS Smithy — Symbols
=============================================

This is the 4th part of a series on writing code generators with [AWS Smithy](https://awslabs.github.io/smithy/). In [part I](https://medium.com/@kgilmer/an-intro-to-codegen-with-aws-smithy-setup-98dca1f589fe?source=your_stories_page----------------------------------------) we get a basic project up that integrates with Smithy via Gradle. In [part II](https://medium.com/@kgilmer/an-intro-to-codegen-with-aws-smithy-ii-graphviz-eafcdb57e4f?source=your_stories_page----------------------------------------) we implement a simple codegen for the GraphViz Dot language. In [part III](https://medium.com/@kgilmer/an-intro-to-codegen-with-aws-smithy-iii-c-entity-codegen-d543391a6f94?source=your_stories_page----------------------------------------) we implement another simple codegen for C++ entity types. In this post we’ll add some additional functionality into our C++ codegen that is important for a clean and maintainable codegen implementation.

NOTE: The code mentioned in this article is available on GitHub here: [https://github.com/kgilmer/aws-smithy-tutorial/tree/part-4](https://github.com/kgilmer/aws-smithy-tutorial/tree/part-4)

![](https://miro.medium.com/max/1400/1*MW2wo8ue-oUTylrKgofNpQ.jpeg)

In part III of this series, we implemented entity type codegen in C++. The code to produce the C++ is littered with minutia such as capitalizing strings for use in class names, formatting parameter list expressions, and mapping Smithy [types](https://awslabs.github.io/smithy/1.0/spec/core/model.html#simple-shapes) to C++ types. Because this C++ knowledge is directly embedded in the code that produces strings, it becomes difficult to modify and maintain. Additionally, what if something provided by a model conflicts with a language keyword, for example a service called “class”? If we blindly write out whatever the model provides, we have no assurance that the output is valid. Ideally there would be a way to encapsulate the logic that “knows” about C++ in such a way that it could be changed once in a consistent way, and the rest of the codegen would just work without further modification. Smithy provides an abstraction exactly for this, called a [Symbol](https://awslabs.github.io/smithy/javadoc/1.8.0/software/amazon/smithy/codegen/core/Symbol.html).

Mapping Names
=============

A simple first thing we can clean up with Smithy Symbols is how C++ names are produced in our codegen. In Part III we simply embedded some string expressions directly. We’ll extract this logic into a Symbol, and use the [SymbolProvider](https://awslabs.github.io/smithy/javadoc/1.8.0/software/amazon/smithy/codegen/core/SymbolProvider.html) as the mapper between our model domain and our language domain. For the purposes of this post, we’ll add our `SymbolProvider` to our `CodegenPlugin` however you may wish to extract this as an independent type in a real codegen project.

Here we specify that the name of a `Structure` shape to be the `name` segment of it’s ID in Smithy. This is likely too simple for a real codegen project but is sufficient for our purposes here.

Now that we can retrieve a `Symbol` for our structure shapes, we can use it in the `HeaderGenerator` codegen:

With this change we’ve moved the logic that determines the name of a C++ class from the internals of the header emitter to the Symbol. We then refactor the remaining codegen to use our `Structure` symbol to determine the name of the C++ class.

Next let’s extract the knowledge that a Smithy `String` should map to a C++ `std::string` from the codegen emission functions into our `SymbolProvider`. For this to work we’ll need to get access to the `Model` instance from our `SymbolProvider` in order to, among other tasks, resolve a `Shape` from it’s id:

And we’ll need to set the value of `pluginContext` when we get access to it from the `execute(PluginContext)` function:

Finally we can remove the mapping from our codegen to the `SymbolProvider` in `HeaderGenerator.kt`:

And with this change we now have a single place to provide knowledge about C++ that can be use in any codegen logic throughout our project.

Another detail of C++ we can move into our `SymbolProvider` is the notion of the source files themselves. In `CodegenPlugin` we have the following code:

Smithy `Symbol`s already have the notion of a source file. We can augment our `toSymbol()` function in the case of `Structure` to specify the names of the header and source files:

Note: `Symbol` supports the ability to specify whatever state you may find useful via a `Map` of properties.

Then we can utilize this by getting the `Symbol` from our structures and resolving the source filenames from it:

The process of refactoring language-specific type information into `Symbol` s can continue until the functions that generate each source file are only concerned with the overall structure, and not the type information. An important note about `Symbol`s we won’t cover here is that they can also model dependency relationships. For example, notice how we statically emit the string `#include <iostream>` at the top of our `generateEntityHeader()` function. It would be better if our dependencies were modeled in our Symbols and the act of adding a Symbol to a codegen output automatically handled the necessary work to include any headers. Additionally, a superset of that information could be used to generate build files as well. We won’t go into these details here but keep in mind that Smithy provides functionality for complex aspects of codegen.

Summary
=======

In this post we learned about Smithy’s `Symbol` abstraction, and how we can use it to extract language-specific information out of various codegen functions into a single place. The code from this post is available at: [https://github.com/kgilmer/aws-smithy-tutorial/tree/part-4](https://github.com/kgilmer/aws-smithy-tutorial/tree/part-4)
