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
