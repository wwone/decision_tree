# decision_tree
This project is a set of Java code that creates a static "decision tree" website. 

The created HTML static pages will be of two types: 
1.  queries, where the web users will be asked a question, and click on an answer "yes" or "no". 
2.   endpoints, where the user will be shown a description of the result of their yes/no entries. 
   
Other static HTML pages may link into any of the query or endpoint pages, and the query and endpoint pages may contain any HTML desired.

Note that **ALL** endpoint pages will be created, each time the program is invoked. The query pages are only created from the given starting point, and all queries must connect correctly. Allowing for multiple entry points makes it possible to enter the decision tree from many possible places, often referred to from static HTML pages which are not part of this created tree.

The structure of the query tree and enpoint information is JSON, as are a few "boilerplate" HTML sections used in all created pages.

An example will be provided of a postal history topic: a postal machine cancel "finder".

This code is active now at:

http://swansongrp.com/machtest/machtest.html


Why use static HTML pages? The reasoning here is to provide as much information as possible to search engines. The "logic" of the decision tree will be embedded in the structure of HTML pages, rather than hidden behind the web in a database, complex javascript, or other logic engine.
