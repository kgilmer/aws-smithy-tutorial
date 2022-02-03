This Gradle module represents the codegen plugin for Smithy.  It integrates with Smithy to generate code.

This codegen project will generate graphviz dot diagrams from API models.  An example diagram in dot format:

```
digraph D {

  subgraph cluster_p {
    label = "Parent";

    subgraph cluster_c1 {
      label = "Child one";
      a;

      subgraph cluster_gc_1 {
        label = "Grand-Child one";
         b;
      }
      subgraph cluster_gc_2 {
        label = "Grand-Child two";
          c;
          d;
      }

    }

    subgraph cluster_c2 {
      label = "Child two";
      e;
    }
  }
} 
```

This type of diagram may be useful to view relationships across entities and containment.
In our codegen project we'll model services -> operations - [inputs, outputs, errors] -> Shapes
