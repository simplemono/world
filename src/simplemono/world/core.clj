(ns simplemono.world.core
  (:require [simplemono.world.exception :as exception]))

(defn wrap-catch
  "Default middleware that wraps a `step` function with a try-catch, which wraps
   catched throwables into a `world-ex-info`."
  [step]
  (fn [w]
    (try
      (step w)
      (catch Throwable e
        (throw (exception/world-ex-info
                (str "error applying step: "
                     step)
                w
                e))))))

(defn world-reduce
  "Reduces the sequence of `steps` functions over the world-value `w0`."
  ([middleware w0 steps]
   (reduce
    (fn [w step]
      (step w))
    w0
    (map
     middleware
     steps)))
  ([w0 steps]
   (world-reduce wrap-catch
                 w0
                 steps)))

(defmacro w<
  "A macro that unnest a `form` of steps and pass them to `world-reduce`."
  ([middleware form]
   (loop [form form
          steps (list)]
     (if (seq? form)
       (recur (second form)
              (conj steps
                    (first form))
              )
       (list `world-reduce
             middleware
             form
             (vec steps)))))
  ([form]
   `(w< ~wrap-catch
        ~form))
  )

(defn tap
  "Invokes `clojure.core/tap>` with the world-value `w` and returns it unchanged.
   Helpful to inspect an intermediate world-value in a `w<` or `world-reduce`
   call."
  [w]
  (tap> w)
  w)
