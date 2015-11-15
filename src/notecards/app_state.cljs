(ns notecards.app-state
  (:require [om.core :as om]
            [shodan.console :as console]
            [cljs.core.async :refer [put!]]
            [goog.string.format]
            [notecards.api :as api]
            [promesa.core :as p]
            [notecards.history :as history]
            [notecards.routes :as routes]))

(def default-signup {:username ""
                     :password ""
                     :confirm ""})

(def default-login {:username ""
                    :password ""})

(defonce app-state (atom {:page :home
                          :signup default-signup
                          :login default-login
                          :notes []
                          :selected-note nil}))

(defn set-page! [data page]
  (om/transact! data #(assoc % :page page)))

(defn set-signup! [data signup]
  (om/transact! data #(assoc % :signup signup)))

(defn sign-up! [data user]
  (-> (api/sign-up user)
      (p/then (fn []
                (om/transact! data #(-> %
                                        (assoc :signup default-signup)
                                        (assoc :login (assoc default-login :username (:username user)))))
                (.setToken history/history (routes/login-path))))))

(defn set-login! [data login]
  (om/transact! data #(assoc % :login login)))

(defn log-in! [data user]
  ; TODO Actually send the login request. Right now we just go to home.
  (om/transact! data #(assoc % :login default-login))
  (.setToken history/history (routes/home-path)))
  #_(-> (api/log-in user)
      (p/then (fn []
                (om/transact! data #(assoc % :login default-login))
                (.setToken history/history (routes/home-path)))))

(defn log-out! [data]
  ; TODO Actually send the logout request. Right now we just go to the login page.
  (.setToken history/history (routes/login-path)))

(defn get-notes! [data]
  (-> (api/get-notes)
      (p/then (fn [notes]
                (om/transact! data #(-> %
                                        (assoc :notes notes)))))))

(defn select-note! [data id]
  (om/transact! data #(assoc % :selected-note id)))

(defn post-message! [ch message]
  (put! ch message))

(defn handle-message! [data {:keys [action] :as message}]
  (case action
    :set-page (set-page! data (:page message))
    :set-signup (set-signup! data (:signup message))
    :sign-up (sign-up! data (:user message))
    :set-login (set-login! data (:login message))
    :log-in (log-in! data (:user message))
    :log-out (log-out! data)
    :get-notes (get-notes! data)
    :select-note (select-note! data (:id message))))
