(ns simplemono.world.WorldException
  "An alternative for `ex-info` to hold `world-values`.

   A `world-value` is a Clojure map that potentially contains a lot of data. The
   amount of data often exceeds the maximum allowed data size for your logging
   service (Google Cloud Logging allows 256 KB per log entry for example). For
   that reason it is problematic to include large data as `ex-data` into an
   `ex-info`, if it is going to be logged.

   Like `ex-info` the `WorldException` takes 3 arguments:

   - The error message (a String)

   - A (large) Clojure map

   - And a Throwable (cause of the exception)

   Instead of the large Clojure map `(ex-data ^WorldException e)` will return
   something like this:

       {:world/value-uuid #uuid \"aaa87af8-a466-45b0-b5fc-32cde6424919\"}

   This data is small enough to be logged by any logging solution.
   `WorldException` also implements `clojure.lang.IDeref`. A `@e` or `(deref e)`
   will return the large Clojure map (the world-value).
  "
  (:gen-class
   :implements [clojure.lang.IExceptionInfo
                clojure.lang.IDeref]
   :extends java.lang.RuntimeException
   :constructors {[String clojure.lang.IPersistentMap Throwable] [String Throwable]} ; mapping of my-constructor -> superclass constuctor
   :init init
   :state state                ; name for the var that holds your internal state
   :main false
   :prefix "world-ex-"))

(defn world-ex-init [message world-value throwable]
  [
   ;; parameters for the superclass constructor:
   [message throwable]

   ;; internal state:
   {:world/value world-value
                        :ex-data {:world/value-uuid (java.util.UUID/randomUUID)}}]
  )

(defn world-ex-getData [this]
  (:ex-data (.state this)))

(defn world-ex-deref [this]
  (:world/value (.state this)))

(when-not (resolve 'simplemono.world.WorldException)
  ;; compile at runtime, if it was not AOT compiled:
  (compile 'simplemono.world.WorldException))

(defmethod
  ^{:doc "Takes care that `pr` only prints the `ex-data` and not the world-value."}
  print-method
  simplemono.world.WorldException [ex writer]
  (#'clojure.core/print-throwable ex
                                  writer)
  )

(comment
  (def example-exception
    (simplemono.world.WorldException.
      "error"
      {:some "data"}
      (Exception. "some error")
      ))

  (ex-data example-exception)

  @example-exception

  (pr-str example-exception)

  (prn example-exception)

  (tap> example-exception)

  (require '[clojure.pprint :as pprint]
           'simplemono.world.pprint)

  (pprint/pprint example-exception)

  )
