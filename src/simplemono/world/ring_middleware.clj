(ns simplemono.world.ring-middleware
  (:require [simplemono.world.exception :as exception]))

(defn wrap-log-world-values
  "A middleware for Ring that can be used to extract and log all world-values from
   a thrown `simplemono.world.exception/world-ex-info`. It will also pass all nested
   `world-ex-info` to `log-value!`, which will receive the UUID as first
   argument and the world-value as second argument."
  [ring-handler log-value!]
  (fn
    ([request]
     (try
       (ring-handler request)
       (catch Throwable e
         (exception/extract-and-log! e
                                     log-value!)
         (throw e))))
    ([request respond raise]
     (try
       (ring-handler request
                     respond
                     (fn [throwable]
                       (exception/extract-and-log! throwable
                                                   log-value!)
                       (raise throwable)))
       (catch Throwable e
         (exception/extract-and-log! e
                                     log-value!)
         (throw e))))))
