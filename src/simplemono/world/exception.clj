(ns simplemono.world.exception
  (:require [simplemono.world.WorldException])
  (:import simplemono.world.WorldException))

(defn world-exception
  "Returns a `WorldException`, which is an alternative to `ex-info` to hold large data.

   See namespace `simplemono.world.WorldException` for more details."
  ([msg world-value]
   (world-exception msg
                    world-value
                    nil))
  ([msg world-value cause]
   (WorldException.
     msg
     world-value
     cause)))

(defn causes-seq
  "Returns a sequence with the `throwable` and all its causes."
  [^Throwable throwable]
  (when throwable
    (cons throwable
          (lazy-seq (causes-seq (.getCause throwable))))))

(defn extract-world-values
  "Extracts all world-values from `e` and its causes. Returns a sequence of
   world-values."
  [^Throwable e]
  (keep
    (fn [throwable]
      (when (instance? WorldException
                       throwable)
        (let [uuid (:world/value-uuid (ex-data throwable))]
          {:uuid uuid
           :value @throwable})))
    (causes-seq e)))

(defn extract-and-log!
  "Extracts all world-values from `e` and its causes. The UUID and the world value
   is passed as arguments to `log-value!`, which is supposed to quickly store the
   world-value."
  [^Throwable e log-value!]
  (doseq [{:keys [uuid value]} (extract-world-values e)]
    (log-value! uuid
                value)))
