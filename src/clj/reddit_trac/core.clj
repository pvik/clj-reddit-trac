(ns reddit-trac.core
  (:require [compojure.handler    :as handler]
            [ring.middleware.json :refer [wrap-json-response
                                          wrap-json-body]]
            [taoensso.timbre :as log]
            [reddit-trac.routes  :refer [app-routes]])
  (:gen-class))

;; initialize stuff here
(defonce _
  (do
    (log/merge-config! (eval (clojure.edn/read-string
                              (slurp "resources/logging.edn"))))
    (log/info "Initializing Reddit-Trac")
    nil))

;; To encode Joda objects correctly in JSON response to user
(extend-protocol cheshire.generate/JSONable
  org.joda.time.DateTime
  (to-json [t jg]
    (cheshire.generate/write-string jg (str t))))

(defn wrap-debug [handler lbl]
  (fn [request]
    (log/debug lbl " -> " request "\n")
    (handler request)))

(def app (as-> #'app-routes $
           ;; (wrap-debug $ "request: ")
           (wrap-json-body $ {:keywords? true})
           (wrap-json-response $)
           (handler/site $)))

