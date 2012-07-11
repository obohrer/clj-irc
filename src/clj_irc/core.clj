(ns clj-irc.core
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
  [{:keys [name host port server-password]
    :or {name "ircbotx" port 6667}
    :as options}]
  (doto (PircBotX.)
        (.setName name)
        (.connect host port server-password)))

(defn join-channel
  [channel-name]
  (.joinChannel *bot* channel-name))

(defn send-message
  [channel message]
  (.sendMessage *bot* channel message))

(defn format-message
  [event]
  {:src
    {:user 
      {:nick       (.. event getUser getNick)
       :real-name  (.. event getUser getRealName)}
     :channel  (.. event getChannel getName)}
   :timestamp  (.getTimestamp event)
   :content  (.getMessage event)})

(defn add-handler
  "Add a handler for incomming messages
   Handler signature is [message reply-to]
   Example :
   (add-handler (fn[m r]
                  (r (str \"You just said\" (:content m)))))
   You can also specify a regexp to filter incomming messages"
  [handler & {:keys [regexp] :or {regexp #".*"}}]
  (let [handler-impl (proxy [ListenerAdapter] []
                       (onMessage [event]
                         (let [message (format-message event)]
                           (when (re-matches regexp (:content message))
                             (handler message
                                      (fn [response] (.respond event response)))))))]
    (.. *bot* getListenerManager (addListener handler-impl))))
