(ns reddit-trac.core
  (:require [compojure.handler    :as handler]
            [ring.middleware.json :refer [wrap-json-response
                                          wrap-json-body]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.cors :refer [wrap-cors]]
            [taoensso.timbre :as log]
            [reddit-trac.routes  :refer [app-routes]]
            [reddit-trac.trac  :refer [trac-sched]])
  (:gen-class))

;; initialize stuff here
(defonce _
  (do
    (log/merge-config! (eval (clojure.edn/read-string
                              (slurp "resources/logging.edn"))))
    (log/info "Initializing Reddit-Trac")
    nil))

(defonce ^:private ^:const config
  (clojure.edn/read-string (slurp "resources/config.edn")))

(defonce ^:const jetty-props
  (:api-server config))

(defonce ^:private ^:const uri
  (:uri config))

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
           (wrap-cors $ :access-control-allow-origin [(re-pattern uri)]
                      :access-control-allow-methods [:get :put :post :delete])
           ;; (wrap-debug $ "request: ")
           (wrap-json-body $ {:keywords? true})
           (wrap-json-response $)
           (handler/site $)))

(defn -main []
  (trac-sched)
  (if (:enabled jetty-props)
    (log/debug "Starting Jetty Server...")
    (run-jetty app (dissoc jetty-props :enabled))))
