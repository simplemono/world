# World

This library provides tools to work with the world-state approach that has been
invented by David Nolen:

[<img src="https://i.ytimg.com/vi/qDGTxyIrKJY/hq2.jpg" width="50%">](https://www.youtube.com/watch?v=qDGTxyIrKJY&t=1659s "Keynote: The Power of Toys - David Nolen - Lambda Days 2022")

The core idea is that functions can be put together like Lego bricks if they
have the following signature:

```
f(w0, a, b, c, ...) → w1
```

The function takes a Clojure map as the first argument, adds something, and returns
this map. However, this library goes one step further so that every function has this
signature:

```
f(w0) → w1
```

All arguments should be passed in the `w0` map.

## Why

Software tends to become complex very quickly, even if you use Clojure and
embrace its simplicity. Logs help to find and diagnose a bug. But the likelihood
is high that you need to log more data to understand the bug fully. During
development, temporarily adding a `def` or `println` here and there helps, but
good luck trying to do this in your production system.

Functions with the signature described above can be put together like this:

```clojure
(f2
 (f1
  (f0 w0)))
```

Or with:

```
(-> w0
    (f0)
    (f1)
    (f2))
```

Let's assume `f1` contains a bug. Both variants will not provide you with the
Clojure map that has been passed to `f1`. This variant:

```clojure
(reduce
 (fn [wx f]
   (try
     (f wx)
     (catch Throwable e
       (log/error e {:world/value wx})
       (throw e))))
 w0
 [f0
  f1
  f2])
```

catches all exceptions and logs the input map before it rethrows it. Having all
input data should increase the chances of finding, understanding, and fixing the bug.

This library provides the function `world-reduce` and the macro `w<` for this
purpose:

```clojure
(w< (f2
     (f1
      (f0 w0)))
```

Additionally, they solve another challenge that is described in the section
'Logging'.

The second part of the 'why' is that functions with the described signature
cannot only be put together like Lego bricks, but they can also stay very simple
like Lego bricks. Each function can solve a single task and doesn't need to know
where the input data is coming from or how to pass its results to the next
function.

## Example

The small example below fetches data from the [HackerNews
API](https://github.com/HackerNews/API).

```clojure
(ns examples.hackernews
  (:require [simplemono.world.core :as w]
            [clj-http.client :as http]))

(defn add-top-stories-request
  [w]
  (assoc w
         :top-stories-request
         {:request-method :get
          :url "https://hacker-news.firebaseio.com/v0/topstories.json"
          :as :json}))

(defn get-top-stories
  [{:keys [top-stories-request] :as w}]
  (assoc w
         :top-stories-response
         (http/request top-stories-request)))

(defn add-item-ids
  [{:keys [top-stories-response top-stories-count] :as w}]
  (assoc w
         :item-ids
         (take top-stories-count
               (:body top-stories-response))))

(defn add-item-requests
  [{:keys [item-ids] :as w}]
  (assoc w
         :item-requests
         (map
          (fn [item-id]
            {:request-method :get
             :url (str "https://hacker-news.firebaseio.com/v0/item/"
                       item-id
                       ".json")
             :as :json})
          item-ids)))

(defn get-items
  [{:keys [item-requests] :as w}]
  (assoc w
         :item-responses
         (doall
          (map
           (fn [item-request]
             (w/w< (http/request item-request)))
           item-requests))))

(defn add-item-titles
  [{:keys [item-responses] :as w}]
  (assoc w
         :item-titles
         (map
          (fn [item-response]
            (:title (:body item-response)))
          item-responses)))

(comment

  (w/w<
   (add-item-titles
    (get-items
     (add-item-requests
      (add-item-ids
       (get-top-stories
        (add-top-stories-request
         {:top-stories-count 3})))))))

  (require '[simplemono.world.exception :as exception])

  (exception/extract-world-values *e)
  )

```

The example can be found in the project folder `examples/hackernews`. The call
in the comment block will return a map with the entry `:item-titles`, which is a
sequence of the titles of the first three articles on
https://news.ycombinator.com/

The functions only do one task at a time. They don't need to know where their
inputs are coming from or who will use their outputs. The function `get-items`
for example, don't know who constructed the `:item-requests` maps, while
`add-item-requests` don't know that the `:item-ids` are only topstories.

Compared to nested function calls, the functions can be replaced without
needing to change the other functions. The trade-off is that now the data
schema is the element that couples the functions. Therefore more attention
should be paid to the data schema, and also namespaced keywords help a lot.

Only the `w/w<` call includes all the nesting. You can use the function
`w/world-reduce` if you prefer the reverse order. But the nested version is very
convenient to work with since you can evaluate each subform by positioning the
editor cursor behind the corresponding parenthesis before you hit the eval
shortcut to quickly look at intermediate results.

If the `clj-http.client/request` call in `get-items` fails, then the exception
will contain data that, for example, include the request map. All other data added to the input map so far will be
included. And `get-items` shows that `w/w<` can be nested so that the
exception will also contain the data of the levels above. However, these maps will be
large, and reading them is inconvenient. Therefore it is recommended to use a
development tool like [portal](https://github.com/djblue/portal) or
[morse](https://github.com/nubank/morse).

Overall the approach will also consume more memory, but the real challenge is
how to log such large maps. A solution to this problem is described in the next
section.

## Logging

Logging larger data is a problem for a lot of logging services. Google Cloud
Logging, for example, allows 256 KB per log entry. Each log entry is a JSON map
with predefined fields that Google Cloud Logging and the log collector fill. The
data provided by your application will end up in the `jsonPayload` field. It is
tough to calculate if your larger data plus the predefined fields will be
slightly above or under 256 KB. The decision might be different for another
logging service. Therefore this library only takes care that the larger data is
not logged by default. It is accomplished with the
`simplemono.world.exception/world-ex-info` function, which takes the same
arguments as `clojure.core/ex-info`. But instead of the exception data with the
large Clojure map, this:

``` clojure
(ex-data (world-ex-info \"message\" {:large \"map\" ...}))
```

will return something like:

``` clojure
{:world/value-uuid #uuid \"aaa87af8-a466-45b0-b5fc-32cde6424919\"}
```

It is small enough to be logged by any logging solution. The (large)
`world-value` is added as metadata to the `ex-data` of the `ExceptionInfo`.

``` clojure
(simplemono.world.exception/extract-and-log! exception
                                             log-value!)
```

The code above can be used to extract and log the `world-value` entries from the
exception and its causes. An implementation of `log-value!` could, for example
serialize the `world-value` entries with
[nippy](https://github.com/ptaoussanis/nippy), write them to a folder using the
`:world/value-uuid` as the file name. Another thread could then be responsible
for uploading those files to a Google Cloud Storage bucket.

The `simplemono.world.ring-middleware/wrap-log-world-values` function provides a
Ring middleware to catch exceptions, invoke `extract-and-log!` on it, and
rethrow it. Another Ring middleware can log the `world-ex-info` like any other
exception.
