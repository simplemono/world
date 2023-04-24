(ns simplemono.world.exception)

(defn world-ex-info
  "An alternative for `ex-info` to hold `world-values`.

   A `world-value` is a Clojure map that potentially contains a lot of data. The
   amount of data often exceeds the maximum allowed data size for your logging
   service (Google Cloud Logging allows 256 KB per log entry for example). For
   that reason it is problematic to include large data as `ex-data` into an
   `ex-info`, if it is going to be logged.

   This function takes the same arguments like `ex-info`:

   - The error message (a String)

   - A (large) Clojure map

   - And a Throwable (cause of the exception)

   Instead of the large Clojure map:

      (ex-data (world-ex-info \"message\" {:large \"map\" ...}))

   something like this will be returned:

       {:world/value-uuid #uuid \"aaa87af8-a466-45b0-b5fc-32cde6424919\"}

   This is small enough to be logged by any logging solution. The (large)
   `world-value` is added as metadata to the `ex-data` of the
   `ExceptionInfo`.

   See: `ex-world-value` to extract the `world-value` from a thrown exception."
  ([message world-value]
   (world-ex-info message
                  world-value
                  nil))
  ([message world-value cause]
   (ex-info
    message
    (with-meta
      {:world/value-uuid (java.util.UUID/randomUUID)}
      {:world/value world-value})
    cause)))

(defn causes-seq
  "Returns a sequence with the `throwable` and all its causes."
  [^Throwable throwable]
  (when throwable
    (cons throwable
          (lazy-seq (causes-seq (.getCause throwable))))))

(defn ex-world-value
  "Extracts the `:world/value-uuid` and `:world/value` from the metadata of the
   `ex-data`."
  [throwable]
  (when-let [world-value (-> throwable
                             (ex-data)
                             (meta)
                             (:world/value))]
    {:world/value-uuid (:world/value-uuid (ex-data throwable))
     :world/value world-value}))

(defn extract-world-values
  "Extracts all world-values from `e` and its causes. Returns a sequence of
   world-values."
  [^Throwable e]
  (keep
   ex-world-value
   (causes-seq e)))

(defn extract-and-log!
  "Extracts all world-values from `e` and its causes. The `:world/value-uuid` and
   the `:world/value` are passed as map to `log-value!`, which is supposed to
   quickly store the world-value."
  [^Throwable e log-value!]
  (doseq [data (extract-world-values e)]
    (log-value! data)))

(comment
  (def example-exception
    (world-ex-info "error"
                   {:some "data"}
                   (Exception. "some error"
                               (world-ex-info "another error"
                                              {:other "data"}))
                   ))

  (ex-world-value example-exception)

  (pr-str example-exception)

  (prn example-exception)

  (tap> example-exception)

  (require '[clojure.pprint :as pprint])

  (pprint/pprint example-exception)

  (extract-and-log! example-exception
                    prn)
  )
