(ns simplemono.world.middleware)

(defn track-duration
  "A middleware that tracks the duration of each step in milliseconds (with
   nanosecond precision)."
  [step]
  (fn [w]
    (let [start (. System (nanoTime))
          w* (step w)
          ms (/ (double (- (. System (nanoTime)) start))
                1000000.0)]
      (assoc-in w*
                [:world/step-durations
                 step]
                ms))))

(comment
  (require '[simplemono.world.core :as w])

  (defn add-api-call
    [w]
    (Thread/sleep 1000)
    (assoc w
           :api-result
           {:x 1
            :y 2}))

  (defn add-calculation
    [{:keys [api-result] :as w}]
    (assoc w
           :result
           (+ (:x api-result)
              (:y api-result))))

  (def example-w0
    {:some "data"})

  (w/w<
    (add-calculation
      (add-api-call example-w0)))

  (w/w<
    (comp w/wrap-catch track-duration)
    (add-calculation
      (add-api-call example-w0)))
  )
