(set-env!
 :source-paths #{"src/clj"}
 
 :dependencies '[[org.clojure/clojure "1.9.0"]
                 ;; server
                 [compojure "1.6.1"]
                 [ring/ring-core "1.7.0"]
                 [ring/ring-jetty-adapter "1.7.0"]
                 [ring/ring-json "0.4.0"]
                 [ring-middleware-format "0.7.2"]
                 [javax.servlet/servlet-api "2.5"]
                 [org.clojure/core.async "0.4.474"]
                 [clj-time "0.14.4"]
                 [clj-http "3.9.1"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/data.codec "0.1.1"] ;; Base64
                 [digest "1.4.8"]
                 ;; db
                 [hikari-cp "2.6.0"]    ;; Connection Pooling
                 [org.clojure/java.jdbc "0.7.8"]
                 [honeysql "0.9.4"]
                 [org.postgresql/postgresql "42.2.5"]
                 ;; logging
                 [com.taoensso/timbre "4.10.0"] ;; logging
                 [com.fzakaria/slf4j-timbre "0.3.12"]
                 [org.slf4j/log4j-over-slf4j "1.7.25"]
                 [org.slf4j/jul-to-slf4j "1.7.25"]
                 [org.slf4j/jcl-over-slf4j "1.7.25"]
                 ;; spec , test & gen
                 [org.clojure/test.check "0.9.0"]
                 ;; Dependencies for build process
                 [pandeiro/boot-http "0.8.3" :scope "test"]
                 [adzerk/boot-reload "0.6.0" :scope "test"]
                 [nrepl "0.4.5" :scope "test"]
                 [weasel "0.7.0" :scope "test"]
                 [ragtime "0.7.2" :scope "test"] ;; Migrations
                 [mbuczko/boot-ragtime "0.3.1" :scope "test"]])

(require 
 '[pandeiro.boot-http :refer [serve]]
 '[adzerk.boot-reload :refer [reload]]
 '[mbuczko.boot-ragtime :refer [ragtime]])

(def db-opts (:db (clojure.edn/read-string (slurp "resources/config.edn"))))
(task-options!
 ragtime {:database (str "jdbc:"
                         (:dbtype db-opts) "://"
                         (:user db-opts) ":"
                         (:password db-opts) "@"
                         (:host db-opts) ":"
                         (:port db-opts) "/"
                         (:dbname db-opts))})


(deftask dev
  "Launch Immediate Feedback Development Environment"
  []
  (comp
   (serve :handler 'reddit-trac.core/app ;; ring handler
          :resource-root "target"            ;; root classpath
          :reload true)                      ;; reload ns
   (watch)
   (reload)
   (target :dir #{"target"})))
