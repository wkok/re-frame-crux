# Introduction to re-frame-crux

[Crux](https://opencrux.com/main/index.html) offers a REST API layer in the [crux-http-server](https://opencrux.com/reference/http.html) module that allows you to send transactions and run queries over HTTP.

This library exposes some of Crux's [REST API endpoints](https://opencrux.com/reference/http.html#rest-api) to your client side application as pre registered [re-frame effects](http://day8.github.io/re-frame/a-loop/#3rd-domino-effect-handling)

It allows you to get started prototyping your [re-frame](https://github.com/day8/re-frame/) frontend application quickly, by writing only client side code, while persisting your data to a (potentially) remote [Crux](https://opencrux.com/main/index.html) database. 

It is similar to the idea of the [Firebase Javascript SDK](https://firebase.google.com/docs/reference/js), allowing frontends to persist data to [Firebase](https://firebase.google.com/) in a 'serverless' way.

In fact this library borrows some of the ideas & structure from the very usefull [re-frame-firebase](https://github.com/deg/re-frame-firebase) library from David Goldfarb.

_Note: This library is still in alpha, and not all of Crux's features are supported yet. I am receptive to feature requests and happy to accept PRs_
