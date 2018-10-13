(ns reddit-trac.notify.mail
  (:require [taoensso.timbre :as log]
            [postal.core     :as postal])
  (:gen-class))

(defonce ^:const ^:private email-server
  (:email (clojure.edn/read-string (slurp "resources/config.edn"))))

(defn send-mail [email-props ]
  (log/info "dispatching email" email-props)
  (let [from              (:from email-server)
        to                (:to email-props)
        cc                (:cc email-props)
        bcc               (:bcc email-props)
        subject           (:subject email-props)
        body              [{:type "text/html; charset=utf-8"
                            :content (:body email-props)}]
        email-msg         {:from from :to to :cc cc :bcc bcc :subject subject :body body}
        postal-response   (if (nil? (:host email-server))
                            ;; nil email-server => use local sendmail
                            (postal/send-message email-msg)
                            (postal/send-message (dissoc email-server :from) email-msg))]
    (log/info postal-response)))
