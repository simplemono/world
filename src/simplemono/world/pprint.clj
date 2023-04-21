(ns simplemono.world.pprint
  "Ensures that the `ex-data` of a `simplemono.world.WorldException` is
   pretty-printed, instead of its world-value."
  (:require [clojure.pprint :as pprint]))

(defmethod pprint/simple-dispatch simplemono.world.WorldException [obj]
  (#'pprint/pprint-simple-default obj))
