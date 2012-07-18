(ns clj-irc.core
  (:require [clj-irc.utils  :as utils]
            [clojure.string :as string])
  (:import [org.pircbotx PircBotX]
           [org.pircbotx.hooks ListenerAdapter]
           [org.pircbotx.hooks.events MessageEvent]))

(def ^{:dynamic true} *bot*)

(defmacro with-bot
  "Run commands in body with specified bot"
  [bot & body]
  `(binding [*bot* ~bot]
     (do ~@body)))

(defn build-bot
  [{:keys [nick name host port server-password messages-delay]
    :or {nick "ircbotx" name "ircbotx" port 6667 messages-delay 1000}
    :as options}]
  (doto (PircBotX.)
        (.setName nick)
        (.setLogin name)
        (.setMessageDelay messages-delay)
        (.connect host port server-password)))

(defn join-channel
  [channel]
  (.joinChannel *bot* channel))

(defn channels
  []
  (->> *bot* .getChannelsNames (into #{})))

(defn send-message
  [channel message]
  (when-not (-> (channels) (contains? channel))
    (join-channel channel))
  (.sendMessage *bot* channel message))

(defn disconnect
  []
  (.disconnect *bot*))

(defn reconnect
  []
  (.reconnect *bot*))

(defn shutdown
  []
  (.shutdown *bot*))

(defn format-message
  [event]
  {:src
    {:user 
      {:nick       (.. event getUser getNick)
       :real-name  (.. event getUser getRealName)}
     :channel  (.. event getChannel getName)}
   :timestamp  (.getTimestamp event)
   :content  (.getMessage event)})

(defn respond
  [event message]
  (doseq [part (string/split message #"\n")]
    (.respond event part)))

(defn add-handler
  "Add a handler for incomming messages
   Handler signature is [message reply-to]
   Example :
   (add-handler (fn[m r]
                  (r (str \"You just said\" (:content m)))))
   You can also specify a regexp to filter incomming messages"
  [handler & {:keys [regexp bot] :or {regexp #".*"}}]
  (let [handler-impl (proxy [ListenerAdapter] []
                       (onMessage [event]
                         (let [message (format-message event)]
                           (when (re-matches regexp (:content message))
                             (handler message
                                      (fn [response] (respond event response)))))))]
    (.. (or bot *bot*) getListenerManager (addListener handler-impl))))

(defmacro on-message
  [msg reply-to opts & body]
  `(apply
    add-handler
    (fn [~msg ~reply-to] (do ~@body))
    (apply concat ~opts)))

(defn add-auto-reconnect
  [& {:keys [bot channels]}]
  (let [reconnect-handler (proxy [ListenerAdapter] []
                            (onDisconnect [event]
                              (with-bot (.getBot event)
                                (utils/try-times* reconnect 10)
                                (doseq [channel channels]
                                  (join-channel channel)))))]
  (.. (or bot *bot*) getListenerManager (addListener reconnect-handler))))

(defmacro defbot
  "Define an irc bot"
  [{:keys [nick host channels auto-reconnect] :as opts} & body]
  `(let [bot# (build-bot ~opts)]
     (with-bot bot#
       (doseq [channel# ~channels]
         (join-channel channel#))
       (do ~@body)
       (when ~auto-reconnect
         (add-auto-reconnect :channels ~channels))
       bot#)))
