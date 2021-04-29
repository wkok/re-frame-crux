# re-frame-crux

[re-frame](https://github.com/day8/re-frame) effects for communicating with a [Crux](https://opencrux.com/main/index.html) database via its REST API

## Overview

[Crux](https://opencrux.com/main/index.html) offers a REST API layer in the [crux-http-server](https://opencrux.com/reference/http.html) module that allows you to send transactions and run queries over HTTP.

This library exposes some of Crux's [REST API endpoints](https://opencrux.com/reference/http.html#rest-api) to your client side application as pre registered [re-frame effects](http://day8.github.io/re-frame/a-loop/#3rd-domino-effect-handling)

It allows you to get started prototyping your [re-frame](https://github.com/day8/re-frame/) frontend application quickly, by writing only client side code, while persisting your data to a (potentially) remote [Crux](https://opencrux.com/main/index.html) database. 

It is similar to the idea of the [Firebase Javascript SDK](https://firebase.google.com/docs/reference/js), allowing frontends to persist data to [Firebase](https://firebase.google.com/) in a 'serverless' way.

In fact this library borrows some of the ideas & structure from the very usefull [re-frame-firebase](https://github.com/deg/re-frame-firebase) library from David Goldfarb.

_Note: This library is still in alpha, and not all of Crux's features are supported yet. I am receptive to feature requests and happy to accept PRs_

## Configuration

[![Clojars Project](https://img.shields.io/clojars/v/com.github/wkok/re-frame-crux.svg)](https://clojars.org/com.github/wkok/re-frame-crux)

* Add this project to your dependencies
    * Leiningen: `[com.github.wkok/re-frame-crux "0.1.0.alpha"]`
    * Deps: `com.github.wkok/re-frame-crux {:mvn/version "0.1.0.alpha"}`

* Require the main namespace in your code
    * `(:require [wkok.re-frame-crux])`

* [Configure a proxy in your ClojureScript compiler](https://shadow-cljs.github.io/docs/UsersGuide.html#dev-http-proxy)
    * _Due to the fact that your ClojureScript code will need to make ajax calls to a host (Crux) which is not the same as the host from which the browser downloads the compiled Javascript, we need to proxy requests to Crux via the shadow-cljs http-server in order not to violate the browser's [CORS policy](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)_

`shadow-cljs.edn`
```clj
{:builds {:app {:devtools { :proxy-url "http://localhost:3000"}}}}
```

* Start a local instance of Crux

`docker`
```bash
docker run -p 3000:3000 juxt/crux-in-memory:21.04-1.16.0
```

For more options to start a local Crux instance, see [howto](https://opencrux.com/howto) and [installation](https://opencrux.com/reference/installation.html)

## Usage

The public portions of the API are all in [re\_frame\_crux.cljs](src/wkok/re_frame_crux.cljs). That file also includes API documentation that may sometimes be more current or complete than what is described here.

This is a [re-frame](https://github.com/day8/re-frame/) API. It is primarily accessed through [re-frame](https://github.com/day8/re-frame/) events and subscriptions.

### PUT

Puts a Document map into Crux
See https://opencrux.com/reference/transactions.html#put

Key arguments:
- `:doc`             Map corresponding to the document. (The `:crux.db/id` key is optional, if not provided, a UUID will be injected)
- `:valid-time`      Optional. Example: "2017-01-20" See: https://opencrux.com/reference/transactions.html#valid-times
- `:end-valid-time`  Optional. Example: "2017-01-20" See: https://opencrux.com/reference/transactions.html#valid-times
- `:on-success`      Function or re-frame event vector to be dispatched.
- `:on-failure`      Function or re-frame event vector to be dispatched.

Example FX:
```clj
(rf/reg-event-fx
  :put-president
  (fn [_ [_ _]]
    {:crux/put {:doc     {:crux.db/id           :president/biden
                          :president/first-name "Joe"
                          :president/last-name  "Biden"}
             :on-success [:success-event]
             :on-failure #(js/console.log %)}}))
```

### GET

Returns the document map for a particular entity
See: https://opencrux.com/reference/http.html#entity

Key arguments:
- `:id`              Document ID
- `:valid-time`      Optional. Example: "2017-01-20" See: https://opencrux.com/reference/transactions.html#valid-times
- `:tx-time`         Optional. Example: "2017-01-20" See: https://opencrux.com/reference/transactions.html#valid-times
- `:tx-id`           Optional. Transaction ID
- `:on-success`      Function or re-frame event vector to be dispatched.
- `:on-failure`      Function or re-frame event vector to be dispatched.

Example FX:
```clj
(rf/reg-event-fx
  :get-president
  (fn [_ [_ _]]
    {:crux/get {:id         :president/biden
                :on-success [:success-event]
                :on-failure #(js/console.log %)}}))
```

### DELETE

Deletes a Document from Crux
See https://opencrux.com/reference/transactions.html#delete

Key arguments:
- `:id`              Document ID to be deleted
- `:valid-time`      Optional. Example: "2017-01-20" See: https://opencrux.com/reference/transactions.html#valid-times
- `:end-valid-time`  Optional. Example: "2017-01-20" See: https://opencrux.com/reference/transactions.html#valid-times
- `:on-success`      Function or re-frame event vector to be dispatched.
- `:on-failure`      Function or re-frame event vector to be dispatched.

Example FX:
```clj
(rf/reg-event-fx
  :delete-president
  (fn [_ [_ _]]
    {:crux/delete {:id         :president/trump
                   :on-success [:success-event]
                   :on-failure #(js/console.log %)}}))
```

### QUERY

Takes a datalog query and returns its results
See https://opencrux.com/reference/queries.html

Key arguments:
- `:query`       Quoted EDN Map containing the Datalog query
- `:on-success`  Function or re-frame event vector to be dispatched.
- `:on-failure`  Function or re-frame event vector to be dispatched.

Example FX:
```clj
(rf/reg-event-fx
  :find-presidents
  (fn [_ [_ _]]
    {:crux/query {:query      '{:find  [?first-name ?last-name]
                                :keys  [first-name last-name]
                                :where [[?e :president/first-name ?first-name]
                                        [?e :president/last-name ?last-name]]}
                  :on-success [:success-event]
                  :on-failure #(js/console.log %)}}))
```

### TRANSACTIONS

Transactions are comprised of a vector of Transaction Operations to be performed.
See https://opencrux.com/reference/transactions.html

Key arguments:
- `:ops`         Vector consisting of transaction operations
- `:on-success`  Function or re-frame event vector to be dispatched.
- `:on-failure`  Function or re-frame event vector to be dispatched.

Example FX:
```clj
(rf/reg-event-fx
  :rotate-presidents
  (fn [_ [_ _]]
    {:crux/submit-tx {:ops        [[:crux.tx/put {:crux.db/id :president/biden :president/first-name "Joe"}]
                                   [:crux.tx/delete :president/trump]]
                      :on-success [:success-event]
                      :on-failure #(js/console.log %)}}))
```

### MATCH / EVICT

[Match](https://opencrux.com/reference/transactions.html#match) & [Evict](https://opencrux.com/reference/transactions.html#evict) can be accomplished with the `:crux/submit-tx` effect.

### MULTI

Convenience effect used to dispatch a vector of crux effects.
(Not to be confused with Crux transactions)

:crux/multi will execute a 'vector of effects', for example,
it can be used to execute multiple queries each with separate on-success callbacks

Example FX:
```clj
(rf/reg-event-fx
  :do-many-things
  (fn [_ [_ _]]
    {:crux/multi [[:crux/query {:query      '{:find  [?first-name ?last-name]
                                              :keys  [first-name last-name]
                                              :where [[?e :president/first-name ?first-name]
                                                      [?e :president/last-name ?last-name]]}
                                :on-success [:success-event-president]}]
                  [:crux/query {:query      '{:find  [?first-name ?last-name]
                                              :keys  [first-name last-name]
                                              :where [[?e :deputy/first-name ?first-name]
                                                      [?e :deputy/last-name ?last-name]]}
                                :on-success [:success-event-deputy]}]
                  [:crux/put {:doc        {:crux.db/id           :secretary/blinken
                                           :secretary/first-name "Antony"
                                           :secretary/last-name  "Blinken"}
                              :on-success [:success-event]}]]}))
```

## Authentication

The Crux HTTP Server can be configured to require athentication, by adding a JWT key set to its startup parameters, see: https://opencrux.com/reference/http.html#start-http-server

A JWT key set can be generated with this utility [Command line JSON Web Key](https://connect2id.com/products/nimbus-jose-jwt/generator)

Your application is responsible for handling authentication with some authentication provider and populate the received JWT token (signed with the same key configured in Crux) in the re-frame app db under the `:crux` -> `:jwt` key like this: `{:crux {:jwt "mytoken"}}`

If this JWT token is set in the re-frame app-db, every request from this library to Crux will contain the token in the HTTP Authorization header.

## Production

Some things to keep in mind before launching your brand new 'serverless' application into production

### Security

- Consider whether the Crux endpoint should be served securely over HTTPS by putting an HTTPS load balancer in front of it.
- Consider configuring Crux to require JWT authentication (see above)
- You'd probably need to write some server side code, at least to implement authorization rules over your data. Remember, even with authentication configured in Crux, any authenticated user will have full read/write access to all data, which might not be what you want. To get you started, Crux exposes a Ring handler, see: https://opencrux.com/reference/http.html#_ring_handler

### Cross-Origin Resource Sharing (CORS)

If your compiled Javascript is not downloaded from the same HTTP Server that is running the Crux REST API, you'll still need to honour the browser's [CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS) policy. Here's two options:

- Configure [ProxyPass](https://docs.nginx.com/nginx/admin-guide/web-server/reverse-proxy/) rules in your load balancer to proxy any requests for `/_crux/*` to the Crux REST endpoint
- Or, in your own [Ring-compatible server hosting Crux](https://opencrux.com/reference/http.html#_ring_handler), consider middleware like [ring-cors](https://github.com/r0man/ring-cors), and add the public URL for this Ring server in the re-frame app db under the `:crux` -> `:url` key like this: `{:crux {:url "https://url-to-crux"}}`


## License

[MIT License](https://choosealicense.com/licenses/mit/)

Copyright (c) 2021 Werner Kok

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
