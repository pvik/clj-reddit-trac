(ns reddit-trac.routes
  (:require [compojure.core     :refer [defroutes context routes GET POST PUT DELETE]]
            [compojure.route    :refer [not-found files resources]]
            [ring.util.response :refer [response]]
            [taoensso.timbre    :as    log]
            [reddit-trac.api    :as    api]
            [reddit-trac.helper :as    h])
  (:gen-class))

(defroutes api-routes
  (context "/api" {body :body}
           ;; CREATE
           (PUT "/watch" [] (api/create-watch body))
           (GET "/watch/validate/:id{[0-9]+}"
                [token email id :<< h/as-int]
                (api/validate-watch id email token))
           ;; READ
           (GET "/watch" [token email]
                (api/get-watch :email email token))
           (GET "/watch/:id{[0-9]+}" [token id :<< h/as-int]
                (api/get-watch :id id token))
           ;; UPDATE
           ;; (POST "/watch" [] (println "update"))
           ;; DELETE
           (DELETE "/watch/:id{[0-9]+}" [token email id :<< h/as-int] (api/delete-watch id email token))))

(defroutes gen-routes 
  (GET "/" [] "Hello from Compojure!")  ;; for testing only
  (files "/" {:root "target"})          ;; to serve static resources
  (resources "/" {:root "target"})      ;; to serve anything else
  (not-found "404 Page Not Found"))     ;; page not found

(def app-routes (routes api-routes
                        gen-routes))

