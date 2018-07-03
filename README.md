# basis-site
Basis-site is a static site generator that optionally lets you watch an input directory for changes and re-generate your static site. It is build on top of [basis-arguments](https://github.com/badlogic/basis-arguments) for CLI argument parsing, and [basis-template](https://github.com/badlogic/basis-template) for templating.

# Motivation
Why another static site generator?

* Tries not to do and be everything to everyone. Basis-site comes only with a handful of simple rules to apply to your static site generation. Bring your own site structure and functionality to be consumed by your templates.
* Can be integrated in a JVM web app that serves dynamic content, e.g. comments on a blog.
* Can be easily extended with any JVM language.
* Uses a more powerful templating language than Hugo and consorts.
* Only depends on basis-argument and basis-template, both having zero dependencies themselves.



